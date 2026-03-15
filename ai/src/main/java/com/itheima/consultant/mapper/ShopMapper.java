package com.itheima.consultant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itheima.consultant.pojo.Reservation;
import com.itheima.consultant.pojo.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author jiwang

 */
@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
    @Select("select * from tb_shop where name=#{shopName}")
    Shop findShop(String shopName);
    
    @Select("select * from tb_shop")
    List<Shop> selectAll();

    @Select({"<script>",
        "select * from tb_shop",
        "<where>",
        "${ew.sqlSegment}",
        "</where>",
        "</script>"})
    List<Shop> selectList(@Param("ew") QueryWrapper<Shop> queryWrapper);
}
