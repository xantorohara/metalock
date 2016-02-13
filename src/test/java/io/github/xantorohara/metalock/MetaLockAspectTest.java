package io.github.xantorohara.metalock;

import io.github.xantorohara.metalock.app.DemoApplication;
import io.github.xantorohara.metalock.app.DemoRegistryService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Queue;

import static io.github.xantorohara.metalock.app.ThreadUtils.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DemoApplication.class)
public class MetaLockAspectTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    DemoRegistryService demoRegistryService;

    @Test
    public void serialWritesShouldWork() throws InterruptedException {
        demoRegistryService.saveRecord("SomeKey", "SomeValue1");
        Queue<String> actions = demoRegistryService.getAuditor().getActions();

        assertThat(actions.poll(), equalTo("Save select SomeKey"));
        assertThat(actions.poll(), equalTo("Save insert SomeKey"));
        assertThat(actions.poll(), is(nullValue()));
        assertThat(demoRegistryService.getRecordsDummyStorage().get("SomeKey"), equalTo("SomeValue1"));

        demoRegistryService.saveRecord("SomeKey", "SomeValue2");

        assertThat(actions.poll(), equalTo("Save select SomeKey"));
        assertThat(actions.poll(), equalTo("Save update SomeKey"));
        assertThat(actions.poll(), is(nullValue()));
        assertThat(demoRegistryService.getRecordsDummyStorage().get("SomeKey"), equalTo("SomeValue2"));
    }

    @Test
    public void concurrentWritesOfRecordsWithSameKeyShouldBeSerial() throws InterruptedException {
        Thread[] threads = {
                new Thread(() -> demoRegistryService.saveRecord("SomeKey", "SomeValue")),
                new Thread(() -> demoRegistryService.saveRecord("SomeKey", "SomeValue"))
        };

        for (Thread thread : threads) {
            thread.start();
            sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Queue<String> actions = demoRegistryService.getAuditor().getActions();

        assertThat(actions.poll(), equalTo("Save select SomeKey"));
        assertThat(actions.poll(), equalTo("Save insert SomeKey"));
        assertThat(actions.poll(), equalTo("Save select SomeKey"));
        assertThat(actions.poll(), equalTo("Save update SomeKey"));
        assertThat(actions.poll(), is(nullValue()));
        assertThat(demoRegistryService.getRecordsDummyStorage().get("SomeKey"), equalTo("SomeValue"));
    }


    @Test
    public void concurrentWritesOfRecordsWithDifferentKeyCanBeParallel() throws InterruptedException {
        Thread[] threads = {
                new Thread(() -> demoRegistryService.saveRecord("SomeKey1", "SomeValue")),
                new Thread(() -> demoRegistryService.saveRecord("SomeKey2", "SomeValue"))
        };

        for (Thread thread : threads) {
            thread.start();
            sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Queue<String> actions = demoRegistryService.getAuditor().getActions();

        assertThat(actions.poll(), equalTo("Save select SomeKey1"));
        assertThat(actions.poll(), equalTo("Save select SomeKey2"));
        assertThat(actions.poll(), equalTo("Save insert SomeKey1"));
        assertThat(actions.poll(), equalTo("Save insert SomeKey2"));
        assertThat(actions.poll(), is(nullValue()));
        assertThat(demoRegistryService.getRecordsDummyStorage().get("SomeKey1"), equalTo("SomeValue"));
        assertThat(demoRegistryService.getRecordsDummyStorage().get("SomeKey2"), equalTo("SomeValue"));
    }

}