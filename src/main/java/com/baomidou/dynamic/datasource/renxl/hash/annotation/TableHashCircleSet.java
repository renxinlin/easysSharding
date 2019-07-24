package com.baomidou.dynamic.datasource.renxl.hash.annotation;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 外部配置初始化时候添加节点
 */
@Data
public class TableHashCircleSet {
    private Map<String,TableHashCircle> logicTableAndHashCircle = new HashMap<>();
    private IHash hashfun = new DefaulltHash();

    public String get(String hashId,String logicTable) {
        if (logicTableAndHashCircle.isEmpty()) {
            return null;
        }
        long hash = hashfun.hash((String) hashId);
        TableHashCircle circleObj = logicTableAndHashCircle.get(logicTable);
        if (!circleObj.getCircle().containsKey(hash)) {
            //返回此映射的部分视图，其键大于等于 hash
            SortedMap<Long, String> tailMap = circleObj.getCircle().tailMap(hash);
            hash = tailMap.isEmpty() ? circleObj.getCircle().firstKey() : tailMap.firstKey();
        }
        return circleObj.getCircle().get(hash);
    }
}
