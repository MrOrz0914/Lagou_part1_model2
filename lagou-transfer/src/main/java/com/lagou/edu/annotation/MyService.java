package com.lagou.edu.annotation;

import java.lang.annotation.*;

/**
 * @author Orz
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyService {
    String value() default "";
}
