package com.baomidou.dynamic.datasource.renxl.hash.annotation;

import com.baomidou.dynamic.datasource.toolkit.DynamicTableContextHolder;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;

import static org.apache.ibatis.reflection.SystemMetaObject.DEFAULT_OBJECT_FACTORY;
import static org.apache.ibatis.reflection.SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY;

/**
 * 该拦截器的作用用于将逻辑表名替换成真实表名
 */
//@Intercepts({
//        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
//        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
//        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
//})

@Intercepts({@Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
)})
public class ShardTableInterceptor implements Interceptor  {


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        System.out.println("================================拦截开始============================");

        String realTable =  DynamicTableContextHolder.tableInfo.get();// 什么时候删除 最优先级Aop删除 注意！！！这个很关键不要各种aop没完成线程数据就不一致了
        if(!StringUtils.isEmpty(realTable)){
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            MetaObject metaStatementHandler = MetaObject.forObject(statementHandler, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY,new DefaultReflectorFactory());
            BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");

            String sql = boundSql.getSql();
            String mSql = sql;
            // 自定义hash一致性分库分表位置: 通过hash一致算法路由之后的数据

            int i = realTable.lastIndexOf("_");
            String logicTable = realTable.substring(0,i);
            String finalSql = mSql.replaceAll(logicTable, realTable);


            //通过反射修改sql语句
            Field field = boundSql.getClass().getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, finalSql);
        }

        // 传递给下一个拦截器处理
        return invocation.proceed();
    }



    @Override
    public Object plugin(Object target) {
        // 当目标类是StatementHandler类型时，才包装目标类，否者直接返回目标本身,减少目标被代理的次数
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    @Override
    public void setProperties(Properties properties) {

    }
    /**
     * Mybatis允许我们能够进行切入的点：
     *
     * Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
     *
     * ParameterHandler (getParameterObject, setParameters)
     *
     * ResultSetHandler (handleResultSets, handleOutputParameters)
     *
     * StatementHandler (prepare, parameterize, batch, update, query)
     */
}
