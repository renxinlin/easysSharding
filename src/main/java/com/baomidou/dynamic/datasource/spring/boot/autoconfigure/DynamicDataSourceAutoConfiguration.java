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
package com.baomidou.dynamic.datasource.spring.boot.autoconfigure;

import com.alibaba.druid.support.json.JSONParser;
import com.baomidou.dynamic.datasource.DynamicDataSourceConfigure;
import com.baomidou.dynamic.datasource.DynamicDataSourceCreator;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationInterceptor;
import com.baomidou.dynamic.datasource.processor.DsHeaderProcessor;
import com.baomidou.dynamic.datasource.processor.DsProcessor;
import com.baomidou.dynamic.datasource.processor.DsSessionProcessor;
import com.baomidou.dynamic.datasource.processor.DsSpelExpressionProcessor;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.provider.YmlDynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.renxl.hash.annotation.DbHashCircle;
import com.baomidou.dynamic.datasource.renxl.hash.annotation.ShardTableInterceptor;
import com.baomidou.dynamic.datasource.renxl.hash.annotation.TableHashCircle;
import com.baomidou.dynamic.datasource.renxl.hash.annotation.TableHashCircleSet;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidDynamicDataSourceConfiguration;
import com.baomidou.dynamic.datasource.strategy.DynamicDataSourceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 动态数据源核心自动配置类
 *
 * @author TaoYu Kanyuxia
 * @see DynamicDataSourceProvider
 * @see DynamicDataSourceStrategy
 * @see DynamicRoutingDataSource
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@Import(DruidDynamicDataSourceConfiguration.class)
public class DynamicDataSourceAutoConfiguration {

    @Autowired
    private DynamicDataSourceProperties properties;


    // hash 规则部分开始======================================================start=============================================

    @Bean
    public ShardTableInterceptor getInterceptor(){
        return new ShardTableInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public DbHashCircle dbHashCircle( ) {
        DbHashCircle hc = new DbHashCircle();
        Set<String> strings = properties.getDatasource().keySet();
        for(String dbName:strings){
            hc.add(dbName);
        }
        return hc;
    }


    @Bean
    @ConditionalOnMissingBean
    public TableHashCircleSet tableHashCircleSet( ) {
        TableHashCircleSet hcSet = new TableHashCircleSet();
        // tablename tanlename1 逻辑表名 以及真实表名集合
        Map<String, List<String>> tables = properties.getTables();

        for(String logicTable:tables.keySet()){
            TableHashCircle tableHashCircle = new TableHashCircle();
            List<String> realTables = tables.get(logicTable);
            for(String realTable:realTables){
                tableHashCircle.add(realTable,10);
            }
            hcSet.getLogicTableAndHashCircle().put(logicTable,tableHashCircle);
        }
        return hcSet;
    }


    // hash 规则部分开始======================================================end===============================================
    @Bean
//    @ConditionalOnMissingBean
    public DataSource dataSource(DynamicDataSourceProvider dynamicDataSourceProvider) {
        log.info("动态数据源加载开始");
        DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource();
        dataSource.setPrimary(properties.getPrimary());
        dataSource.setStrategy(properties.getStrategy());
        dataSource.setProvider(dynamicDataSourceProvider);
        dataSource.setP6spy(properties.getP6spy());
        dataSource.setStrict(properties.getStrict());
        return dataSource;
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceProvider dynamicDataSourceProvider(DynamicDataSourceCreator dynamicDataSourceCreator) {
        return new YmlDynamicDataSourceProvider(properties, dynamicDataSourceCreator);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceCreator dynamicDataSourceCreator(@Autowired(required = false) WebApplicationContext webApplicationContext) {
        DynamicDataSourceCreator dynamicDataSourceCreator = new DynamicDataSourceCreator();
        dynamicDataSourceCreator.setApplicationContext(webApplicationContext);
        dynamicDataSourceCreator.setDruidGlobalConfig(properties.getDruid());
        dynamicDataSourceCreator.setHikariGlobalConfig(properties.getHikari());
        return dynamicDataSourceCreator;
    }



    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceAnnotationAdvisor dynamicDatasourceAnnotationAdvisor(DsProcessor dsProcessor, DbHashCircle dbHashCircle, TableHashCircleSet tableHashCircleSet) {
        DynamicDataSourceAnnotationInterceptor interceptor = new DynamicDataSourceAnnotationInterceptor();
        interceptor.setDsProcessor(dsProcessor);
        interceptor.setDbHashCircle(dbHashCircle);
        interceptor.setTableHashCircleSet(tableHashCircleSet);
        log.info("动态数据库hash一致环数据{}",dbHashCircle);
        log.info("动态数据表hash一致环数据{}",tableHashCircleSet);
        DynamicDataSourceAnnotationAdvisor advisor = new DynamicDataSourceAnnotationAdvisor(interceptor);
        advisor.setOrder(properties.getOrder());
        return advisor;
    }

    @Bean
    @ConditionalOnMissingBean
    public DsProcessor dsProcessor() {
        DsHeaderProcessor headerProcessor = new DsHeaderProcessor();
        DsSessionProcessor sessionProcessor = new DsSessionProcessor();
        DsSpelExpressionProcessor spelExpressionProcessor = new DsSpelExpressionProcessor();
        headerProcessor.setNextProcessor(sessionProcessor);
        sessionProcessor.setNextProcessor(spelExpressionProcessor);
        return headerProcessor;
    }

    @Bean
    @ConditionalOnBean(DynamicDataSourceConfigure.class)
    public DynamicDataSourceAdvisor dynamicAdvisor(DynamicDataSourceConfigure dynamicDataSourceConfigure, DsProcessor dsProcessor) {
        DynamicDataSourceAdvisor advisor = new DynamicDataSourceAdvisor(dynamicDataSourceConfigure.getMatchers());
        advisor.setDsProcessor(dsProcessor);
        return advisor;
    }
}
