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
public class NameLockAspectTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    DemoRegistryService demoRegistryService;

    @Test
    public void singleWriteToThePublicDomainShouldWork() throws InterruptedException {
        demoRegistryService.createDirectoryInThePublicDomain("Documents");
        Queue<String> actions = demoRegistryService.getAuditor().getActions();

        assertThat(actions.poll(), equalTo("Creating Public Documents"));
        assertThat(actions.poll(), equalTo("Created Public Documents"));
        assertThat(actions.poll(), is(nullValue()));
    }

    @Test
    public void concurrentWritesToThePublicDomainShouldBeSerial() throws InterruptedException {
        Thread[] threads = {
                new Thread(() -> demoRegistryService.createDirectoryInThePublicDomain("Documents")),
                new Thread(() -> demoRegistryService.createDirectoryInThePublicDomain("Projects")),
                new Thread(demoRegistryService::indexPublicDomain),
        };

        for (Thread thread : threads) {
            thread.start();
            sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Queue<String> actions = demoRegistryService.getAuditor().getActions();

        assertThat(actions.poll(), equalTo("Creating Public Documents"));
        assertThat(actions.poll(), equalTo("Created Public Documents"));
        assertThat(actions.poll(), equalTo("Creating Public Projects"));
        assertThat(actions.poll(), equalTo("Created Public Projects"));
        assertThat(actions.poll(), equalTo("Indexing Public"));
        assertThat(actions.poll(), equalTo("Indexed Public"));
        assertThat(actions.poll(), is(nullValue()));
    }

    @Test
    public void concurrentWritesToTheDifferentDomainsCanBeParallel() throws InterruptedException {
        Thread[] threads = {
                new Thread(demoRegistryService::indexPublicDomain),
                new Thread(demoRegistryService::indexDomain),
        };

        for (Thread thread : threads) {
            thread.start();
            sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Queue<String> actions = demoRegistryService.getAuditor().getActions();

        assertThat(actions.poll(), equalTo("Indexing Public"));
        assertThat(actions.poll(), equalTo("Indexing Personal"));
        assertThat(actions.poll(), equalTo("Indexed Public"));
        assertThat(actions.poll(), equalTo("Indexed Personal"));
        assertThat(actions.poll(), is(nullValue()));
    }

    @Test
    public void backupAcquiresBothLocksButSubsequentOperationsCanWorkInParallel() throws InterruptedException {
        Thread[] threads = {
                new Thread(demoRegistryService::backupDomains),
                new Thread(demoRegistryService::indexPublicDomain),
                new Thread(demoRegistryService::indexDomain),
        };

        for (Thread thread : threads) {
            thread.start();
            sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Queue<String> actions = demoRegistryService.getAuditor().getActions();

        assertThat(actions.poll(), equalTo("Backup started"));
        assertThat(actions.poll(), equalTo("Backup done"));
        assertThat(actions.poll(), equalTo("Indexing Public"));
        assertThat(actions.poll(), equalTo("Indexing Personal"));
        assertThat(actions.poll(), equalTo("Indexed Public"));
        assertThat(actions.poll(), equalTo("Indexed Personal"));
        assertThat(actions.poll(), is(nullValue()));
    }
}