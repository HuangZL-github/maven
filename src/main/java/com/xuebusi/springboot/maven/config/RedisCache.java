package com.xuebusi.springboot.maven.config;

import org.apache.ibatis.cache.Cache;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 将Redis作为二级缓存
 * Mybatis的二级缓存可以自动地对数据库的查询做缓存，并且可以在更新数据时同时自动地更新缓存。
 * 实现Mybatis的二级缓存很简单，只需要新建一个类实现org.apache.ibatis.cache.Cache接口即可。
 * 该接口共有以下 7个方法：
 *    1.String getId()：mybatis缓存操作对象的标识符。一个mapper对应一个mybatis的缓存操作对象。
 *    2.void putObject(Object key, Object value)：将查询结果塞入缓存。
 *    3.Object getObject(Object key)：从缓存中获取被缓存的查询结果。
 *    4.Object removeObject(Object key)：从缓存中删除对应的key、value。只有在回滚时触发。一般我们也可以不用实现，具体使用方式请参考：org.apache.ibatis.cache.decorators.TransactionalCache。
 *    5.void clear()：发生更新时，清除缓存。
 *    6.int getSize()：可选实现。返回缓存的数量。
 *    7.ReadWriteLock getReadWriteLock()：可选实现。用于实现原子性的缓存操作。
 *
 * @author zhuzhe
 * @date 2018/5/9 9:51
 * @email 1529949535@qq.com
 */
public class RedisCache implements Cache {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final String id; // cache instance id
    private static RedisTemplate redisTemplate = RedisCache.getRedisTemplate();

    private static final long EXPIRE_TIME_IN_MINUTES = 30; // redis过期时间

    public RedisCache(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Put query result to redis
     *
     * @param key
     * @param value
     */
    @Override
    @SuppressWarnings("unchecked")
    public void putObject(Object key, Object value) {
        ValueOperations opsForValue = redisTemplate.opsForValue();
        System.out.println("更新redis缓存:key=" + key + ",value=" + value);
        opsForValue.set(key, value, EXPIRE_TIME_IN_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Get cached query result from redis
     *
     * @param key
     * @return
     */
    @Override
    public Object getObject(Object key) {
        ValueOperations opsForValue = redisTemplate.opsForValue();
        Object value = opsForValue.get(key);
        System.out.println("查询redis缓存:key=" + key + ",value=" + value);
        return value;
    }

    /**
     * Remove cached query result from redis
     *
     * @param key
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object removeObject(Object key) {
        redisTemplate.delete(key);
        System.out.println("移除redis缓存:key=" + key);
        return null;
    }

    /**
     * Clears this cache instance
     */
    @Override
    public void clear() {
        redisTemplate.execute((RedisCallback) connection -> {
            System.out.println("清除redis缓存实例");
            connection.flushDb();
            return null;
        });
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }

    private static RedisTemplate getRedisTemplate() {
        if (redisTemplate == null) {
            redisTemplate = ApplicationContextHolder.getBean("redisTemplate");
            System.out.println("==============从Spring容器获取redisTemplate实例:" + redisTemplate);
        }
        return redisTemplate;
    }
}