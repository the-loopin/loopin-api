package com.loopin.api.common.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a bounded business operation for Loopin's Prometheus metrics.
 * Domain and operation values must be static, never derived from request data.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoopinOperation {

    String domain();

    String operation();
}
