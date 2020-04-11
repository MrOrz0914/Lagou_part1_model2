package com.lagou.edu.annotation;

import java.lang.annotation.*;

/**
 * @author Orz
 */
@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyTransactional {
}
