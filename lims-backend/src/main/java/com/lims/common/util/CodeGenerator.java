package com.lims.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 业务单号生成器: 前缀 + 年份 + Redis 自增序列(4位, 跨年重置)。
 * 如 WT-2026-0001 / HT20260001 / BJ20260001 / KH20260001
 */
@Component
@RequiredArgsConstructor
public class CodeGenerator {

    private final StringRedisTemplate redisTemplate;

    /**
     * @param prefix   业务前缀, 如 "WT-"
     * @param withDash 年份与序号之间是否用 "-" 连接
     */
    public String next(String prefix, boolean withDash) {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String key = "lims:seq:" + prefix + year;
        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redisTemplate.expireAt(key, LocalDate.now().plusYears(1).withDayOfYear(1)
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        }
        return prefix + year + (withDash ? "-" : "") + String.format("%04d", seq == null ? 1 : seq);
    }
}
