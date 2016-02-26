package io.github.xantorohara.metalock.app;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DummyAuditor {

    private List<String> actions = new CopyOnWriteArrayList<>();

    public List<String> takeActions() {
        try {
            return new ArrayList<>(actions);
        } finally {
            actions.clear();
        }
    }

    /**
     * Just collect actions in the list
     *
     * @param action
     */
    public void logAction(String action) {
        actions.add(action);
    }
}
