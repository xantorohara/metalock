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
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static io.github.xantorohara.metalock.TestUtils.runConcurrent;
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
    @Repeat(3)
    public void concurrentWritesUsingTableLockingAreSerial() throws InterruptedException {
        runConcurrent(100,
                () -> metadataService.createMetadataUsingTableLocking("SomeKey1", "SomeValue1"),
                () -> metadataService.createMetadataUsingTableLocking("SomeKey2", "SomeValue2")
        );

        Metadata metadata1 = metadataRepository.findByKey("SomeKey1");
        assertThat(metadata1.getValue(), equalTo("SomeValue1"));

        Metadata metadata2 = metadataRepository.findByKey("SomeKey2");
        assertThat(metadata2.getValue(), equalTo("SomeValue2"));
    }

    @Test
    @Repeat(3)
    public void concurrentWritesWithDifferentKeysCanBeParallel() throws InterruptedException {
        runConcurrent(100,
                () -> metadataService.createMetadata("SomeKey1", "SomeValue1"),
                () -> metadataService.createMetadata("SomeKey2", "SomeValue2")
        );

        Metadata metadata1 = metadataRepository.findByKey("SomeKey1");
        assertThat(metadata1.getValue(), equalTo("SomeValue1"));

        Metadata metadata2 = metadataRepository.findByKey("SomeKey2");
        assertThat(metadata2.getValue(), equalTo("SomeValue2"));
    }

    @Test
    @Repeat(3)
    public void concurrentWritesWithTheSameKeyShouldBeSerial() throws InterruptedException {
        runConcurrent(100,
                () -> metadataService.createMetadata("SomeKey", "SomeValue1"),
                () -> metadataService.createMetadata("SomeKey", "SomeValue2")
        );

        Metadata metadata = metadataRepository.findByKey("SomeKey");
        assertThat(metadata.getValue(), equalTo("SomeValue2"));
    }

    @Test
    @Repeat(3)
    public void serialWritesWhenKeysAreDifferentButTheSameUsername() throws InterruptedException {
        runConcurrent(100,
                () -> metadataService.createMetadata("Monkey", "SomeKey1", "SomeValue1"),
                () -> metadataService.createMetadata("Monkey", "SomeKey2", "SomeValue2")
        );

        Metadata metadata1 = metadataRepository.findByKey("SomeKey1");
        assertThat(metadata1.getValue(), equalTo("SomeValue1"));

        Metadata metadata2 = metadataRepository.findByKey("SomeKey2");
        assertThat(metadata2.getValue(), equalTo("SomeValue2"));
    }

    @Test
    @Repeat(3)
    public void serialWritesWhenUsernamesAreDifferentButTheSameKey() throws InterruptedException {
        runConcurrent(100,
                () -> metadataService.createMetadata("Donkey", "SomeKey", "SomeValue1"),
                () -> metadataService.createMetadata("Monkey", "SomeKey", "SomeValue2")
        );

        Metadata metadata1 = metadataRepository.findByKey("SomeKey");
        assertThat(metadata1.getValue(), equalTo("SomeValue2"));
    }

    @Test
    @Repeat(3)
    public void parallelWritesWhenKeysAndUsernamesAreDifferent() throws InterruptedException {
        runConcurrent(100,
                () -> metadataService.createMetadata("Donkey", "SomeKey1", "SomeValue1"),
                () -> metadataService.createMetadata("Monkey", "SomeKey2", "SomeValue2")
        );

        Metadata metadata1 = metadataRepository.findByKey("SomeKey1");
        assertThat(metadata1.getValue(), equalTo("SomeValue1"));

        Metadata metadata2 = metadataRepository.findByKey("SomeKey2");
        assertThat(metadata2.getValue(), equalTo("SomeValue2"));
    }
}
