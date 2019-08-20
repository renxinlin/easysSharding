package com.baomidou.dynamic.datasource.renxl.hash.annotation;

import com.baomidou.dynamic.datasource.toolkit.DynamicTableContextHolder;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
@Slf4j
public class ShardTableInterceptor implements Interceptor  {


    @Override
    public Object intercept(Invocation invocation) throws Throwable {

//        String realTable =  DynamicTableContextHolder.tableInfo.get();// 什么时候删除 最优先级Aop删除 注意！！！这个很关键不要各种aop没完成线程数据就不一致了
        // realTables为本地线程变量的副本;千万不要传递给其他线程，否则我的分库分表可能发生不知名错误
        List<String> realTables = DynamicTableContextHolder.tablesInfo.get();
        if(!CollectionUtils.isEmpty(realTables)){

            StatementHandler statementHandler = (StatementHandler) PluginUtils.realTarget(invocation.getTarget());
            MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler);
            BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
            String sql = boundSql.getSql();
            String mSql = sql;
            List<String> interceptorsAfter = new ArrayList<>();
            List<String> interceptorsBefore = new ArrayList<>();
            // 自定义hash一致性分库分表位置: 通过hash一致算法路由之后的数据
            mSql = mSql +" ";
            for( String realTable:realTables){
                int i = realTable.lastIndexOf("_");
                String logicTable = realTable.substring(0,i);
                StringBuffer logicTableSb = new StringBuffer(" ").append(logicTable).append(" ");
                StringBuffer realTableSb = new StringBuffer(" ").append(realTable).append(" ");
                mSql = mSql.replaceAll(logicTableSb.toString(), realTableSb.toString());
                if(mSql.contains(realTable)){
                    interceptorsAfter.add(realTable);
                    interceptorsBefore.add(logicTable);
                }
            }
            if(!CollectionUtils.isEmpty(interceptorsBefore)){
                log.info("renxl: ================================拦截开始,表名替换如下============================interceptorsBefore =>{},interceptorsAfter=>{}", Arrays.toString(interceptorsBefore.toArray()),Arrays.toString(interceptorsAfter.toArray()));
            }



            //通过反射修改sql语句
            Field field = boundSql.getClass().getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, mSql);
        }

        // 传递给下一个拦截器处理
        return invocation.proceed();
    }



    @Override
    public Object plugin(Object target) {
        // 当目标类是StatementHandler类型时，才包装目标类，否者直接返回目标本身,减少目标被代理的次数
//        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
//        } else {
//            return target;
//        }
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
