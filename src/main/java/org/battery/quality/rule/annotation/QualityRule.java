package org.battery.quality.rule.annotation;

import org.battery.quality.model.RuleType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 质量规则注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface QualityRule {
    // 规则类型
    String type();
    
    // 规则异常编码（数字类型）
    int code();
    
    // 规则描述
    String description();
    
    // 规则类别
    RuleType category();
    
    // 规则优先级
    int priority() default 0;
    
    // 是否启用
    boolean enabled() default true;
} 