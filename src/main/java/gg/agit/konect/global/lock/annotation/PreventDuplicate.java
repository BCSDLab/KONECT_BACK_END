package gg.agit.konect.global.lock.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(METHOD)
@Retention(RUNTIME)
public @interface PreventDuplicate {
    String key();
    long leaseTime() default 1;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
