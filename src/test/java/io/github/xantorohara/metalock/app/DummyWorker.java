package io.github.xantorohara.metalock.app;

import org.springframework.stereotype.Component;

@Component
public class DummyWorker {
    /**
     * Just sleeps specified amount of time
     *
     * @param sleepTime
     */
    public void doSomeWork(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignore) {
        }
    }
}
