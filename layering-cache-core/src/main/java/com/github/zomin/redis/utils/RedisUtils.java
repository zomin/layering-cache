package com.github.zomin.redis.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by Kalend Zhang on 2019/6/21.
 *
 * @author Kalend
 */
@Component
public class RedisUtils {

    @Resource
    private RedisTemplate redisTemplate;

    public RedisUtils() {
    }

    public RedisUtils(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // -------------------key相关操作---------------------

    /**
     * 删除key
     *
     * @param key key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除key
     *
     * @param keys keys
     */
    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 序列化key
     *
     * @param key key
     *
     * @return 序列化
     */
    public byte[] dump(String key) {
        return redisTemplate.dump(key);
    }

    /**
     * 是否存在key
     *
     * @param key redis key
     *
     * @return Boolean 成功或者失败
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     *
     * @param key     redis key
     * @param timeout 过期时间
     * @param unit    时区
     *
     * @return Boolean 成功或者失败
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 设置过期时间
     *
     * @param key  redis key
     * @param date 过期时间
     *
     * @return Boolean 成功或者失败
     */
    public Boolean expireAt(String key, Date date) {
        return redisTemplate.expireAt(key, date);
    }

    /**
     * 查找匹配的key
     *
     * @param pattern 查询表达式
     *
     * @return Set key集合
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 将当前数据库的 key 移动到给定的数据库 db 当中
     *
     * @param key     redis key
     * @param dbIndex redis dbindex
     *
     * @return Boolean 成功或者失败
     */
    public Boolean move(String key, int dbIndex) {
        return redisTemplate.move(key, dbIndex);
    }

    /**
     * 移除 key 的过期时间，key 将持久保持
     *
     * @param key redis key
     *
     * @return Boolean 成功或者失败
     */
    public Boolean persist(String key) {
        return redisTemplate.persist(key);
    }

    /**
     * 返回 key 的剩余的过期时间
     *
     * @param key  redis key
     * @param unit 时区
     *
     * @return Long 剩余过期时间
     */
    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    /**
     * 返回 key 的剩余的过期时间
     *
     * @param key redis key
     *
     * @return Long 剩余过期时间
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 从当前数据库中随机返回一个 key
     *
     * @return String redis key
     */
    public String randomKey() {
        return String.valueOf(redisTemplate.randomKey());
    }

    /**
     * 修改 key 的名称
     *
     * @param oldKey 旧key
     * @param newKey 新key
     */
    public void rename(String oldKey, String newKey) {
        redisTemplate.rename(oldKey, newKey);
    }

    /**
     * 仅当 newkey 不存在时，将 oldKey 改名为 newkey
     *
     * @param oldKey 旧key
     * @param newKey 新key
     *
     * @return Boolean 成功或者失败
     */
    public Boolean renameIfAbsent(String oldKey, String newKey) {
        return redisTemplate.renameIfAbsent(oldKey, newKey);
    }

    /**
     * 返回 key 所储存的值的类型
     *
     * @param key redis key
     *
     * @return DataType
     */
    public DataType type(String key) {
        return redisTemplate.type(key);
    }

    /** -------------------string相关操作--------------------- */

    /**
     * 设置指定 key 的值
     *
     * @param key   redis key
     * @param value value
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 获取指定 key 的值
     *
     * @param key redis key
     *
     * @return String value
     */
    public String get(String key) {
        Object objByRedis = redisTemplate.opsForValue().get(key);
        if (Objects.isNull(objByRedis)) {
            return null;
        }
        return String.valueOf(objByRedis);
    }

    /**
     * 返回 key 中字符串值的子字符
     *
     * @param key   redis key
     * @param start 开始字符位置
     * @param end   结束字符位置
     *
     * @return value
     */
    public String getRange(String key, long start, long end) {
        return redisTemplate.opsForValue().get(key, start, end);
    }

    /**
     * 将给定 key 的值设为 value ，并返回 key 的旧值(old value)
     *
     * @param key   redis key
     * @param value value
     *
     * @return value
     */
    public String getAndSet(String key, String value) {
        return String.valueOf(redisTemplate.opsForValue().getAndSet(key, value));
    }

    /**
     * 对 key 所储存的字符串值，获取指定偏移量上的位(bit)
     *
     * @param key    redis key
     * @param offset 指定偏移量
     *
     * @return Boolean 成功或者失败
     */
    public Boolean getBit(String key, long offset) {
        return redisTemplate.opsForValue().getBit(key, offset);
    }

    /**
     * 批量获取
     *
     * @param keys keys
     *
     * @return values
     */
    public List<?> multiGet(Collection<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * 设置ASCII码, 字符串'a'的ASCII码是97, 转为二进制是'01100001', 此方法是将二进制第offset位值变为value
     *
     * @param key    key
     * @param offset 位置
     * @param value  值,true为1, false为0
     *
     * @return Boolean 成功或者失败
     */
    public boolean setBit(String key, long offset, boolean value) {
        return redisTemplate.opsForValue().setBit(key, offset, value);
    }

    /**
     * 将值 value 关联到 key ，并将 key 的过期时间设为 timeout
     *
     * @param key     key
     * @param value   value
     * @param timeout 过期时间
     * @param unit    时间单位, 天:TimeUnit.DAYS 小时:TimeUnit.HOURS 分钟:TimeUnit.MINUTES
     *                秒:TimeUnit.SECONDS 毫秒:TimeUnit.MILLISECONDS
     */
    public void setEx(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 只有在 key 不存在时设置 key 的值
     *
     * @param key   key
     * @param value value
     *
     * @return 之前已经存在返回false, 不存在返回true
     */
    public boolean setIfAbsent(String key, String value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    /**
     * 用 value 参数覆写给定 key 所储存的字符串值，从偏移量 offset 开始
     *
     * @param key    key
     * @param value  value
     * @param offset 从指定位置开始覆写
     */
    public void setRange(String key, String value, long offset) {
        redisTemplate.opsForValue().set(key, value, offset);
    }

    /**
     * 获取字符串的长度
     *
     * @param key key
     *
     * @return Long 字符长度
     */
    public Long size(String key) {
        return redisTemplate.opsForValue().size(key);
    }

    /**
     * 批量添加
     *
     * @param maps map
     */
    public void multiSet(Map<String, String> maps) {
        redisTemplate.opsForValue().multiSet(maps);
    }

    /**
     * 同时设置一个或多个 key-value 对，当且仅当所有给定 key 都不存在
     *
     * @param maps map
     *
     * @return 之前已经存在返回false, 不存在返回true
     */
    public boolean multiSetIfAbsent(Map<String, String> maps) {
        return redisTemplate.opsForValue().multiSetIfAbsent(maps);
    }

    /**
     * 增加(自增长), 负数则为自减
     *
     * @param key       key
     * @param increment increment
     *
     * @return Long
     */
    public Long incrBy(String key, long increment) {
        return redisTemplate.opsForValue().increment(key, increment);
    }

    /**
     * @param key       key
     * @param increment increment
     *
     * @return Double
     */
    public Double incrByFloat(String key, double increment) {
        return redisTemplate.opsForValue().increment(key, increment);
    }

    /**
     * 追加到末尾
     *
     * @param key   key
     * @param value value
     *
     * @return Integer
     */
    public Integer append(String key, String value) {
        return redisTemplate.opsForValue().append(key, value);
    }

    // -------------------hash相关操作-------------------------

    /**
     * 获取存储在哈希表中指定字段的值
     *
     * @param key   key
     * @param field field
     *
     * @return Object
     */
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 获取所有给定字段的值
     *
     * @param key key
     *
     * @return Map
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 获取所有给定字段的值
     *
     * @param key    key
     * @param fields fields
     *
     * @return List
     */
    public List<Object> hMultiGet(String key, Collection<Object> fields) {
        return redisTemplate.opsForHash().multiGet(key, fields);
    }

    public void hPut(String key, String hashKey, String value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public void hPutAll(String key, Map<String, String> maps) {
        redisTemplate.opsForHash().putAll(key, maps);
    }

    /**
     * 仅当hashKey不存在时才设置
     *
     * @param key     key
     * @param hashKey hashKey
     * @param value   value
     *
     * @return Boolean
     */
    public Boolean hPutIfAbsent(String key, String hashKey, String value) {
        return redisTemplate.opsForHash().putIfAbsent(key, hashKey, value);
    }

    /**
     * 查看哈希表 key 中，指定的字段是否存在
     *
     * @param key   key
     * @param field field
     *
     * @return boolean
     */
    public boolean hExists(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * 为哈希表 key 中的指定字段的整数值加上增量 increment
     *
     * @param key       key
     * @param field     field
     * @param increment increment
     *
     * @return Long
     */
    public Long hIncrBy(String key, Object field, long increment) {
        return redisTemplate.opsForHash().increment(key, field, increment);
    }

    /**
     * 获取所有哈希表中的字段
     *
     * @param key key
     *
     * @return Set
     */
    public Set<Object> hKeys(String key) {
        return redisTemplate.opsForHash().keys(key);
    }

    /**
     * 获取哈希表中字段的数量
     *
     * @param key key
     *
     * @return Long
     */
    public Long hSize(String key) {
        return redisTemplate.opsForHash().size(key);
    }

    /**
     * 获取哈希表中所有值
     *
     * @param key key
     *
     * @return List
     */
    public List<Object> hValues(String key) {
        return redisTemplate.opsForHash().values(key);
    }

    /**
     * 迭代哈希表中的键值对
     *
     * @param key     key
     * @param options options
     *
     * @return Cursor
     */
    public Cursor<Map.Entry<Object, Object>> hScan(String key, ScanOptions options) {
        return redisTemplate.opsForHash().scan(key, options);
    }

    // ------------------------list相关操作----------------------------

    /**
     * 通过索引获取列表中的元素
     *
     * @param key   key
     * @param index index
     *
     * @return String
     */
    public String lIndex(String key, long index) {
        return String.valueOf(redisTemplate.opsForList().index(key, index));
    }

    /**
     * 获取列表指定范围内的元素
     *
     * @param key   key
     * @param start 开始位置, 0是开始位置
     * @param end   结束位置, -1返回所有
     *
     * @return List
     */
    public List<String> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 存储在list头部
     *
     * @param key   key
     * @param value value
     *
     * @return Long
     */
    public Long lLeftPush(String key, String value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * @param key   key
     * @param value value
     *
     * @return Long
     */
    public Long lLeftPushAll(String key, String... value) {
        return redisTemplate.opsForList().leftPushAll(key, value);
    }

    /**
     * @param key   key
     * @param value value
     *
     * @return Long
     */
    public Long lLeftPushAll(String key, Collection<String> value) {
        return redisTemplate.opsForList().leftPushAll(key, value);
    }

    /**
     * 当list存在的时候才加入
     *
     * @param key   key
     * @param value value
     *
     * @return Long
     */
    public Long lLeftPushIfPresent(String key, String value) {
        return redisTemplate.opsForList().leftPushIfPresent(key, value);
    }

    /**
     * 如果pivot存在,再pivot前面添加
     *
     * @param key   key
     * @param pivot pivot
     * @param value value
     *
     * @return Long
     */
    public Long lLeftPush(String key, String pivot, String value) {
        return redisTemplate.opsForList().leftPush(key, pivot, value);
    }

    /**
     * @param key   key
     * @param value value
     *
     * @return Long
     */
    public Long lRightPush(String key, String value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * @param key   key
     * @param value value
     *
     * @return Long
     */
    public Long lRightPushAll(String key, String... value) {
        return redisTemplate.opsForList().rightPushAll(key, value);
    }

    /**
     * @param key   key
     * @param value value
     *
     * @return Long
     */
    public Long lRightPushAll(String key, Collection<String> value) {
        return redisTemplate.opsForList().rightPushAll(key, value);
    }

    /**
     * 为已存在的列表添加值
     *
     * @param key   key
     * @param value value
     *
     * @return Long
     */
    public Long lRightPushIfPresent(String key, String value) {
        return redisTemplate.opsForList().rightPushIfPresent(key, value);
    }

    /**
     * 在pivot元素的右边添加值
     *
     * @param key   key
     * @param pivot pivot
     * @param value value
     *
     * @return Long
     */
     public Long lRightPush(String key, String pivot, String value) {
         return redisTemplate.opsForList().rightPush(key, pivot, value);
     }

    /**
     * 通过索引设置列表元素的值
     *
     * @param key   key
     * @param index 位置
     * @param value value
     */
    public void lSet(String key, long index, String value) {
        redisTemplate.opsForList().set(key, index, value);
    }

    /**
     * 移出并获取列表的第一个元素
     *
     * @param key key
     *
     * @return 删除的元素
     */
    public String lLeftPop(String key) {
        return String.valueOf(redisTemplate.opsForList().leftPop(key));
    }

    /**
     * 移出并获取列表的第一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param key     key
     * @param timeout 等待时间
     * @param unit    时间单位
     *
     * @return String
     */
    public String lLeftPopWithTimeout(String key, long timeout, TimeUnit unit) {
        return String.valueOf(redisTemplate.opsForList().leftPop(key, timeout, unit));
    }

    /**
     * 移除并获取列表最后一个元素
     *
     * @param key key
     *
     * @return 删除的元素
     */
    public String lRightPop(String key) {
        return String.valueOf(redisTemplate.opsForList().rightPop(key));
    }

    /**
     * 移出并获取列表的最后一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param key     key
     * @param timeout 等待时间
     * @param unit    时间单位
     *
     * @return String
     */
    public String lRightPopWithTimeout(String key, long timeout, TimeUnit unit) {
        return String.valueOf(redisTemplate.opsForList().rightPop(key, timeout, unit));
    }

    /**
     * 移除列表的最后一个元素，并将该元素添加到另一个列表并返回
     *
     * @param sourceKey      sourceKey
     * @param destinationKey destinationKey
     *
     * @return String
     */
    public String lRightPopAndLeftPush(String sourceKey, String destinationKey) {
        return String.valueOf(redisTemplate.opsForList().rightPopAndLeftPush(sourceKey, destinationKey));
    }

    /**
     * 从列表中弹出一个值，将弹出的元素插入到另外一个列表中并返回它； 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param sourceKey      sourceKey
     * @param destinationKey destinationKey
     * @param timeout        timeout
     * @param unit           unit
     *
     * @return String
     */
    public String lRightPopAndLeftPushWithTimeout(String sourceKey, String destinationKey, long timeout,
                                                  TimeUnit unit) {
        return String.valueOf(redisTemplate.opsForList().rightPopAndLeftPush(sourceKey, destinationKey, timeout, unit));
    }

    /**
     * 删除集合中值等于value得元素
     *
     * @param key   key
     * @param index index=0, 删除所有值等于value的元素; index大于0, 从头部开始删除第一个值等于value的元素;
     *              index小于0, 从尾部开始删除第一个值等于value的元素;
     * @param value value
     *
     * @return Long
     */
    public Long lRemove(String key, long index, String value) {
        return redisTemplate.opsForList().remove(key, index, value);
    }

    /**
     * 裁剪list
     *
     * @param key   key
     * @param start start
     * @param end   end
     */
    public void lTrim(String key, long start, long end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    /**
     * 获取列表长度
     *
     * @param key key
     *
     * @return Long
     */
    public Long lLen(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // --------------------set相关操作--------------------------

    /**
     * set添加元素
     *
     * @param key    key
     * @param values values
     *
     * @return Long
     */
     public Long sAdd(String key, String... values) {
         return redisTemplate.opsForSet().add(key, values);
     }

    /**
     * set移除元素
     *
     * @param key    key
     * @param values values
     *
     * @return Long
     */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    /**
     * 移除并返回集合的一个随机元素
     *
     * @param key key
     *
     * @return String
     */
    public String sPop(String key) {
        return String.valueOf(redisTemplate.opsForSet().pop(key));
    }

    /**
     * 将元素value从一个集合移到另一个集合
     *
     * @param key     key
     * @param value   value
     * @param destKey destKey
     *
     * @return Boolean
     */
    public Boolean sMove(String key, String value, String destKey) {
        return redisTemplate.opsForSet().move(key, value, destKey);
    }

    /**
     * 获取集合的大小
     *
     * @param key key
     *
     * @return Long
     */
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 判断集合是否包含value
     *
     * @param key   key
     * @param value value
     *
     * @return Boolean
     */
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取两个集合的交集
     *
     * @param key      key
     * @param otherKey otherKey
     *
     * @return Set
     */
     public Set<String> sIntersect(String key, String otherKey) {
         return redisTemplate.opsForSet().intersect(key, otherKey);
     }

    /**
     * 获取key集合与多个集合的交集
     *
     * @param key       key
     * @param otherKeys otherKey
     *
     * @return Set
     */
    public Set<String> sIntersect(String key, Collection<String> otherKeys) {
        return redisTemplate.opsForSet().intersect(key, otherKeys);
    }

    /**
     * key集合与otherKey集合的交集存储到destKey集合中
     *
     * @param key      key
     * @param otherKey otherKey
     * @param destKey  destKey
     *
     * @return Long
     */
    public Long sIntersectAndStore(String key, String otherKey, String destKey) {
        return redisTemplate.opsForSet().intersectAndStore(key, otherKey, destKey);
    }

    /**
     * key集合与多个集合的交集存储到destKey集合中
     *
     * @param key       key
     * @param otherKeys otherKey
     * @param destKey   destKey
     *
     * @return Long
     */
    public Long sIntersectAndStore(String key, Collection<String> otherKeys, String destKey) {
        return redisTemplate.opsForSet().intersectAndStore(key, otherKeys, destKey);
    }

    /**
     * 获取两个集合的并集
     *
     * @param key       key
     * @param otherKeys otherKeys
     *
     * @return Set
     */
    public Set<String> sUnion(String key, String otherKeys) {
        return redisTemplate.opsForSet().union(key, otherKeys);
    }

    /**
     * 获取key集合与多个集合的并集
     *
     * @param key       key
     * @param otherKeys otherKeys
     *
     * @return Set
     */
    public Set<String> sUnion(String key, Collection<String> otherKeys) {
        return redisTemplate.opsForSet().union(key, otherKeys);
    }

    /**
     * key集合与otherKey集合的并集存储到destKey中
     *
     * @param key      key
     * @param otherKey otherKey
     * @param destKey  destKey
     *
     * @return Long
     */
    public Long sUnionAndStore(String key, String otherKey, String destKey) {
        return redisTemplate.opsForSet().unionAndStore(key, otherKey, destKey);
    }

    /**
     * key集合与多个集合的并集存储到destKey中
     *
     * @param key       key
     * @param otherKeys otherKeys
     * @param destKey   destKey
     *
     * @return Long
     */
    public Long sUnionAndStore(String key, Collection<String> otherKeys, String destKey) {
       return redisTemplate.opsForSet().unionAndStore(key, otherKeys, destKey);
    }

    /**
     * 获取两个集合的差集
     *
     * @param key      key
     * @param otherKey otherKey
     *
     * @return Set
     */
    public Set<String> sDifference(String key, String otherKey) {
       return redisTemplate.opsForSet().difference(key, otherKey);
    }

    /**
     * 获取key集合与多个集合的差集
     *
     * @param key       key
     * @param otherKeys otherKeys
     *
     * @return Set
     */
    public Set<String> sDifference(String key, Collection<String> otherKeys) {
       return redisTemplate.opsForSet().difference(key, otherKeys);
    }

    /**
     * key集合与otherKey集合的差集存储到destKey中
     *
     * @param key      key
     * @param otherKey otherKey
     * @param destKey  destKey
     *
     * @return Long
     */
    public Long sDifference(String key, String otherKey, String destKey) {
       return redisTemplate.opsForSet().differenceAndStore(key, otherKey, destKey);
    }

    /**
    * key集合与多个集合的差集存储到destKey中
    *
    * @param key       key
    * @param otherKeys otherKeys
    * @param destKey   destKey
    *
    * @return Long
    */
    public Long sDifference(String key, Collection<String> otherKeys, String destKey) {
       return redisTemplate.opsForSet().differenceAndStore(key, otherKeys, destKey);
    }

    /**
    * 获取集合所有元素
    *
    * @param key key
    *
    * @return Set
    */
    public Set<String> setMembers(String key) {
       return redisTemplate.opsForSet().members(key);
    }

    /**
    * 随机获取集合中的一个元素
    *
    * @param key key
    *
    * @return String
    */
    public String sRandomMember(String key) {
       return String.valueOf(redisTemplate.opsForSet().randomMember(key));
    }

    /**
    * 随机获取集合中count个元素
    *
    * @param key   key
    * @param count count
    *
    * @return List
    */
    public List<String> sRandomMembers(String key, long count) {
       return redisTemplate.opsForSet().randomMembers(key, count);
    }

    /**
    * 随机获取集合中count个元素并且去除重复的
    *
    * @param key   key
    * @param count count
    *
    * @return Set
    */
    public Set<String> sDistinctRandomMembers(String key, long count) {
       return redisTemplate.opsForSet().distinctRandomMembers(key, count);
    }

    /**
    * @param key     key
    * @param options options
    *
    * @return Cursor
    */
    public Cursor<String> sScan(String key, ScanOptions options) {
       return redisTemplate.opsForSet().scan(key, options);
    }

    //------------------zSet相关操作--------------------------------

    /**
    * 添加元素,有序集合是按照元素的score值由小到大排列
    *
    * @param key   key
    * @param value value
    * @param score score
    *
    * @return Boolean
    */
    public Boolean zAdd(String key, String value, double score) {
       return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
    * @param key    key
    * @param values values
    *
    * @return Long
    */
    public Long zAdd(String key, Set<ZSetOperations.TypedTuple<String>> values) {
       return redisTemplate.opsForZSet().add(key, values);
    }

    /**
    * @param key    key
    * @param values values
    *
    * @return Long
    */
    public Long zRemove(String key, Object... values) {
       return redisTemplate.opsForZSet().remove(key, values);
    }

    /**
    * 增加元素的score值，并返回增加后的值
    *
    * @param key   key
    * @param value value
    * @param delta delta
    *
    * @return Double
    */
    public Double zIncrementScore(String key, String value, double delta) {
       return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
    * 返回元素在集合的排名,有序集合是按照元素的score值由小到大排列
    *
    * @param key   key
    * @param value value
    *
    * @return 0表示第一位
    */
    public Long zRank(String key, Object value) {
       return redisTemplate.opsForZSet().rank(key, value);
    }

    /**
    * 返回元素在集合的排名,按元素的score值由大到小排列
    *
    * @param key   key
    * @param value value
    *
    * @return Long
    */
    public Long zReverseRank(String key, Object value) {
       return redisTemplate.opsForZSet().reverseRank(key, value);
    }

    /**
    * 获取集合的元素, 从小到大排序
    *
    * @param key   key
    * @param start 开始位置
    * @param end   结束位置, -1查询所有
    *
    * @return Set
    */
    public Set<String> zRange(String key, long start, long end) {
       return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
    * 获取集合元素, 并且把score值也获取
    *
    * @param key   key
    * @param start start
    * @param end   end
    *
    * @return Set
    */
    public Set<ZSetOperations.TypedTuple<String>> zRangeWithScores(String key, long start, long end) {
       return redisTemplate.opsForZSet().rangeWithScores(key, start, end);
    }

    /**
    * 根据Score值查询集合元素
    *
    * @param key key
    * @param min 最小值
    * @param max 最大值
    *
    * @return Set
    */
    public Set<String> zRangeByScore(String key, double min, double max) {
       return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    /**
    * 根据Score值查询集合元素, 从小到大排序
    *
    * @param key key
    * @param min 最小值
    * @param max 最大值
    *
    * @return Set
    */
    public Set<ZSetOperations.TypedTuple<String>> zRangeByScoreWithScores(String key, double min, double max) {
       return redisTemplate.opsForZSet().rangeByScoreWithScores(key, min, max);
    }

    /**
    * @param key   key
    * @param min   min
    * @param max   max
    * @param start start
    * @param end   end
    *
    * @return Set
    */
    public Set<ZSetOperations.TypedTuple<String>> zRangeByScoreWithScores(String key, double min, double max,
                                                                         long start, long end) {
       return redisTemplate.opsForZSet().rangeByScoreWithScores(key, min, max, start, end);
    }

    /**
    * 获取集合的元素, 从大到小排序
    *
    * @param key   key
    * @param start start
    * @param end   end
    *
    * @return Set
    */
    public Set<String> zReverseRange(String key, long start, long end) {
       return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
    * 获取集合的元素, 从大到小排序, 并返回score值
    *
    * @param key   key
    * @param start start
    * @param end   end
    *
    * @return Set
    */
    public Set<ZSetOperations.TypedTuple<String>> zReverseRangeWithScores(String key, long start, long end) {
       return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /**
    * 根据Score值查询集合元素, 从大到小排序
    *
    * @param key key
    * @param min min
    * @param max max
    *
    * @return Set
    */
    public Set<String> zReverseRangeByScore(String key, double min, double max) {
       return redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
    }

    /**
    * 根据Score值查询集合元素, 从大到小排序
    *
    * @param key key
    * @param min min
    * @param max max
    *
    * @return Set
    */
    public Set<ZSetOperations.TypedTuple<String>> zReverseRangeByScoreWithScores(String key, double min, double max) {
       return redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, min, max);
    }

    /**
    * @param key   key
    * @param min   min
    * @param max   max
    * @param start start
    * @param end   end
    *
    * @return Set
    */
    public Set<String> zReverseRangeByScore(String key, double min, double max, long start, long end) {
       return redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, start, end);
    }

    /**
    * 根据score值获取集合元素数量
    *
    * @param key key
    * @param min min
    * @param max max
    *
    * @return Long
    */
    public Long zCount(String key, double min, double max) {
       return redisTemplate.opsForZSet().count(key, min, max);
    }

    /**
    * 获取集合大小
    *
    * @param key key
    *
    * @return Long
    */
    public Long zSize(String key) {
       return redisTemplate.opsForZSet().size(key);
    }

    /**
    * 获取集合大小
    *
    * @param key key
    *
    * @return Long
    */
    public Long zCard(String key) {
       return redisTemplate.opsForZSet().zCard(key);
    }

    /**
    * 获取集合中value元素的score值
    *
    * @param key   key
    * @param value value
    *
    * @return Double
    */
    public Double zScore(String key, Object value) {
       return redisTemplate.opsForZSet().score(key, value);
    }

    /**
    * 移除指定索引位置的成员
    *
    * @param key   key
    * @param start start
    * @param end   end
    *
    * @return Long
    */
    public Long zRemoveRange(String key, long start, long end) {
       return redisTemplate.opsForZSet().removeRange(key, start, end);
    }

    /**
    * 根据指定的score值的范围来移除成员
    *
    * @param key key
    * @param min min
    * @param max max
    *
    * @return Long
    */
    public Long zRemoveRangeByScore(String key, double min, double max) {
       return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    /**
    * 获取key和otherKey的并集并存储在destKey中
    *
    * @param key      key
    * @param otherKey otherKey
    * @param destKey  destKey
    *
    * @return Long
    */
    public Long zUnionAndStore(String key, String otherKey, String destKey) {
       return redisTemplate.opsForZSet().unionAndStore(key, otherKey, destKey);
    }

    /**
    * @param key       key
    * @param otherKeys otherKeys
    * @param destKey   destKey
    *
    * @return Long
    */
    public Long zUnionAndStore(String key, Collection<String> otherKeys, String destKey) {
       return redisTemplate.opsForZSet().unionAndStore(key, otherKeys, destKey);
    }

    /**
    * 交集
    *
    * @param key      key
    * @param otherKey otherKey
    * @param destKey  destKey
    *
    * @return Long
    */
    public Long zIntersectAndStore(String key, String otherKey, String destKey) {
       return redisTemplate.opsForZSet().intersectAndStore(key, otherKey, destKey);
    }

    /**
    * 交集
    *
    * @param key       key
    * @param otherKeys otherKeys
    * @param destKey   destKey
    *
    * @return Long
    */
    public Long zIntersectAndStore(String key, Collection<String> otherKeys, String destKey) {
       return redisTemplate.opsForZSet().intersectAndStore(key, otherKeys, destKey);
    }

    /**
    * @param key     key
    * @param options options
    *
    * @return Cursor
    */
    public Cursor<ZSetOperations.TypedTuple<String>> zScan(String key, ScanOptions options) {
       return redisTemplate.opsForZSet().scan(key, options);
    }

    /**
     * setNx
     *
     * @param key     key
     * @param value   value
     * @param timeout timeout
     *
     * @return boolean
     */
    public boolean setNx(String key, String value, long timeout) {
        boolean b = redisTemplate.opsForValue().setIfAbsent(key, value);
        if (b) {
            redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        }
        return b;
    }

    /**
     * runLua 执行lua脚本
     *
     * @param fileClasspath lua脚本Classpath 与 application.properties
     * @param returnType lua脚本返回类型
     * @param keys lua脚本需要操作的 redis key 合集
     * @param values lua脚本需要操作的value
     * @param <T> 泛型
     * @return java.lang.Object Object
     **/
    public <T> Object runLua(String fileClasspath, Class returnType, List keys, Object ... values) {
        DefaultRedisScript<T> redisScript =new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(fileClasspath)));
        redisScript.setResultType(returnType);
        return redisTemplate.execute(redisScript,keys,values);
    }

    /**
     * 批量写入redis
     *
     * @param keys   键集合
     * @param values 值集合
     * @param timeout 有效时间
     *
     * @return Boolean 是否成功
     */
    public Boolean batchAdd(List keys, List values, Long timeout) {
        if (CollectionUtils.isEmpty(keys)) {
            return false;
        }
        values.add(0,timeout);
        return (Boolean) runLua("/lua/batchAdd.lua", Boolean.class, keys, values.toArray());
    }

    /**
     * 批量查询redis
     * @param keys keys
     *
     * @return List 集合
     */
    public List batchGet(List keys) {
        if(CollectionUtils.isEmpty(keys)){
            return Collections.EMPTY_LIST;
        }
        Integer count = 0;
        List list = (List) runLua("/lua/batchGet.lua", List.class, keys);
        for (Object o : list) {
            if (Objects.isNull(o)) {
                count ++;
            }
        }
        if (count.equals(list.size())) {
            return null;
        }
        return list;
    }

    /**
     * 批量写入redis
     *
     * @param keys   键集合
     *
     * @return 是否成功
     */
    public Boolean batchDelete(List keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return false;
        }
        return (Boolean) runLua("/lua/batchDelete.lua", Boolean.class, keys);
    }

}
