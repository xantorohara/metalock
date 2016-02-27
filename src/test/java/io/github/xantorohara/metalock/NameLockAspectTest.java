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

import static io.github.xantorohara.metalock.TestUtils.runConcurrent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DemoApplication.class)
public class NameLockAspectTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    DemoRegistryService demoRegistryService;

    @Before
    public void before() {
        demoRegistryService.clearRecords();
    }

    @Test
    @Repeat(2)
    public void singleWriteToThePublicDomainShouldWork() throws InterruptedException {
        demoRegistryService.createDirectoryInThePublicDomain("Documents");
        List<String> actions = demoRegistryService.getAuditor().takeActions();
        assertThat(actions, contains("Creating Public Documents", "Created Public Documents"));
    }

    @Test
    @Repeat(3)
    public void concurrentWritesToThePublicDomainShouldBeSerial() throws InterruptedException {
        runConcurrent(100,
                () -> demoRegistryService.createDirectoryInThePublicDomain("Documents"),
                () -> demoRegistryService.createDirectoryInThePublicDomain("Projects"),
                demoRegistryService::indexPublicDomain
        );

        List<String> actions = demoRegistryService.getAuditor().takeActions();

        assertThat(actions, contains(
                "Creating Public Documents",
                "Created Public Documents",
                "Creating Public Projects",
                "Created Public Projects",
                "Indexing Public",
                "Indexed Public"
        ));
    }

    @Test
    @Repeat(3)
    public void concurrentWritesToTheDifferentDomainsCanBeParallel() throws InterruptedException {
        runConcurrent(100,
                demoRegistryService::indexPublicDomain,
                demoRegistryService::indexPersonalDomain
        );

        List<String> actions = demoRegistryService.getAuditor().takeActions();

        assertThat(actions, contains(
                "Indexing Public", "Indexing Personal",
                "Indexed Public", "Indexed Personal"
        ));
    }

    @Test
    @Repeat(3)
    public void backupAcquiresBothLocksButSubsequentOperationsCanBeParallel() throws InterruptedException {
        runConcurrent(100,
                demoRegistryService::backupDomains,
                demoRegistryService::indexPublicDomain,
                demoRegistryService::indexPersonalDomain
        );

        List<String> actions = demoRegistryService.getAuditor().takeActions();

        assertThat(actions.get(0), equalTo("Backup started"));
        assertThat(actions.get(1), equalTo("Backup done"));
        assertThat(actions.get(2), isOneOf("Indexing Public", "Indexing Personal"));
        assertThat(actions.get(3), isOneOf("Indexing Public", "Indexing Personal"));
        assertThat(actions.get(4), isOneOf("Indexed Public", "Indexed Personal"));
        assertThat(actions.get(5), isOneOf("Indexed Public", "Indexed Personal"));
    }
}