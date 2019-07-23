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

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.renxl.hash.annotation.HashForDbAndTable;
import lombok.NonNull;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * 动态数据源AOP织入
 *
 * @author TaoYu
 * @since 1.2.0
 */
public class DynamicDataSourceAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

    private Advice advice;

    private Pointcut pointcut;

    public DynamicDataSourceAnnotationAdvisor(@NonNull DynamicDataSourceAnnotationInterceptor dynamicDataSourceAnnotationInterceptor) {
        this.advice = dynamicDataSourceAnnotationInterceptor;
        this.pointcut = buildPointcut();
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (this.advice instanceof BeanFactoryAware) {
            ((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
        }
    }

    // 源码
//    private Pointcut buildPointcut() {
//        Pointcut cpc = new AnnotationMatchingPointcut(DS.class,  true);
//        Pointcut mpc = AnnotationMatchingPointcut.forMethodAnnotation(DS.class);
//        return new ComposablePointcut(cpc).union(mpc);
//    }


    private Pointcut buildPointcut() {
        Pointcut cpc = new AnnotationMatchingPointcut(DS.class,  true);
        Pointcut mpc = AnnotationMatchingPointcut.forMethodAnnotation(DS.class);
        Pointcut mpc1 = AnnotationMatchingPointcut.forMethodAnnotation(HashForDbAndTable.class);
        // TODO 测试支持DS注解或者renxl自定义注解
        return new ComposablePointcut(cpc).union(mpc).union(mpc1);
    }
    /**
     *
     * public ComposablePointcut intersection(ClassFilter other)
     * 将复合切点和一个ClassFilter对象进行交集运算，得到一个结果复合切点
     *
     * public ComposablePointcut intersection(MethodMatcher other)
     * 将复合切点和一个MethodMatcher对象进行交集运算，得到一个结果复合切点
     *
     * public ComposablePointcut intersection(Pointcut other)
     * 将复合切点和一个切点对象进行交集运算，得到一个结果复合切点
     * public ComposablePointcut union(ClassFilter other)
     * 将复合切点和一个ClassFilter对象进行并集运算，得到一个结果复合切点
     *
     * public ComposablePointcut union(MethodMatcher other)
     *
     * 将复合切点和一个MethodMatcher对象进行并集运算，得到一个结果复合切点
     *
     * public ComposablePointcut union(Pointcut other)
     * 将复合切点和一个切点对象进行并集运算，得到一个结果复合切点
     *
     *
     *
     *
     * public static Pointcut union(Pointcut pc1, Pointcut pc2)
     * 对两个切点进行交集运算，返回一个结果切点，该切点即ComposablePointcut对象的实例
     *
     * public static Pointcut intersection(Pointcut pc1, Pointcut pc2)
     * 对两个切点进行并集运算，返回一个结果切点，该切点即ComposablePointcut对象的实例
     */
}
