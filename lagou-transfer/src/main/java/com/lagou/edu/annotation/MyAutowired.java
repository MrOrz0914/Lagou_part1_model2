package com.lagou.edu.annotation;

import java.lang.annotation.*;

/**
 * @author Orz
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAutowired {
    String value() default "";
}
