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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wrap methods annotated as @MetaLock and make them synchronised by name from annotation
 * and value from method parameter.
 *
 * @author Xantorohara
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MetaLockAspect {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final static String DEBUG_FORMAT = "ML{}U {}";
    private final static String TRACE_FORMAT = "ML{}U {} {}";


    /**
     * Serial number generator to log Before, After or Error states
     * of the target method invocation
     */
    private final AtomicLong serial = new AtomicLong(1000000);

    /**
     * Locks storage.
     */
    private final ConcurrentMap<String, ReservedLock> namedLocks = new ConcurrentHashMap<>();

    private final ReentrantLock synchronizer = new ReentrantLock();

    @Around("@annotation(io.github.xantorohara.metalock.MetaLock)||" +
            "@annotation(io.github.xantorohara.metalock.MetaLocks)")
    public Object lockAround(ProceedingJoinPoint pjp) throws Throwable {
        long unique = serial.incrementAndGet();

        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        String methodName = methodSignature.toShortString();
        String[] parameterNames = methodSignature.getParameterNames();

        log.debug(DEBUG_FORMAT, unique, methodName);

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

        if (!lockNames.isEmpty()) {
            if (lockNames.size() > 1) {
                Collections.sort(lockNames);
            }
            lock(lockNames, unique);
        }

        try {
            log.debug(DEBUG_FORMAT, unique, "Before");
            Object result = pjp.proceed();
            log.debug(DEBUG_FORMAT, unique, "After");
            return result;
        } catch (Throwable e) {
            log.debug(DEBUG_FORMAT, unique, "Error");
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
    private void lock(List<String> sortedLockNames, long unique) {
        for (String lockName : sortedLockNames) {
            log.trace(TRACE_FORMAT, unique, "Locking", lockName);
            ReservedLock lock;

            synchronizer.lock();
            try {
                lock = namedLocks.computeIfAbsent(lockName, s -> new ReservedLock());
                lock.reserve();
            } finally {
                synchronizer.unlock();
            }

            lock.lock();
            log.trace(TRACE_FORMAT, unique, "Locked", lockName);
        }
    }

    /**
     * Release sorted named locks in reverse order
     */
    private void unlock(List<String> sortedLockNames, long unique) {
        ListIterator<String> iterator = sortedLockNames.listIterator(sortedLockNames.size());

        while (iterator.hasPrevious()) {
            String lockName = iterator.previous();
            log.trace(TRACE_FORMAT, unique, "Unlocking", lockName);

            ReservedLock lock;

            synchronizer.lock();
            try {
                lock = namedLocks.get(lockName);
                lock.release();
                if (lock.isFree()) {
                    namedLocks.remove(lockName);
                    log.trace(TRACE_FORMAT, unique, "Removed", lockName);
                }
            } finally {
                synchronizer.unlock();
            }

            lock.unlock();
            log.trace(TRACE_FORMAT, unique, "Unlocked", lockName);
        }
    }

    /**
     * Extension of the ReentrantLock with ability to "reserve"
     * lock before the real locking.
     * <p/>
     * Actually this class itself is not thread-safe, but its methods
     * reserve(), release() and isFree() are always called from the thread-safe environment.
     */
    private final static class ReservedLock extends ReentrantLock {
        private AtomicInteger count = new AtomicInteger();

        void reserve() {
            count.incrementAndGet();
        }

        void release() {
            count.decrementAndGet();
        }

        boolean isFree() {
            return count.get() == 0;
        }

        ReservedLock() {
            super(true);
        }
    }
}