package io.github.xantorohara.metalock;

import io.github.xantorohara.metalock.app.DirectoryService;
import io.github.xantorohara.metalock.app.Sleep;
import io.github.xantorohara.metalock.app.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Queue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestApplication.class)
public class NameLockAspectTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    DirectoryService directoryService;

    @Test
    public void concurrentWritesToThePublicDomainShouldBeSerial() throws InterruptedException {
        Thread[] threads = {
                new Thread(() -> directoryService.createDirectoryInThePublicDomain("Documents")),
                new Thread(() -> directoryService.createDirectoryInThePublicDomain("Projects")),
                new Thread(() -> directoryService.indexDirectoriesInPublicDomain()),
        };

        for (Thread thread : threads) {
            thread.start();
            Sleep.sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Queue<String> actions = directoryService.getAuditor().getActions();

        assertThat(actions.poll(), equalTo("Creating Documents"));
        assertThat(actions.poll(), equalTo("Created Documents"));
        assertThat(actions.poll(), equalTo("Creating Projects"));
        assertThat(actions.poll(), equalTo("Created Projects"));
        assertThat(actions.poll(), equalTo("Indexing"));
        assertThat(actions.poll(), equalTo("Indexed"));
        assertThat(actions.poll(), is(nullValue()));
    }
}