package io.github.xantorohara.metadata.service;

import io.github.xantorohara.metadata.entity.Metadata;
import io.github.xantorohara.metadata.repository.MetadataRepository;
import io.github.xantorohara.metalock.MetaLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetadataService {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MetadataRepository metadataRepository;

    @MetaLock(name = "Metadata", param = "key")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Metadata createMetadata(String key, String val) {
        log.info("Create Metadata {}", key);
        Metadata metadata = metadataRepository.findByKey(key);

        if (metadata == null) {
            metadata = new Metadata(key, val);
        } else {
            metadata.setValue(val);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ignore) {
        }

        metadata = metadataRepository.saveAndFlush(metadata);
        log.info("Created Metadata {}", key);
        return metadata;
    }

    /**
     * For some reason we need to prevent concurrent insertion of Metadada object for the same user.
     * Also value from the "metadataKey" parameter is unique constraint
     */
    @MetaLock(name = "Metadata", param = "metadataKey")
    @MetaLock(name = "User", param = "username")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Metadata createMetadata(String username, String metadataKey, String metadataVal) {
        log.info("Create Metadata {} for user {}", metadataKey, username);
        Metadata metadata = metadataRepository.findByKey(metadataKey);

        if (metadata == null) {
            metadata = new Metadata(metadataKey, metadataVal);
        } else {
            metadata.setValue(metadataVal);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ignore) {
        }

        metadata = metadataRepository.saveAndFlush(metadata);

        log.info("Create Metadata {} for user {}", metadataKey, username);
        return metadata;
    }
}
