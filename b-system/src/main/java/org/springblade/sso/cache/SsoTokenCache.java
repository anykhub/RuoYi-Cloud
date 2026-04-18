package org.springblade.sso.cache;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SSO 短效 Token 缓存类 (B系统)
 * 基于 ConcurrentHashMap 实现 30 秒缓存机制
 */
@Component
public class SsoTokenCache {

    // 缓存数据结构，存储 token 及其对应的用户信息 (比如 email) 和过期时间
    private final ConcurrentHashMap<String, CacheItem> cache = new ConcurrentHashMap<>();

    /**
     * 将 Token 存入缓存，默认 30 秒过期
     *
     * @param token token 字符串
     * @param email 关联的用户邮箱
     */
    public void put(String token, String email) {
        long expireTime = System.currentTimeMillis() + 30 * 1000; // 30 秒后过期
        cache.put(token, new CacheItem(email, expireTime));
    }

    /**
     * 获取并删除 token
     * 保证取出立即删除（防重放）
     *
     * @param token token 字符串
     * @return 对应的 email，如果不存在或已过期则返回 null
     */
    public String getAndRemove(String token) {
        CacheItem item = cache.remove(token);
        if (item != null && item.getExpireTime() >= System.currentTimeMillis()) {
            return item.getEmail();
        }
        return null;
    }

    /**
     * 定时清理过期缓存 (每 10 秒执行一次)
     */
    @Scheduled(fixedRate = 10000)
    public void cleanUp() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().getExpireTime() < now);
    }

    /**
     * 内部类：缓存项
     */
    private static class CacheItem {
        private final String email;
        private final long expireTime;

        public CacheItem(String email, long expireTime) {
            this.email = email;
            this.expireTime = expireTime;
        }

        public String getEmail() {
            return email;
        }

        public long getExpireTime() {
            return expireTime;
        }
    }
}
