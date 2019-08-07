/**
 * Copyright © 2018 organization baomidou
 * <pre>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <pre/>
 */
package com.baomidou.dynamic.datasource.aop;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.dynamic.datasource.DynamicDataSourceClassResolver;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.processor.DsProcessor;
import com.baomidou.dynamic.datasource.renxl.hash.annotation.*;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.dynamic.datasource.toolkit.DynamicTableContextHolder;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * renxl: 核心切面 very import
 * 动态数据源AOP核心拦截器[renxl: 此名称源于spring源码解说时候就是拦截器]
 *
 * @author TaoYu
 * @since 1.2.0
 */
public class DynamicDataSourceAnnotationInterceptor implements MethodInterceptor {

    /**
     * SPEL参数标识
     */
    private static final String DYNAMIC_PREFIX = "#";

    @Setter
    private DsProcessor dsProcessor;

    private DynamicDataSourceClassResolver dynamicDataSourceClassResolver = new DynamicDataSourceClassResolver();

    private Logger logger = LoggerFactory.getLogger(DynamicDataSourceAnnotationInterceptor.class);
    /**
     * 目前内部使用;不注入
     * 不由外部代码拓展
     */
    private IHash hashfun = new DefaulltHash();

    @Setter
    private DbHashCircle dbHashCircle;

    @Setter
    private TableHashCircleSet tableHashCircleSet;


    // 通过线程传递到mybatis层

    /**
     * 业务方法的执行
     * @param invocation
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            Object[] arguments = invocation.getArguments();
            // 怎么获取上下文路由字段值
            String hashId = null;
            String[] logicTables = null;
            for(Object argument : arguments){
                Field[] declaredFields = argument.getClass().getDeclaredFields();
                for (Field declaredField: declaredFields){
                    boolean annotationPresent = declaredField.isAnnotationPresent(HashId.class);
                    if(annotationPresent){
                        declaredField.setAccessible(true);
                        try {
                            try {
                                hashId = String.valueOf(declaredField.get(argument));
                                HashId declaredAnnotation = declaredField.getDeclaredAnnotation(HashId.class);
                                HashForDbAndTable hashForDbAndTable = invocation.getMethod().getDeclaredAnnotation(HashForDbAndTable.class);
                                // 动态替换的表名
                                logicTables = hashForDbAndTable.logicTable();
                                // 如果注解中没有逻辑表名，则不会处理dao层的表名替换
                                if(logicTables != null && logicTables.length>0){
                                    ArrayList<String> realTables = new ArrayList<>();
                                    for(String logicTable:logicTables){

                                        String realTable = determineTableSharding(hashId, logicTable);
                                        realTables.add(realTable);
                                    }
                                    // 传递给mybatis的拦截器进行执行
                                    // 设置本地线程变量的副本
                                    AtomicInteger atomicInteger = DynamicTableContextHolder.aopNum.get();
                                    if(atomicInteger == null){
                                        AtomicInteger aopNum = new AtomicInteger();
                                        aopNum.incrementAndGet();
                                        DynamicTableContextHolder.aopNum.set(aopNum);
                                    }else{
                                        atomicInteger.incrementAndGet();
                                        DynamicTableContextHolder.aopNum.set(atomicInteger);

                                    }
                                    if (CollectionUtils.isEmpty(DynamicTableContextHolder.tablesInfo.get())){
                                        DynamicTableContextHolder.tablesInfo.set(realTables);
                                    }else {
                                        final List<String> oldRealTables = DynamicTableContextHolder.tablesInfo.get();
                                        realTables.removeIf(realTable-> oldRealTables.contains(realTable));
                                        oldRealTables.addAll(realTables);
                                    }
                                }
                            } catch (IllegalArgumentException e) {
                               throw new RenxlProcrssorException();
                            } catch (IllegalAccessException e) {
                                throw new RenxlProcrssorException();

                            }
                        } catch (RenxlProcrssorException e) {

                        }

                    }

                }

            }
            DynamicDataSourceContextHolder.push(determineDatasource(invocation,hashId));
            return invocation.proceed();
        } finally {
            DynamicDataSourceContextHolder.poll();
            // 这个优先级最牛逼 所以这一步mybatis已经执行结束！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！


            AtomicInteger atomicInteger = DynamicTableContextHolder.aopNum.get();
            if(atomicInteger != null ){
                int andDecrement = atomicInteger.decrementAndGet();
                if (andDecrement == 0){
                    List<String> realTablesList = DynamicTableContextHolder.tablesInfo.get();
                    if (!CollectionUtils.isEmpty(realTablesList)){
                        Object[] realTables = realTablesList .toArray();
                        logger.info("分表信息执行完毕，清空线程中的分表信息{}", Arrays.toString(realTables) );
                        // 防止内存泄漏
                        DynamicTableContextHolder.tablesInfo.remove();// 先计算hash对应数组位置后遍历移除
                    }
                    DynamicTableContextHolder.aopNum.remove();
                }
            }
        }

    }
//
//    public static void main(String[] args) {
//        ArrayList<String> oldRealTables = new ArrayList<>();
//        ArrayList<String> realTables = new ArrayList<>();
//        oldRealTables.add("1");
//        oldRealTables.add("2");
//        oldRealTables.add("3");
//
//        realTables.add("4");
//        realTables.add("2");
//        realTables.add("5");
//        realTables.removeIf(realTable-> oldRealTables.contains(realTable));
//        oldRealTables.addAll(realTables);
//        oldRealTables.forEach(System.out::println);
//    }

    /**
     * 这一步是关键
     * 获取注解动态解析数据源
     * 现在我要根据一致性hash通过组织名hash决定分库分表规则
     * @param invocation
     * @return
     * @throws Throwable
     */
    private String determineDatasource(MethodInvocation invocation,String hashId  ) throws Throwable {
         //  renxl 只支持方法级别
        // 在这里将我的注解解析成ds即可
        //
//        这个realName 需要通过mybatis动态替换掉查询语句中的值

        if(hashId!=null){
            String dbName = determineDatasourceByRenxl(invocation, hashId);
            return dbName;
        }


        //没有renxl定义的注解则解析苞米豆的注解
        Method method = invocation.getMethod();
        Class<?> declaringClass = dynamicDataSourceClassResolver.targetClass(invocation);
        DS ds = method.isAnnotationPresent(DS.class) ? method.getAnnotation(DS.class)
                : AnnotationUtils.findAnnotation(declaringClass, DS.class);
        String key = ds.value();
        return (!key.isEmpty() && key.startsWith(DYNAMIC_PREFIX)) ? dsProcessor.determineDatasource(invocation, key) : key;
    }

    /**
     *
     * @param hashId 通过hashId注解需要指定的路由字段
     * @param logicTable 配置的逻辑表
     * @return 最终需要路由的真实表【逻辑表+尾缀】
     */
    private String determineTableSharding(String hashId,String logicTable) {
        /**
         * tablename1_1 tablename1
         * ablename1_2 tablename1
         * ablename1_3  tablename1
         * ablename1_4  tablename1
         * ablename2_1  tablename2
         * ablename2_2  tablename2
         * ablename2_3  tablename2
         *
         * twoTableName1_1 twoTableName1
         * twoTableName2_1 twoTableName2
         */
        String realNode = renxlHash(hashId);
        Map<String, TableHashCircle> logicTableAndHashCircle = tableHashCircleSet.getLogicTableAndHashCircle();
        TableHashCircle tableHashCircle = logicTableAndHashCircle.get(logicTable);
        String realTable = tableHashCircle.get(hashId);
        return realTable;
    }

    private String determineDatasourceByRenxl(MethodInvocation invocation, String hashId) {
        // realNode就是master_1 ;slave_1这些
        return renxlHash(hashId);
    }

    /**
     * 根据hash结果路由表和数据源
     * @param hashId
     * @return
     */
    private String renxlHash(String hashId) {
        long hash = hashfun.hash(hashId);
        String realNode = dbHashCircle.get(String.valueOf(hash));
        return  realNode;
    }
//
//
//    private String renxlHashTable(String hashId,String logicTable) {
//        long hash = hashfun.hash(hashId);
//        String realNode = tableHashCircleSet.getLogicTableAndHashCircle().get(logicTable).get(String.valueOf(hash));
//        return  realNode;
//    }


}