package com.itheima.consultant.config;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itheima.consultant.mapper.ShopMapper;
import com.itheima.consultant.pojo.Shop;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingStoreService {
    // 静态初始化块，确认类被加载
    static {
        System.out.println("===== EmbeddingStoreService 类加载 =====");
    }
    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmbeddingStore store; // 这里是通过 @Bean 创建的 EmbeddingStore 实例

    // 记录上次更新时间
    private LocalDateTime lastUpdateTime = LocalDateTime.now().minusDays(7); // 初始值设为7天前，确保首次执行时能获取所有数据

    // 标记是否已经执行过首次全量加载
    private boolean initialized = false;

    // 增量更新的时间窗口（秒）
    private static final int UPDATE_WINDOW_SECONDS = 10;

    // 应用启动时执行首次全量加载
    @PostConstruct
    public void init() {
        System.err.println("执行首次全量加载");
        System.err.flush();
        fullUpdate();
        initialized = true;
        System.err.println("首次全量加载完成");
        System.err.flush();
    }

    // 定义一个周期性任务，每隔一定时间执行一次增量更新
    // 使用先删旧向量再插新向量的策略，确保向量不重复
    @Scheduled(fixedRate = 10000)  // 每10秒执行一次
    public void periodicIncrementalUpdate() {
        incrementalUpdate();
    }

    // 定义一个周期性任务，每隔7天执行一次全量更新（兜底机制）
    @Scheduled(fixedRate = 604800000)  // 每7天执行一次全量更新
    public void periodicFullUpdate() {
        fullUpdate();
    }

    /**
     * 增量更新方法
     */
    private void incrementalUpdate() {
        try {
            System.err.println("\n\n==========================================");
            System.err.println("========= 增量更新开始 ========");
            System.err.println("==========================================");
            System.err.flush();
            
            LocalDateTime now = LocalDateTime.now();
            
            System.err.println("1. 时间信息：");
            System.err.println("   当前时间：" + now);
            System.err.println("   上次更新时间：" + lastUpdateTime);
            System.err.println("   窗口大小：上次更新到现在");
            System.err.flush();
            
            // ========== 先检测并删除被删除的商铺向量 ==========
            System.err.println("2. 检测被删除的商铺...");
            detectAndRemoveOrphanedEmbeddings();
            
            System.err.println("3. 执行增量查询：");
            QueryWrapper<Shop> queryWrapper = new QueryWrapper<>();
            // 使用 lastUpdateTime 作为查询条件，查上次更新之后的所有新数据
            String lastUpdateStr = lastUpdateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            queryWrapper.gt("update_time", lastUpdateStr);
            System.err.println("   查询条件：update_time > " + lastUpdateStr);
            System.err.println("   构建的查询条件：" + queryWrapper.getSqlSegment());
            System.err.flush();
            
            List<Shop> shops = shopMapper.selectList(queryWrapper);
            System.err.println("   增量查询结果：" + shops.size() + " 条");
            
            // 输出每条数据的updateTime
            if (!shops.isEmpty()) {
                System.err.println("   数据详情：");
                for (int i = 0; i < shops.size() && i < 5; i++) { // 最多显示5条
                    Shop shop = shops.get(i);
                    System.err.println("     " + (i+1) + ". 商铺名称：" + shop.getName());
                    System.err.println("        更新时间：" + shop.getUpdateTime());
                    System.err.println("        是否在窗口内：" + shop.getUpdateTime().isAfter(lastUpdateTime));
                }
                if (shops.size() > 5) {
                    System.err.println("     ... 还有 " + (shops.size() - 5) + " 条数据");
                }
            }
            System.err.flush();

            if (shops.isEmpty()) {
                System.err.println("4. 结果：没有数据需要更新");
                System.err.println("==========================================");
                System.err.println("========= 增量更新结束 ========");
                System.err.println("==========================================\n\n");
                System.err.flush();
                return;
            }

            System.err.println("4. 结果：");
            System.err.println("   需要更新的数据量：" + shops.size());
            System.err.flush();

            // 处理数据并存储
            System.err.println("5. 处理数据并存储（使用映射表管理向量）：");
            System.err.flush();
            
            for (Shop shop : shops) {
                String content = "商铺名称: " + shop.getName() + "\n"
                        + "商铺类型ID: " + shop.getTypeId() + "\n"
                        + "商圈: " + shop.getArea() + "\n"
                        + "地址: " + shop.getAddress() + "\n"
                        + "均价: " + shop.getAvgPrice() + "\n"
                        + "销量: " + shop.getSold() + "\n"
                        + "评分: " + shop.getScore() + "\n"
                        + "营业时间: " + shop.getOpenHours();

                String shopIdStr = shop.getId().toString();
                
                System.err.println("   处理商铺 " + shop.getName() + " (ID: " + shopIdStr + ")...");
                
                // ========== 1. 查询映射表，获取旧的 embedding_id ==========
                String oldEmbeddingId = null;
                try {
                    List<Map<String, Object>> results = jdbcTemplate.queryForList(
                        "SELECT embedding_id FROM shop_embedding_mapping WHERE shop_id = ?", shopIdStr);
                    if (!results.isEmpty()) {
                        oldEmbeddingId = (String) results.get(0).get("embedding_id");
                    }
                } catch (Exception e) {
                    // 映射表可能不存在，忽略
                }
                
                // ========== 2. 如果存在旧向量，使用 Chroma 客户端删除 ==========
                if (oldEmbeddingId != null) {
                    deleteEmbeddingById(oldEmbeddingId);
                    System.err.println("      已删除旧向量 (embedding_id: " + oldEmbeddingId + ")");
                }
                
                // ========== 3. 生成新向量并插入 ==========
                TextSegment segment = new TextSegment(content, Metadata.from("shopId", shopIdStr));
                var embedding = embeddingModel.embed(content).content();
                String newEmbeddingId = store.add(embedding, segment);
                System.err.println("      已添加新向量 (embedding_id: " + newEmbeddingId + ")");
                
                // ========== 4. 更新映射表 ==========
                if (oldEmbeddingId != null) {
                    jdbcTemplate.update("UPDATE shop_embedding_mapping SET embedding_id = ?, updated_at = NOW() WHERE shop_id = ?",
                        newEmbeddingId, shopIdStr);
                } else {
                    jdbcTemplate.update("INSERT INTO shop_embedding_mapping (shop_id, embedding_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                        shopIdStr, newEmbeddingId);
                }
            }

            // 更新上次更新时间
            lastUpdateTime = LocalDateTime.now();
            System.err.println("6. 完成：");
            System.err.println("   增量更新完成，更新了 " + shops.size() + " 条数据，更新时间：" + lastUpdateTime);
            
            System.err.println("==========================================");
            System.err.println("========= 增量更新结束 ========");
            System.err.println("==========================================\n\n");
            System.err.flush();
        } catch (Exception e) {
            System.err.println("增量更新过程中发生异常：" + e.getMessage());
            e.printStackTrace();
            System.err.flush();
        }
    }
    
    /**
     * 检测并删除孤立向量
     * 对比映射表和数据库，删除在向量库中存在但数据库已删除的商铺向量
     */
    private void detectAndRemoveOrphanedEmbeddings() {
        try {
            // 1. 获取映射表中所有商铺ID
            List<Map<String, Object>> mappingResults = jdbcTemplate.queryForList(
                "SELECT shop_id, embedding_id FROM shop_embedding_mapping");
            
            if (mappingResults.isEmpty()) {
                System.err.println("   映射表为空，跳过检测");
                return;
            }
            
            // 2. 获取数据库当前所有商铺ID
            List<Shop> allShops = shopMapper.selectList(new QueryWrapper<>());
            List<Long> dbShopIds = allShops.stream().map(Shop::getId).toList();
            
            int deletedCount = 0;
            
            // 3. 遍历映射表，找出孤立的记录
            for (Map<String, Object> row : mappingResults) {
                Long shopId = ((Number) row.get("shop_id")).longValue();
                String embeddingId = (String) row.get("embedding_id");
                
                // 如果数据库中不存在这个商铺ID，说明已被删除
                if (!dbShopIds.contains(shopId)) {
                    System.err.println("   发现孤立向量：shop_id=" + shopId + ", embedding_id=" + embeddingId);
                    
                    // 使用 Chroma 客户端删除孤立向量
                    deleteEmbeddingById(embeddingId);
                    
                    // 删除映射表记录
                    jdbcTemplate.update("DELETE FROM shop_embedding_mapping WHERE shop_id = ?", shopId);
                    
                    deletedCount++;
                }
            }
            
            if (deletedCount > 0) {
                System.err.println("   已删除 " + deletedCount + " 个孤立向量");
            } else {
                System.err.println("   没有发现孤立向量");
            }
            
        } catch (Exception e) {
            System.err.println("   检测孤立向量时出错：" + e.getMessage());
        }
    }

    /**
     * 全量更新方法
     * 执行时机：
     * 1. 应用首次启动时
     * 2. 每7天执行一次兜底更新
     * 核心逻辑：
     * - 清空向量库
     * - 遍历所有商铺，逐个添加向量
     * - 同步更新映射表
     */
    private void fullUpdate() {
        System.err.println("==========================================");
        System.err.println("========= 全量更新开始 ========");
        System.err.println("==========================================");
        System.err.println("执行全量更新（首次启动或定期兜底更新）");
        System.err.flush();
        
        // 1. 从数据库中获取所有商铺数据
        System.err.println("1. 查询数据：");
        System.err.println("   更新类型：全表查询");
        System.err.println("   查询条件：无（获取所有商铺数据）");
        List<Shop> shops = shopMapper.selectList(new QueryWrapper<>());
        System.err.println("   查询结果：共 " + shops.size() + " 条商铺数据");
        System.err.println("   数据范围：包含所有商铺信息，将重新构建向量索引");
        System.err.flush();

        // 2. 清空映射表
        System.err.println("2. 清空映射表：");
        try {
            jdbcTemplate.execute("TRUNCATE TABLE shop_embedding_mapping");
            System.err.println("   已清空映射表");
        } catch (Exception e) {
            System.err.println("   清空映射表失败：" + e.getMessage());
        }
        System.err.flush();

        // 3. ChromaEmbeddingStore 不支持直接清空，但可以重建 collection
        // 这里直接添加新数据即可，Chroma 会在同一 collection 中累加
        System.err.println("3. 提示：Chroma 不支持直接清空，将增量添加向量");
        System.err.flush();

        // 4. 遍历所有商铺，添加向量
        System.err.println("4. 逐个添加向量：");
        for (Shop shop : shops) {
            String content = "商铺名称: " + shop.getName() + "\n"
                    + "商铺类型ID: " + shop.getTypeId() + "\n"
                    + "商圈: " + shop.getArea() + "\n"
                    + "地址: " + shop.getAddress() + "\n"
                    + "均价: " + shop.getAvgPrice() + "\n"
                    + "销量: " + shop.getSold() + "\n"
                    + "评分: " + shop.getScore() + "\n"
                    + "营业时间: " + shop.getOpenHours();

            String shopIdStr = shop.getId().toString();
            
            // 生成向量并添加
            TextSegment segment = new TextSegment(content, Metadata.from("shopId", shopIdStr));
            var embedding = embeddingModel.embed(content).content();
            String embeddingId = store.add(embedding, segment);
            
            // 更新映射表
            jdbcTemplate.update("INSERT INTO shop_embedding_mapping (shop_id, embedding_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                shopIdStr, embeddingId);
        }
        System.err.println("   向量添加完成，共 " + shops.size() + " 条");
        System.err.flush();

        // 4. 更新上次更新时间
        lastUpdateTime = LocalDateTime.now();
        System.err.println("4. 完成：");
        System.err.println("   全量更新完成，更新了 " + shops.size() + " 条数据，更新时间：" + lastUpdateTime);
        System.err.println("   向量索引和映射表已重建");
        System.err.println("==========================================");
        System.err.println("========= 全量更新结束 ========");
        System.err.println("==========================================\n\n");
        System.err.flush();
    }

    /**
     * 使用 Chroma 客户端按 ID 删除向量
     */
    private void deleteEmbeddingById(String embeddingId) {
        try {
            ChromaEmbeddingStore chromaStore = (ChromaEmbeddingStore) store;
            chromaStore.removeAll(List.of(embeddingId));
        } catch (Exception e) {
            System.err.println("      删除向量失败: " + e.getMessage());
        }
    }

    /**
     * 根据商铺ID删除旧的向量
     * 使用 Chroma 客户端按 ID 删除
     */
    private void removeEmbeddingsByShopId(Long shopId) {
        try {
            String shopIdStr = shopId.toString();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT embedding_id FROM shop_embedding_mapping WHERE shop_id = ?", shopIdStr);
            if (!results.isEmpty()) {
                String embeddingId = (String) results.get(0).get("embedding_id");
                deleteEmbeddingById(embeddingId);
            }
        } catch (Exception e) {
            System.err.println("      删除向量失败: " + e.getMessage());
        }
    }

}
