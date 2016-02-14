package io.github.xantorohara.metalock.app;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DummyAuditor {

    private ConcurrentLinkedQueue<String> actions = new ConcurrentLinkedQueue<>();

    public ConcurrentLinkedQueue<String> getActions() {
        return actions;
    }

    /**
     * Just collect actions in the queue
     * @param action
     */
    public void logAction(String action) {
        actions.add(action);
    }
}
