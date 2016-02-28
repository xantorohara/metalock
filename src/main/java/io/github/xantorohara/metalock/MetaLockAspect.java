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
 * Wrap methods annotated as @MetaLock.
 * Make them synchronised by name from the annotation and by values from method parameters.
 *
 * @author Xantorohara
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MetaLockAspect {
    private static final Logger LOG = LoggerFactory.getLogger(MetaLockAspect.class);

    private static final char SEPARATOR = '§';

    private static final String DEBUG_FORMAT = "ML{}U {}";
    private static final String TRACE_FORMAT = "ML{}U {} {}";

    /**
     * Serial number generator to log Before, After or Error states.
     */
    private final AtomicLong serial = new AtomicLong(1000000);

    /**
     * Locks storage.
     */
    private final ConcurrentMap<String, ReservedLock> namedLocks = new ConcurrentHashMap<>();

    private final ReentrantLock synchronizer = new ReentrantLock();

    @Around("@annotation(io.github.xantorohara.metalock.MetaLock)||"
            + "@annotation(io.github.xantorohara.metalock.MetaLocks)")
    public final Object lockAround(final ProceedingJoinPoint pjp) throws Throwable {
        long unique = serial.incrementAndGet();

        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        String methodName = methodSignature.toShortString();
        String[] parameterNames = methodSignature.getParameterNames();

        LOG.debug(DEBUG_FORMAT, unique, methodName);

        MetaLock[] metaLocks;
        if (method.isAnnotationPresent(MetaLocks.class)) {
            MetaLocks metaLocksAnnotation = method.getAnnotation(MetaLocks.class);
            metaLocks = metaLocksAnnotation.value();
        } else {
            metaLocks = new MetaLock[]{method.getAnnotation(MetaLock.class)};
        }

        Object[] args = pjp.getArgs();

        String[] lockNames = new String[metaLocks.length];

        for (int i = 0; i < metaLocks.length; i++) {
            MetaLock metaLock = metaLocks[i];
            lockNames[i] = getLockName(metaLock.name(), metaLock.param(), parameterNames, args);
        }

        Arrays.sort(lockNames);
        lock(lockNames, unique);

        try {
            LOG.debug(DEBUG_FORMAT, unique, "Before");
            Object result = pjp.proceed();
            LOG.debug(DEBUG_FORMAT, unique, "After");
            return result;
        } catch (Throwable e) {
            LOG.debug(DEBUG_FORMAT, unique, "Error");
            throw e;
        } finally {
            unlock(lockNames, unique);
        }
    }

    static String getLockName(final String metaLockName, final String[] metalockParam,
                              final String[] methodParameterNames, final Object[] methodArgs) {
        StringBuilder lockName = new StringBuilder(metaLockName);
        for (int j = 0; j < methodParameterNames.length; j++) {
            for (String metalockParamName : metalockParam) {
                if (methodParameterNames[j].equals(metalockParamName)) {
                    lockName.append(SEPARATOR);
                    if (methodArgs[j] == null) {
                        lockName.append("null");
                    } else {
                        lockName.append(methodArgs[j].toString());
                    }
                    break;
                }
            }
        }
        return lockName.toString();
    }

    /**
     * Create or obtain named locks.
     */
    private void lock(final String[] sortedLockNames, final long unique) {
        for (String lockName : sortedLockNames) {
            LOG.trace(TRACE_FORMAT, unique, "Locking", lockName);
            ReservedLock lock;

            synchronizer.lock();
            try {
                lock = namedLocks.computeIfAbsent(lockName, s -> new ReservedLock());
                lock.reserve();
            } finally {
                synchronizer.unlock();
            }

            lock.lock();
            LOG.trace(TRACE_FORMAT, unique, "Locked", lockName);
        }
    }

    /**
     * Release sorted named locks in reverse order.
     */
    private void unlock(final String[] sortedLockNames, final long unique) {
        for (int i = sortedLockNames.length - 1; i >= 0; i--) {
            String lockName = sortedLockNames[i];

            LOG.trace(TRACE_FORMAT, unique, "Unlocking", lockName);

            ReservedLock lock;

            synchronizer.lock();
            try {
                lock = namedLocks.get(lockName);
                lock.release();
                if (lock.isFree()) {
                    namedLocks.remove(lockName);
                    LOG.trace(TRACE_FORMAT, unique, "Removed", lockName);
                }
            } finally {
                synchronizer.unlock();
            }

            lock.unlock();
            LOG.trace(TRACE_FORMAT, unique, "Unlocked", lockName);
        }
    }

    /**
     * Extension of the ReentrantLock with ability to "reserve"
     * lock before the real locking.
     * <p/>
     * Actually this class itself is not thread-safe, but its methods
     * reserve(), release() and isFree() are always called from the thread-safe environment.
     */
    private static final class ReservedLock extends ReentrantLock {
        private int count = 0;

        void reserve() {
            count++;
        }

        void release() {
            count--;
        }

        boolean isFree() {
            return count == 0;
        }

        ReservedLock() {
            super(true);
        }
    }
}
