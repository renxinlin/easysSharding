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

import com.baomidou.dynamic.datasource.DynamicDataSourceClassResolver;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.processor.DsProcessor;
import com.baomidou.dynamic.datasource.renxl.hash.annotation.HashForDbAndTable;
import com.baomidou.dynamic.datasource.renxl.hash.annotation.HashId;
import com.baomidou.dynamic.datasource.renxl.hash.annotation.RenxlProcrssorException;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
            for(Object argument : arguments){
                Field[] declaredFields = argument.getClass().getDeclaredFields();
                for (Field declaredField: declaredFields){
                    boolean annotationPresent = declaredField.isAnnotationPresent(HashId.class);
                    if(annotationPresent){
                        declaredField.setAccessible(true);
                        try {
                            try {
                                hashId = declaredField.get(argument)+ "";
                                HashId declaredAnnotation = declaredField.getDeclaredAnnotation(HashId.class);
                                HashForDbAndTable hashForDbAndTable = invocation.getMethod().getDeclaredAnnotation(HashForDbAndTable.class);
                                // 动态替换表名的关键
                                String table = hashForDbAndTable.table();
                                String tableUnderline = hashForDbAndTable.table_underline();
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
        }
    }

    /**
     * 这一步是关键
     * 获取注解动态解析数据源
     * 现在我要根据一致性hash通过组织名hash决定分库分表规则
     * @param invocation
     * @return
     * @throws Throwable
     */
    private String determineDatasource(MethodInvocation invocation,String hashId) throws Throwable {
         //  renxl 只支持方法级别
        // 在这里将我的注解解析成ds即可
        //
        if(hashId!=null){
            return determineDatasourceByRenxl( invocation, hashId);
        }


        //没有renxl定义的注解则解析苞米豆的注解
        Method method = invocation.getMethod();
        Class<?> declaringClass = dynamicDataSourceClassResolver.targetClass(invocation);
        DS ds = method.isAnnotationPresent(DS.class) ? method.getAnnotation(DS.class)
                : AnnotationUtils.findAnnotation(declaringClass, DS.class);
        String key = ds.value();
        return (!key.isEmpty() && key.startsWith(DYNAMIC_PREFIX)) ? dsProcessor.determineDatasource(invocation, key) : key;
    }

    private String determineDatasourceByRenxl(MethodInvocation invocation, String hashId) {
        //
        String key = renxlHash(hashId);
        return (!key.isEmpty() && key.startsWith(DYNAMIC_PREFIX)) ? dsProcessor.determineDatasource(invocation, key) : key;
    }

    /**
     * 根据hash结果路由表和数据源
     * @param hashId
     * @return
     */
    private String renxlHash(String hashId) {
        return  hashId;
    }
}