package io.github.xantorohara.metadata;

import io.github.xantorohara.metadata.entity.Metadata;
import io.github.xantorohara.metadata.repository.MetadataRepository;
import io.github.xantorohara.metadata.service.MetadataService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class MetadataServiceTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MetadataService metadataService;

    @Autowired
    MetadataRepository metadataRepository;

    @Test
    public void concurrentWritesUsingTableLockingAreSerial() throws InterruptedException {
        Thread[] threads = {
                new Thread(() -> metadataService.createMetadataUsingTableLocking("SomeKey1", "SomeValue1")),
                new Thread(() -> metadataService.createMetadataUsingTableLocking("SomeKey2", "SomeValue2"))
        };
        for (Thread thread : threads) {
            thread.start();
            Thread.sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Metadata metadata1 = metadataRepository.findByKey("SomeKey1");
        assertThat(metadata1.getValue(), equalTo("SomeValue1"));

        Metadata metadata2 = metadataRepository.findByKey("SomeKey2");
        assertThat(metadata2.getValue(), equalTo("SomeValue2"));
    }

    @Test
    public void concurrentWritesWithDifferentKeysCanBeParallel() throws InterruptedException {
        Thread[] threads = {
                new Thread(() -> metadataService.createMetadata("SomeKey1", "SomeValue1")),
                new Thread(() -> metadataService.createMetadata("SomeKey2", "SomeValue2"))
        };
        for (Thread thread : threads) {
            thread.start();
            Thread.sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Metadata metadata1 = metadataRepository.findByKey("SomeKey1");
        assertThat(metadata1.getValue(), equalTo("SomeValue1"));

        Metadata metadata2 = metadataRepository.findByKey("SomeKey2");
        assertThat(metadata2.getValue(), equalTo("SomeValue2"));
    }

    @Test
    public void concurrentWritesWithTheSameKeyShouldBeSerial() throws InterruptedException {
        log.info("concurrentWritesWithTheSameKeyShouldBeSerial");

        Thread[] threads = {
                new Thread(() -> metadataService.createMetadata("SomeKey", "SomeValue1")),
                new Thread(() -> metadataService.createMetadata("SomeKey", "SomeValue2"))
        };

        for (Thread thread : threads) {
            thread.start();
            Thread.sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Metadata metadata = metadataRepository.findByKey("SomeKey");
        assertThat(metadata.getValue(), equalTo("SomeValue2"));
    }

    @Test
    public void serialWritesWhenKeysAreDifferentButTheSameUsername() throws InterruptedException {
        String username = "Monkey";

        Thread[] threads = {
                new Thread(() -> metadataService.createMetadata(username, "SomeKey1", "SomeValue1")),
                new Thread(() -> metadataService.createMetadata(username, "SomeKey2", "SomeValue2"))
        };

        for (Thread thread : threads) {
            thread.start();
            Thread.sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Metadata metadata1 = metadataRepository.findByKey("SomeKey1");
        assertThat(metadata1.getValue(), equalTo("SomeValue1"));

        Metadata metadata2 = metadataRepository.findByKey("SomeKey2");
        assertThat(metadata2.getValue(), equalTo("SomeValue2"));
    }

    @Test
    public void serialWritesWhenUsernamesAreDifferentButTheSameKey() throws InterruptedException {
        log.info("serialWritesWhenUsernamesAreDifferentButTheSameKey");
        Thread[] threads = {
                new Thread(() -> metadataService.createMetadata("Donkey", "SomeKey", "SomeValue1")),
                new Thread(() -> metadataService.createMetadata("Monkey", "SomeKey", "SomeValue2"))
        };

        for (Thread thread : threads) {
            thread.start();
            Thread.sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Metadata metadata1 = metadataRepository.findByKey("SomeKey");
        assertThat(metadata1.getValue(), equalTo("SomeValue2"));
    }

    @Test
    public void parallelWritesWhenKeysAndUsernamesAreDifferent() throws InterruptedException {

        Thread[] threads = {
                new Thread(() -> metadataService.createMetadata("Donkey", "SomeKey1", "SomeValue1")),
                new Thread(() -> metadataService.createMetadata("Monkey", "SomeKey2", "SomeValue2"))
        };

        for (Thread thread : threads) {
            thread.start();
            Thread.sleep(100);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Metadata metadata1 = metadataRepository.findByKey("SomeKey1");
        assertThat(metadata1.getValue(), equalTo("SomeValue1"));

        Metadata metadata2 = metadataRepository.findByKey("SomeKey2");
        assertThat(metadata2.getValue(), equalTo("SomeValue2"));
    }
}
