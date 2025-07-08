package org.battery.quality.rule.annotation;

import org.battery.quality.rule.RuleCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 规则定义注解
 * 用于标注规则类，提供规则的元数据
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RuleDefinition {
    /**
     * 规则类型
     */
    String type();
    
    /**
     * 规则编码
     */
    int code();
    
    /**
     * 规则描述
     */
    String description() default "";
    
    /**
     * 规则分类
     */
    RuleCategory category() default RuleCategory.VALIDITY;
    
    /**
     * 规则优先级，值越大优先级越高
     */
    int priority() default 0;
    
    /**
     * 是否启用
     */
    boolean enabled() default true;
} 