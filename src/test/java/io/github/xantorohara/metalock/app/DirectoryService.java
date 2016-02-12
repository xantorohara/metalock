package io.github.xantorohara.metalock.app;

import io.github.xantorohara.metalock.NameLock;
import org.springframework.stereotype.Service;

@Service
public class DirectoryService {

    @NameLock("PublicDomain")
    public void createDirectoryInPublicDomain(String directoryName) {
        System.out.println("1");
    }

    @NameLock("PublicDomain")
    public void indexDirectoriesInPublicDomain() {
        System.out.println("1");
    }
}
