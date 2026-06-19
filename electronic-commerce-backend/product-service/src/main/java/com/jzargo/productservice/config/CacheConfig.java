package com.jzargo.productservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@EnableCaching
public class CacheConfig {

    private final ApplicationPropertyStorage applicationPropertyStorage;

    @Bean
    public CacheManagerCustomizer<ConcurrentMapCacheManager> cacheManagerCacheManagerCustomizer() {
        return (cacheManager) -> cacheManager.setAllowNullValues(false);
    }

    @Bean
    public CacheManager cacheManager(){
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

        List<ApplicationPropertyStorage.Caching.CacheProperties> caches =
                applicationPropertyStorage.getCaching().getCaches();

        caches.forEach(
                (cache) -> caffeineCacheManager.
                        registerCustomCache(
                                cache.getName(),
                                Caffeine.newBuilder()
                                        .expireAfterAccess(cache.getExpireAfterAccessInSeconds(), TimeUnit.SECONDS)
                                        .expireAfterWrite(cache.getExpireAfterWriteInSeconds(), TimeUnit.SECONDS)
                                        .maximumSize(cache.getMaxSize())
                                        .build()
                        )
        );

        return caffeineCacheManager;
    }
}