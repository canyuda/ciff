package com.ciff.app.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(DataSource.class)
@MapperScan("com.ciff.**.mapper")
public class MybatisConfig {
}
