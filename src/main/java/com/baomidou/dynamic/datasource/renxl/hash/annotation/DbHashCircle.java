package com.baomidou.dynamic.datasource.renxl.hash.annotation;

import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationInterceptor;
import lombok.Data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 外部配置初始化时候添加节点
 */
@Data
public class DbHashCircle {

    private IHash hashfun = new DefaulltHash();

    private final SortedMap<Long, String> circle = new TreeMap();



    public String get(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = hashfun.hash((String) key);
        if (!circle.containsKey(hash)) {
            //返回此映射的部分视图，其键大于等于 hash
            SortedMap<Long, String> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    int numberOfReplicas = 100;
    public void add(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hashfun.hash(node.toString() + i), node);
        }
    }

    /**
     * 移除节点
     * @param node
     */
    public void remove(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(hashfun.hash(node.toString() + i));
        }
    }
}
