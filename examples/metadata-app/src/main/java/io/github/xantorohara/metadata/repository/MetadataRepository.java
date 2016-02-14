package io.github.xantorohara.metadata.repository;

import io.github.xantorohara.metadata.entity.Metadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MetadataRepository extends JpaRepository<Metadata, UUID> {
    public Metadata findByKey(String key);
}
