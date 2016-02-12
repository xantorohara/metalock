package io.github.xantorohara.metalock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that the execution of the target method
 * should be synchronized by the given name (or multiple names).
 *
 * @author Xantorohara
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface NameLock {
    /**
     * Name (or names) of the lock
     * I.e.: "USER_TABLE"
     */
    String[] value();
}