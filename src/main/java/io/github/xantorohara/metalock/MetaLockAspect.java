package io.github.xantorohara.metalock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wrap methods annotated as @MetaLock and make them synchronised by name from annotation
 * and value from method parameter.
 *
 * @author Xantorohara
 */
@Aspect
public class MetaLockAspect {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Serial number generator to log Before, After or Error states
     * of the target method invocation
     */
    private final AtomicInteger serial = new AtomicInteger(1000000);

    /**
     * Locks storage.
     */
    private final ConcurrentMap<String, ReservedLock> namedLocks = new ConcurrentHashMap<>();

    private final ReentrantLock synchronizer = new ReentrantLock();

    @Around("@annotation(io.github.xantorohara.metalock.MetaLock)||" +
            "@annotation(io.github.xantorohara.metalock.MetaLocks)")
    public Object lockAround(ProceedingJoinPoint pjp) throws Throwable {

        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        String methodName = methodSignature.toShortString();
        String[] parameterNames = methodSignature.getParameterNames();

        MetaLock[] metaLocks;
        if (method.isAnnotationPresent(MetaLocks.class)) {
            MetaLocks namedLockAnnotation = method.getAnnotation(MetaLocks.class);
            metaLocks = namedLockAnnotation.value();
        } else {
            metaLocks = new MetaLock[]{method.getAnnotation(MetaLock.class)};
        }

        Object[] args = pjp.getArgs();

        List<String> lockNames = new ArrayList<>(metaLocks.length);

        for (MetaLock metaLock : metaLocks) {
            String param = null;
            for (int j = 0; j < parameterNames.length; j++) {
                if (parameterNames[j].equals(metaLock.param())) {
                    if (args[j] != null) {
                        param = args[j].toString();
                    }
                    break;
                }
            }
            if (param != null) {
                lockNames.add(metaLock.name() + ((char) 0) + param);
            }
        }

        int unique = serial.incrementAndGet();

        if (!lockNames.isEmpty()) {
            if (lockNames.size() > 1) {
                Collections.sort(lockNames);
            }
            lock(lockNames, unique);
        }

        try {
            log.debug("{} Before {}", unique, methodName);
            Object result = pjp.proceed();
            log.debug("{} After {}", unique, methodName);
            return result;
        } catch (Throwable e) {
            log.debug("{} Error {}", unique, methodName);
            throw e;
        } finally {
            if (!lockNames.isEmpty()) {
                unlock(lockNames, unique);
            }
        }
    }

    /**
     * Create or obtain named locks
     */
    private void lock(List<String> sortedLockNames, int unique) {
        for (String lockName : sortedLockNames) {
            log.debug("{} Locking {}", unique, lockName);
            ReservedLock lock;

            synchronizer.lock();
            try {
                lock = namedLocks.computeIfAbsent(lockName, s -> new ReservedLock());
                lock.reserve();
            } finally {
                synchronizer.unlock();
            }

            lock.lock();
            log.debug("{} Locked {}", unique, lockName);
        }
    }

    /**
     * Release sorted named locks in reverse order
     */
    private void unlock(List<String> sortedLockNames, int unique) {
        ListIterator<String> iter = sortedLockNames.listIterator(sortedLockNames.size());

        while (iter.hasPrevious()) {
            String lockName = iter.previous();
            log.debug("{} Unlocking {}", unique, lockName);

            ReservedLock lock;

            synchronizer.lock();
            try {
                lock = namedLocks.get(lockName);
                lock.release();
                if (lock.isFree()) {
                    namedLocks.remove(lockName);
                    log.debug("{} Removed {}", unique, lockName);
                }
            } finally {
                synchronizer.unlock();
            }

            lock.unlock();
            log.debug("{} Unlocked {}", unique, lockName);
        }
    }

    /**
     * Extension of the ReentrantLock with ability to "reserve"
     * lock before the real locking.
     * <p>
     * Actually this class itself is not thread-safe, but its methods
     * reserve(), release() and isFree() are always called from the thread-safe environment.
     */
    private static class ReservedLock extends ReentrantLock {
        private volatile int count = 0;

        void reserve() {
            count++;
        }

        void release() {
            count--;
        }

        boolean isFree() {
            return count == 0;
        }
    }
}