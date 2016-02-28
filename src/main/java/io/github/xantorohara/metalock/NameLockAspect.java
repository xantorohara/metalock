package io.github.xantorohara.metalock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wrap methods annotated as @NameLock and make them synchronised by name (or names)
 * It sorts names before locks and reversed unlocks to prevent deadlocks
 *
 * @author Xantorohara
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NameLockAspect {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final static String DEBUG_FORMAT = "NL{}U {}";
    private final static String TRACE_FORMAT = "NL{}U {} {}";

    /**
     * Serial number generator to log Before, After or Error states
     * of the target method invocation
     */
    private final AtomicLong serial = new AtomicLong(1000000);

    /**
     * Locks storage.
     * This storage collects all locked names and never removes them.
     * This is ok, because all these names come from the static annotation parameters.
     */
    private final ConcurrentMap<String, ReentrantLock> namedLocks = new ConcurrentHashMap<>();

    @Around("@annotation(io.github.xantorohara.metalock.NameLock)")
    public Object lockAround(ProceedingJoinPoint pjp) throws Throwable {
        long unique = serial.incrementAndGet();

        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        String methodName = methodSignature.toShortString();

        log.debug(DEBUG_FORMAT, unique, methodName);

        NameLock nameLockAnnotation = method.getAnnotation(NameLock.class);
        String[] lockNames = nameLockAnnotation.value();

        Arrays.sort(lockNames);

        lock(lockNames, unique);

        try {
            log.debug(DEBUG_FORMAT, unique, "Before");
            Object result = pjp.proceed();
            log.debug(DEBUG_FORMAT, unique, "After");
            return result;
        } catch (Throwable e) {
            log.debug(DEBUG_FORMAT, unique, "Error");
            throw e;
        } finally {
            unlock(lockNames, unique);
        }
    }

    /**
     * Create or obtain named locks
     *
     * @param sortedLockNames - array of names
     * @param unique          - unique invocation number to log
     */
    private void lock(String[] sortedLockNames, long unique) {
        for (String lockName : sortedLockNames) {
            log.trace(TRACE_FORMAT, unique, "Locking", lockName);
            namedLocks.computeIfAbsent(lockName, s -> new ReentrantLock(true)).lock();
            log.trace(TRACE_FORMAT, unique, "Locked", lockName);
        }
    }

    /**
     * Release sorted named locks in reverse order
     *
     * @param sortedLockNames - array of names
     * @param unique          - unique invocation number to log
     */
    private void unlock(String[] sortedLockNames, long unique) {
        for (int i = sortedLockNames.length - 1; i >= 0; i--) {
            String lockName = sortedLockNames[i];
            log.trace(TRACE_FORMAT, unique, "Unlocking", lockName);
            namedLocks.get(lockName).unlock();
            log.trace(TRACE_FORMAT, unique, "Unlocked", lockName);
        }
    }

}