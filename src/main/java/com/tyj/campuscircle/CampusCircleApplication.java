package com.tyj.campuscircle;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(value = "com.tyj.campuscircle", markerInterface = BaseMapper.class)
@SpringBootApplication
public class CampusCircleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusCircleApplication.class, args);
    }

}
