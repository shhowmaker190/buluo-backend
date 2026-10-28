package com.vote.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-path:uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 必须解析成绝对路径，否则会随进程工作目录变化而失效（与 FileController 保持一致）
        String location = "file:" + new File(uploadPath).getAbsoluteFile().toPath().normalize() + File.separator;
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
        log.info("静态资源映射 /uploads/** -> {}", location);
    }
}
