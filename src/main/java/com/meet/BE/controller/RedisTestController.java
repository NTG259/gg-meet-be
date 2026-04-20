package com.meet.BE.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisTestController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/test/redis")
    public void testRedis() {
        redisTemplate.opsForValue().set("be_test", "hello_from_be");
        String value = redisTemplate.opsForValue().get("be_test");
        System.out.println("Redis value: " + value);
    }
}