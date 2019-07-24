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

import java.lang.reflect.Field;
import java.util.Properties;

import static org.apache.ibatis.reflection.SystemMetaObject.DEFAULT_OBJECT_FACTORY;
import static org.apache.ibatis.reflection.SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY;

/**
 * 该拦截器的作用用于将逻辑表名替换成真实表名
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
})
@Component
public class ShardTableInterceptor implements Interceptor  {


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaStatementHandler = MetaObject.forObject(statementHandler, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY,new DefaultReflectorFactory());
        Object parameterObject = metaStatementHandler.getValue("delegate.boundSql");

        if(true){
            //先拦截到RoutingStatementHandler，里面有个StatementHandler类型的delegate变量，其实现类是BaseStatementHandler，然后就到BaseStatementHandler的成员变量mappedStatement
            MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");
            //id为执行的mapper方法的全路径名，如com.uv.dao.UserMapper.insertUser
            String id = mappedStatement.getId();
            //sql语句类型 select、delete、insert、update
            String sqlCommandType = mappedStatement.getSqlCommandType().toString();
            BoundSql boundSql = statementHandler.getBoundSql();
            //获取到原始sql语句
            String sql = boundSql.getSql();
            String mSql = sql;
            //TODO 自定义hash一致性分库分表位置: 通过hash一致算法路由之后的数据
            String table =  DynamicTableContextHolder.tableInfo.get();// 什么时候删除？？？？ 需要注解被执行完毕删除
            String logicTable = table.split("_")[0];
            mSql.replaceAll(logicTable,table);


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
