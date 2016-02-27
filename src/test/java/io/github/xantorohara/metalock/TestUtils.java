package io.github.xantorohara.metalock;

import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TestUtils {

    /**
     * Run multiple tasks concurrently and wait until all are finished.
     * Use small gap times between starts together with continuous tasks
     * in order to model the relevant concurrent cases.
     * <p/>
     * For example, we have two tasks, gap time = 10ms, task time = 20ms.
     * <p/>
     * Without metalocking tasks execution would be like this:
     * <pre>
     * Task1: |run|___20ms_task_time___|end|
     * Task2: |_10ms_gap_|run|___20ms_task_time___|end|
     *                       |^^^^^^^^^|
     *                         parallel
     * </pre>
     * <p/>
     * With metalocking:
     * <pre>
     * Task1: |run|___20ms_task_time___|end|
     *        ^acquire the metalock        ^release the metalock
     * Task2: |_10ms_gap_|___________________run|___20ms_task_time___|end|
     *                   |^^^^^^^^^^^^^^^^^|^acquire the metalock
     *                      wait for the metalock
     * </pre>
     *
     * @param startGapTime - a gap time before subsequent thread start in milliseconds
     * @param tasks        - varargs of Runnable
     * @throws InterruptedException
     */
    public static void runConcurrent(long startGapTime, Runnable... tasks) throws InterruptedException {
        final ReentrantLock lock = new ReentrantLock();
        final Condition started = lock.newCondition();

        ArrayList<Thread> threads = new ArrayList<>(tasks.length);

        for (Runnable task : tasks) {
            threads.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        lock.lock();
                        started.signal();
                    } finally {
                        lock.unlock();
                    }
                    task.run();
                }
            }));
        }

        for (Thread thread : threads) {
            try {
                lock.lock();
                thread.start();
                started.await();
            } finally {
                lock.unlock();
            }
            Thread.sleep(startGapTime);
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
