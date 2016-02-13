package io.github.xantorohara.metalock.app;

public class ThreadUtils {
    public static void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignore) {
        }
    }
}
