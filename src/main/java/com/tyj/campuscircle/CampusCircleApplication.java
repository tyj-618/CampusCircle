package com.tyj.campuscircle;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.ai.AiProperties;
import com.tyj.campuscircle.ai.SearchProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@MapperScan(value = "com.tyj.campuscircle", markerInterface = BaseMapper.class)
@SpringBootApplication
@EnableConfigurationProperties({AiProperties.class, SearchProperties.class})
public class CampusCircleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusCircleApplication.class, args);
    }

}
