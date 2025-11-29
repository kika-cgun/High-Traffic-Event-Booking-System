package com.example.hightrafficeventbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisLockService {
    private final StringRedisTemplate redisTemplate;

    public boolean acquireLock(Long seatId, Long userId){
        String key = "lock:seat:" + seatId;

        boolean success = redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(userId), Duration.ofMinutes(10));

        return success;
    }

    public void releaseLock(Long seatId){
        String key = "lock:seat:" + seatId;
        redisTemplate.delete(key);
    }

}
