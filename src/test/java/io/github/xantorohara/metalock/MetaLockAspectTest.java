package io.github.xantorohara.metalock;

import io.github.xantorohara.metalock.app.DemoApplication;
import io.github.xantorohara.metalock.app.DemoRegistryService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static io.github.xantorohara.metalock.TestUtils.runConcurrent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DemoApplication.class)
public class MetaLockAspectTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    DemoRegistryService demoRegistryService;

    @Before
    public void before() {
        demoRegistryService.clearRecords();
    }

    @Test
    public void serialWritesShouldWork() throws InterruptedException {
        List<String> actions;

        demoRegistryService.saveRecord("SomeKey", "SomeValue1");
        actions = demoRegistryService.getAuditor().takeActions();

        assertThat(actions, contains("Save select SomeKey", "Save insert SomeKey SomeValue1"));
        assertThat(demoRegistryService.getRecordsDummyStorage().get("SomeKey"), equalTo("SomeValue1"));

        demoRegistryService.saveRecord("SomeKey", "SomeValue2");
        actions = demoRegistryService.getAuditor().takeActions();

        assertThat(actions, contains("Save select SomeKey", "Save update SomeKey SomeValue2"));
        assertThat(demoRegistryService.getRecordsDummyStorage().get("SomeKey"), equalTo("SomeValue2"));
    }

    @Test
    @Repeat(3)
    public void concurrentWritesOfRecordsWithSameKeyShouldBeSerial() throws InterruptedException {
        runConcurrent(100,
                () -> demoRegistryService.saveRecord("SomeKey", "SomeValue1"),
                () -> demoRegistryService.saveRecord("SomeKey", "SomeValue2")
        );

        List<String> actions = demoRegistryService.getAuditor().takeActions();

        assertThat(actions, contains(
                "Save select SomeKey",
                "Save insert SomeKey SomeValue1",
                "Save select SomeKey",
                "Save update SomeKey SomeValue2"
        ));
        assertThat(demoRegistryService.getRecordsDummyStorage().get("SomeKey"), equalTo("SomeValue2"));
    }

    @Test
    @Repeat(3)
    public void concurrentWritesOfRecordsWithDifferentKeyCanBeParallel() throws InterruptedException {
        runConcurrent(100,
                () -> demoRegistryService.saveRecord("SomeKey1", "SomeValue"),
                () -> demoRegistryService.saveRecord("SomeKey2", "SomeValue")
        );

        List<String> actions = demoRegistryService.getAuditor().takeActions();

        assertThat(actions,  contains(
                "Save select SomeKey1",
                "Save select SomeKey2",
                "Save insert SomeKey1 SomeValue",
                "Save insert SomeKey2 SomeValue"
        ));

        assertThat(demoRegistryService.getRecordsDummyStorage().get("SomeKey1"), equalTo("SomeValue"));
        assertThat(demoRegistryService.getRecordsDummyStorage().get("SomeKey2"), equalTo("SomeValue"));
    }
}