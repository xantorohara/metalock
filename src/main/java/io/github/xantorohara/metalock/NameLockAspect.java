package io.github.xantorohara.metalock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wrap methods annotated as @NameLock and make them synchronised by name (or names)
 * It sorts names before locks and reversed unlocks to prevent deadlocks
 *
 * @author Xantorohara
 */
@Aspect
public class NameLockAspect {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Serial number generator to log Before, After or Error states
     * of the target method invocation
     */
    private final AtomicInteger serial = new AtomicInteger(1000000);

    /**
     * Locks storage.
     * This storage collects all locked names and never removes them.
     * This is ok, because all these names come from the static annotation parameters.
     */
    private final ConcurrentMap<String, ReentrantLock> namedLocks = new ConcurrentHashMap<>();

    /**
     * Create named lock around method with @NamedLock annotation
     *
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("@annotation(io.github.xantorohara.metalock.NameLock)")
    public Object lockAround(ProceedingJoinPoint pjp) throws Throwable {

        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        String methodName = methodSignature.toShortString();

        NameLock nameLockAnnotation = method.getAnnotation(NameLock.class);
        String[] lockNames = nameLockAnnotation.value();

        Arrays.sort(lockNames);

        int unique = serial.incrementAndGet();

        lock(lockNames, unique);

        try {
            log.debug("{} Before {}", unique, methodName);
            Object result = pjp.proceed();
            log.debug("{} After {}", unique, methodName);
            return result;
        } catch (Throwable e) {
            log.debug("{} Error {}", unique, methodName);
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
    private void lock(String[] sortedLockNames, int unique) {
        for (String lockName : sortedLockNames) {
            log.debug("{} Locking {}", unique, lockName);
            namedLocks.computeIfAbsent(lockName, s -> new ReentrantLock()).lock();
            log.debug("{} Locked {}", unique, lockName);
        }
    }

    /**
     * Release sorted named locks in reverse order
     *
     * @param sortedLockNames - array of names
     * @param unique          - unique invocation number to log
     */
    private void unlock(String[] sortedLockNames, int unique) {
        for (int i = sortedLockNames.length - 1; i >= 0; i--) {
            String lockName = sortedLockNames[i];
            log.debug("{} Unlocking {}", unique, lockName);
            namedLocks.get(lockName).unlock();
            log.debug("{} Unlocked {}", unique, lockName);
        }
    }

}