package io.github.xantorohara.metadata.repository;

import io.github.xantorohara.metadata.Application;
import io.github.xantorohara.metadata.entity.Metadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class MetadataRepositoryTest {

    @Autowired
    MetadataRepository metadataRepository;

    @Test
    public void saveTest() {
        metadataRepository.save(new Metadata("Key", "Value"));
        Metadata metadata = metadataRepository.findByKey("Key");
        assertThat(metadata.getValue(), equalTo("Value"));
    }
}
