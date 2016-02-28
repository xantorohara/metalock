package io.github.xantorohara.metalock;

import java.lang.annotation.*;

/**
 * Annotation indicates that the execution of the target method
 * should be synchronized by the given name and parameter name.
 * <p/>
 * Name and param form an unique key to lock.
 * Param type can be: number, string or any object which .toString() method returns string that represents
 * some unique characteristic of the object.
 * <p/>
 * This annotation can be repeatable.
 *
 * @author Xantorohara
 */
@Repeatable(MetaLocks.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface MetaLock {
    /**
     * Name of the lock.
     * I.e.: "User"
     */
    String name();

    /**
     * Name of the method parameter (or multiple parameters).
     * I.e.: "username"
     * or {"firstName", "lastName"}
     */
    String[] param();
}
