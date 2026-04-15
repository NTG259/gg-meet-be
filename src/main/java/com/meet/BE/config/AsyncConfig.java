package com.meet.BE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Luôn có 5 luồng chờ sẵn
        executor.setMaxPoolSize(20); // Tối đa 20 luồng gửi mail cùng lúc
        executor.setQueueCapacity(100); // Hàng đợi 100 request
        executor.setThreadNamePrefix("MailSender-");
        executor.initialize();
        return executor;
    }
}