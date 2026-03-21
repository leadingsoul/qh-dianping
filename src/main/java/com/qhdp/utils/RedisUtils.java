package com.qhdp.utils;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    private void checkKey(String key) {
        if (StrUtil.isBlank(key)) {
            throw new IllegalArgumentException("Redis key 不能为 null、空字符串或空白字符");
        }
    }

    // 批量 key 校验
    private void checkKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("Redis keys 集合不能为 null 或空");
        }
        for (String key : keys) {
            checkKey(key);
        }
    }

    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存并设置过期时间
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间（秒）
     */
    public void set(String key, Object value, long timeout) {
        checkKey(key);
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置Redis中的键值对，并指定过期时间
     * @param redisKey Redis中的键
     * @param value 要存储的值
     * @param cacheTtl 过期时间
     * @param timeUnit 时间单位
     */
    public void set(String redisKey, Object value, Long cacheTtl, TimeUnit timeUnit) {
        checkKey(redisKey);
        redisTemplate.opsForValue().set(redisKey, value, cacheTtl, timeUnit);
    }

    /**
     * 获取缓存
     * @param key 缓存键
     * @return 缓存值
     */
    public Object get(String key) {
        checkKey(key);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存
     * @param key 缓存键
     * @param clazz 缓存值类型
     * @return 缓存值
     */
    public <T> T get(String key, Class<T> clazz) {
        checkKey(key);
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }
        // JSON 反序列化（如果你的 Redis 存的是 JSON）
        return JSONUtil.toBean(JSONUtil.toJsonStr(obj), clazz);
    }

    /**
     * 获取集合类型缓存（List / Set）
     * @param key 缓存键
     * @param collectionClass 集合类型（List.class / Set.class）
     * @param elementClass 集合内元素类型
     * @return 泛型集合
     */
    @SuppressWarnings("unchecked")
    public <T, C extends java.util.Collection<T>> C get(String key, Class<C> collectionClass, Class<T> elementClass) {
        checkKey(key);
        Object obj = redisTemplate.opsForValue().get(key);
        if (ObjectUtil.isNull(obj)) {
            return null;
        }
        // 转 JSON 字符串，再反序列化为指定集合+指定元素类型
        JSONArray jsonArray = JSONUtil.parseArray(obj);
        // 2. 转成指定元素类型的 List
        List<T> list = jsonArray.toList(elementClass);
        // 3. 根据传入的集合类型（List/Set）返回对应实例
        if (Set.class.isAssignableFrom(collectionClass)) {
            return (C) new HashSet<>(list);
        } else {
            return (C) list;
        }
    }

    /**
     * 根据指定的键和元素类型获取列表
     * 这是一个泛型方法，可以返回任意类型的列表
     *
     * @param <T> 列表中元素的类型
     * @param key 用于获取列表的键值
     * @param elementClass 列表中元素的Class对象
     * @return 返回指定类型的列表，如果不存在则返回null
     */
    @SuppressWarnings("unchecked")
     public <T> List<T> getList(String key, Class<T> elementClass) {
            return get(key, List.class, elementClass);
    }
    /**
     * 根据指定的键和元素类型获取列表
     * 这是一个泛型方法，可以返回任意类型的列表
     *
     * @param <T> 列表中元素的类型
     * @param key 用于获取列表的键值
     * @param elementClass 列表中元素的Class对象
     * @return 返回指定类型的集合，如果不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getSet(String key, Class<T> elementClass) {
        return get(key, Set.class, elementClass);
    }

    /**
     * 从Redis获取分页对象 Page<T>
     * @param key redis键
     * @param typeReference 泛型类型（解决Page<T>泛型擦除问题）
     * @return 分页对象
     */
    public <T> Page<T> getPage(String key, TypeReference<Page<T>> typeReference) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        // 反序列化为Page<T>
        return JSONUtil.toBean(JSONUtil.toJsonStr(value), typeReference,true);
    }
    /**
     * 删除缓存
     * @param key 缓存键
     */
    public void delete(String key) {
        checkKey(key);
        redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     * @param keys 缓存键集合
     */
    public void delete(Collection<String> keys) {
        checkKeys(keys);
        redisTemplate.delete(keys);
    }

    /**
     * 设置过期时间
     * @param key 缓存键
     * @param timeout 过期时间（秒）
     * @return 是否成功
     */
    public Boolean expire(String key, long timeout) {
        checkKey(key);
        return redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        checkKey(key);
        return redisTemplate.hasKey(key);
    }

    /**
     * 根据前缀获取所有匹配的key
     * @param pattern key前缀
     * @return 匹配的key集合
     */
    public Set<String> keys(String pattern) {
        checkKey(pattern);
        return redisTemplate.keys(pattern);
    }

    /**
     * 根据前缀删除所有匹配的key
     * @param pattern key前缀
     */
    public void deleteByPattern(String pattern) {
        checkKey(pattern);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 设置Hash缓存
     * @param key 缓存键
     * @param hashKey Hash键
     * @param value Hash值
     */
    public void hSet(String key, String hashKey, Object value) {
        checkKey(key);
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 获取Hash缓存
     * @param key 缓存键
     * @param hashKey Hash键
     * @return Hash值
     */
    public Object hGet(String key, String hashKey) {
        checkKey(key);
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 设置整个Hash缓存
     * @param key 缓存键
     * @param map Hash表
     */
    public void hSetAll(String key, Map<String, Object> map) {
        checkKey(key);
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取整个Hash缓存
     * @param key 缓存键
     * @return Hash表
     */
    public Map<Object, Object> hGetAll(String key) {
        checkKey(key);
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除Hash缓存中的某个键
     * @param key 缓存键
     * @param hashKey Hash键
     */
    public void hDelete(String key, Object... hashKey) {
        checkKey(key);
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    /**
     * 判断Hash缓存中是否存在某个键
     * @param key 缓存键
     * @param hashKey Hash键
     * @return 是否存在
     */
    public Boolean hHasKey(String key, String hashKey) {
        checkKey(key);
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    /**
     * set添加单个元素
     * @param key 键
     * @param value 值
     * @return 添加成功的数量
     */
    public Long sAdd(String key, Object value) {
        checkKey(key);
        return redisTemplate.opsForSet().add(key, value);
    }

    /**
     * set批量添加元素
     * @param key 键
     * @param values 值集合
     * @return 添加成功的数量
     */
    public Long sAdd(String key, Collection<Object> values) {
        checkKey(key);
        return redisTemplate.opsForSet().add(key, values.toArray());
    }

    /**
     * set移除单个元素
     * @param key 键
     * @param value 值
     * @return 移除成功的数量
     */
    public Long sRemove(String key, Object value) {
        checkKey(key);
        return redisTemplate.opsForSet().remove(key, value);
    }

    /**
     * set批量移除元素
     * @param key 键
     * @param values 值集合
     * @return 移除成功的数量
     */
    public Long sRemove(String key, Collection<Object> values) {
        checkKey(key);
        return redisTemplate.opsForSet().remove(key, values.toArray());
    }

    /**
     * 移除并返回集合的一个随机元素
     * @param key 键
     * @return 随机元素
     */
    public Object sPop(String key) {
        checkKey(key);
        return redisTemplate.opsForSet().pop(key);
    }

    /**
     * 将元素value从一个集合移到另一个集合
     * @param key 源键
     * @param value 元素
     * @param destKey 目标键
     * @return 是否移动成功
     */
    public Boolean sMove(String key, Object value, String destKey) {
        checkKey(key);
        checkKey(destKey);
        return redisTemplate.opsForSet().move(key, value, destKey);
    }

    /**
     * 获取集合的大小
     * @param key 键
     * @return 集合大小
     */
    public Long sSize(String key) {
        checkKey(key);
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 判断集合是否包含value
     * @param key 键
     * @param value 值
     * @return 是否包含
     */
    public Boolean sIsMember(String key, Object value) {
        checkKey(key);
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取两个集合的交集
     * @param key 键
     * @param otherKey 另一个键
     * @return 交集集合
     */
    public Set<Object> sIntersect(String key, String otherKey) {
        checkKey(key);
        checkKey(otherKey);
        return redisTemplate.opsForSet().intersect(key, otherKey);
    }

    /**
     * 获取key集合与多个集合的交集
     * @param key 键
     * @param otherKeys 多个键集合
     * @return 交集集合
     */
    public Set<Object> sIntersect(String key, Collection<String> otherKeys) {
        checkKey(key);
        return redisTemplate.opsForSet().intersect(key, otherKeys);
    }

    /**
     * key集合与otherKey集合的交集存储到destKey集合中
     * @param key 键
     * @param otherKey 另一个键
     * @param destKey 存储目标键
     * @return 交集元素数量
     */
    public Long sIntersectAndStore(String key, String otherKey, String destKey) {
        checkKey(key);
        checkKey(otherKey);
        return redisTemplate.opsForSet().intersectAndStore(key, otherKey, destKey);
    }

    /**
     * key集合与多个集合的交集存储到destKey集合中
     * @param key 键
     * @param otherKeys 多个键集合
     * @param destKey 存储目标键
     * @return 交集元素数量
     */
    public Long sIntersectAndStore(String key, Collection<String> otherKeys, String destKey) {
        checkKey(key);
        checkKey(destKey);
        return redisTemplate.opsForSet().intersectAndStore(key, otherKeys, destKey);
    }

    /**
     * 获取两个集合的并集
     * @param key 键
     * @param otherKey 另一个键
     * @return 并集集合
     */
    public Set<Object> sUnion(String key, String otherKey) {
        checkKey(key);
        checkKey(otherKey);
        return redisTemplate.opsForSet().union(key, otherKey);
    }

    /**
     * 获取key集合与多个集合的并集
     * @param key 键
     * @param otherKeys 多个键集合
     * @return 并集集合
     */
    public Set<Object> sUnion(String key, Collection<String> otherKeys) {
        checkKey(key);
        checkKeys(otherKeys);
        return redisTemplate.opsForSet().union(key, otherKeys);
    }

    /**
     * key集合与otherKey集合的并集存储到destKey中
     * @param key 键
     * @param otherKey 另一个键
     * @param destKey 存储目标键
     * @return 并集元素数量
     */
    public Long sUnionAndStore(String key, String otherKey, String destKey) {
        checkKey(key);
        checkKey(otherKey);
        checkKey(destKey);
        return redisTemplate.opsForSet().unionAndStore(key, otherKey, destKey);
    }

    /**
     * key集合与多个集合的并集存储到destKey中
     * @param key 键
     * @param otherKeys 多个键集合
     * @param destKey 存储目标键
     * @return 并集元素数量
     */
    public Long sUnionAndStore(String key, Collection<String> otherKeys, String destKey) {
        checkKey(key);
        checkKeys(otherKeys);
        checkKey(destKey);
        return redisTemplate.opsForSet().unionAndStore(key, otherKeys, destKey);
    }

    /**
     * 获取两个集合的差集
     * @param key 键
     * @param otherKey 另一个键
     * @return 差集集合
     */
    public Set<Object> sDifference(String key, String otherKey) {
        checkKey(key);
        checkKey(otherKey);
        return redisTemplate.opsForSet().difference(key, otherKey);
    }

    /**
     * 获取key集合与多个集合的差集
     * @param key 键
     * @param otherKeys 多个键集合
     * @return 差集集合
     */
    public Set<Object> sDifference(String key, Collection<String> otherKeys) {
        checkKey(key);
        checkKeys(otherKeys);
        return redisTemplate.opsForSet().difference(key, otherKeys);
    }

    /**
     * key集合与otherKey集合的差集存储到destKey中
     * @param key 键
     * @param otherKey 另一个键
     * @param destKey 存储目标键
     * @return 差集元素数量
     */
    public Long sDifferenceAndStore(String key, String otherKey, String destKey) {
        checkKey(key);
        checkKey(otherKey);
        checkKey(destKey);
        return redisTemplate.opsForSet().differenceAndStore(key, otherKey, destKey);
    }

    /**
     * key集合与多个集合的差集存储到destKey中
     * @param key 键
     * @param otherKeys 多个键集合
     * @param destKey 存储目标键
     * @return 差集元素数量
     */
    public Long sDifferenceAndStore(String key, Collection<String> otherKeys, String destKey) {
        checkKey(key);
        checkKeys(otherKeys);
        checkKey(destKey);
        return redisTemplate.opsForSet().differenceAndStore(key, otherKeys, destKey);
    }

    /**
     * 获取集合所有元素
     * @param key 键
     * @return 集合所有元素
     */
    public Set<Object> sMembers(String key) {
        checkKey(key);
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 随机获取集合中的一个元素
     * @param key 键
     * @return 随机元素
     */
    public Object sRandomMember(String key) {
        checkKey(key);
        return redisTemplate.opsForSet().randomMember(key);
    }

    /**
     * 随机获取集合中count个元素（可重复）
     * @param key 键
     * @param count 数量
     * @return 随机元素列表
     */
    public List<Object> sRandomMembers(String key, long count) {
        checkKey(key);
        return redisTemplate.opsForSet().randomMembers(key, count);
    }

    /**
     * 随机获取集合中count个元素并且去除重复
     * @param key 键
     * @param count 数量
     * @return 去重随机元素集合
     */
    public Set<Object> sDistinctRandomMembers(String key, long count) {
        checkKey(key);
        return redisTemplate.opsForSet().distinctRandomMembers(key, count);
    }

    /**
     * 游标遍历Set集合
     * @param key 键
     * @param options 遍历选项
     * @return 游标
     */
    public Cursor<Object> sScan(String key, ScanOptions options) {
        checkKey(key);
        return redisTemplate.opsForSet().scan(key, options);
    }


    /**
     * 将列表放入缓存
     * @param key 缓存键
     * @param value 列表值
     * @return 列表长度
     */
    public Long lPush(String key, Object value) {
        checkKey(key);
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 将列表放入缓存
     * @param key 缓存键
     * @param value 列表值
     * @param timeout 过期时间（秒）
     * @return 列表长度
     */
    public Long lPush(String key, Object value, long timeout) {
        checkKey(key);
        Long count = redisTemplate.opsForList().rightPush(key, value);
        expire(key, timeout);
        return count;
    }

    /**
     * 将多个值放入列表缓存
     * @param key 缓存键
     * @param values 值列表
     * @return 列表长度
     */
    public Long lPushAll(String key, List<Object> values) {
        checkKey(key);
        return redisTemplate.opsForList().rightPushAll(key, values);
    }

    /**
     * 将多个值放入列表缓存并设置过期时间
     * @param key 缓存键
     * @param values 值列表
     * @param timeout 过期时间（秒）
     * @return 列表长度
     */
    public Long lPushAll(String key, List<Object> values, long timeout) {
        checkKey(key);
        Long count = redisTemplate.opsForList().rightPushAll(key, values);
        expire(key, timeout);
        return count;
    }

    /**
     * 获取列表缓存
     * @param key 缓存键
     * @param start 开始索引
     * @param end 结束索引
     * @return 列表
     */
    public List<Object> lRange(String key, long start, long end) {
        checkKey(key);
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     * @param key 缓存键
     * @return 列表长度
     */
    public Long lSize(String key) {
        checkKey(key);
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 获取列表中指定索引的值
     * @param key 缓存键
     * @param index 索引
     * @return 值
     */
    public Object lIndex(String key, long index) {
        checkKey(key);
        return redisTemplate.opsForList().index(key, index);
    }

    /**
     * 移除列表中的值
     * @param key 缓存键
     * @param count 移除数量
     * @param value 值
     * @return 移除数量
     */
    public Long lRemove(String key, long count, Object value) {
        checkKey(key);
        return redisTemplate.opsForList().remove(key, count, value);
    }

    /**
     * 向有序集合添加元素，如果已存在则更新分数
     * @param key 缓存键
     * @param value 值
     * @param score 分数
     * @return 是否成功
     */
    public Boolean zAdd(String key, Object value, double score) {
        checkKey(key);
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 增加有序集合中元素的分数
     * @param key 缓存键
     * @param value 值
     * @param delta 增加的分数
     * @return 新的分数
     */
    public Double zIncrementScore(String key, Object value, double delta) {
        checkKey(key);
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * 获取有序集合中元素的分数
     * @param key 缓存键
     * @param value 值
     * @return 分数
     */
    public Double zScore(String key, Object value) {
        checkKey(key);
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * 获取有序集合的大小
     * @param key 缓存键
     * @return 集合大小
     */
    public Long zSize(String key) {
        checkKey(key);
        return redisTemplate.opsForZSet().size(key);
    }

    /**
     * 获取有序集合中指定分数范围的元素
     * @param key 缓存键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素集合
     */
    public Set<Object> zRangeByScore(String key, double min, double max) {
        checkKey(key);
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    /**
     * 获取有序集合中指定排名范围的元素（从高到低）
     * @param key 缓存键
     * @param start 开始排名（0表示第一个）
     * @param end 结束排名
     * @return 元素集合
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        checkKey(key);
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取有序集合中指定排名范围的元素和分数（从高到低）
     * @param key 缓存键
     * @param start 开始排名（0表示第一个）
     * @param end 结束排名
     * @return 元素和分数的集合
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        checkKey(key);
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /**
     * 移除有序集合中的元素
     * @param key 缓存键
     * @param values 要移除的元素
     * @return 移除的数量
     */
    public Long zRemove(String key, Object... values) {
        checkKey(key);
        return redisTemplate.opsForZSet().remove(key, values);
    }

    /**
     * 向Redis GEO添加单个经纬度坐标
     * @param key GEO的key
     * @param longitude 经度
     * @param latitude 纬度
     * @param member 成员名称（如店铺id、用户id）
     */
    public void geoAdd(String key, double longitude, double latitude, String member) {
        checkKey(key);
        redisTemplate.opsForGeo().add(key, new Point(longitude, latitude), member);
    }

    /**
     * 批量添加Redis GEO坐标
     * @param key GEO的key
     * @param geoMap key=成员, value=Point(经纬度)
     */
    public void geoAdd(String key, RedisGeoCommands.GeoLocation<Object> geoMap) {
        checkKey(key);
        redisTemplate.opsForGeo().add(key, geoMap);
    }

    /**
     * 获取指定成员的经纬度坐标
     * @param key GEO的key
     * @param members 成员列表
     * @return 坐标列表
     */
    public List<Point> geoPosition(String key, String... members) {
        checkKey(key);
        return redisTemplate.opsForGeo().position(key, members);
    }

    /**
     * 获取两个成员之间的距离
     * @param key GEO的key
     * @param member1 成员1
     * @param member2 成员2
     * @return 距离（米）
     */
    public Distance geoDistance(String key, String member1, String member2) {
        checkKey(key);
        return redisTemplate.opsForGeo().distance(key, member1, member2);
    }

    /**
     * 【你要的核心方法】
     * 按坐标+半径搜索附近的成员（带距离）
     * @param key GEO key
     * @param x 经度
     * @param y 纬度
     * @param radius 半径（米）
     * @param limit 限制返回条数
     * @return GeoResults 包含成员+距离+坐标
     */
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoSearch(
            String key,
            double x,
            double y,
            double radius,
            int limit
    ) {
        checkKey(key);
        return redisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(radius),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .limit(limit)
        );
    }

    /**
     * 按成员搜索附近的成员
     * @param key GEO key
     * @param member 中心成员
     * @param radius 半径（米）
     * @param limit 限制返回条数
     * @return 结果
     */
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoSearchByMember(
            String key,
            String member,
            double radius,
            int limit
    ) {
        checkKey(key);
        return redisTemplate.opsForGeo().search(
                key,
                GeoReference.fromMember(member),
                new Distance(radius),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .limit(limit)
        );
    }

    /**
     * 删除GEO中的指定成员
     * @param key GEO key
     * @param members 要删除的成员
     * @return 删除数量
     */
    public Long geoRemove(String key, String... members) {
        checkKey(key);
        return redisTemplate.opsForGeo().remove(key, members);
    }
}
