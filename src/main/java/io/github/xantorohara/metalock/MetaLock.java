package io.github.xantorohara.metalock;

import java.lang.annotation.*;

/**
 * Annotation indicating that the execution of the target method
 * should be synchronized by the given name and parameter name.
 * <p>
 * Name and param form an unique key to lock.
 * This annotation can be repeatable.
 *
 * @author Xantorohara
 */
@Repeatable(MetaLocks.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface MetaLock {
    /**
     * Name of the lock
     * I.e.: "User"
     */
    String name();

    /**
     * Name of the method parameter
     * I.e.: "username"
     */
    String param();
}