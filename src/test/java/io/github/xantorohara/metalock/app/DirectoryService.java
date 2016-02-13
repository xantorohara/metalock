package io.github.xantorohara.metalock.app;

import io.github.xantorohara.metalock.NameLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import static io.github.xantorohara.metalock.app.Sleep.sleep;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DirectoryService {

    @Autowired
    private Auditor auditor;

    public Auditor getAuditor() {
        return auditor;
    }

    @NameLock("PublicDomain")
    public void createDirectoryInThePublicDomain(String directoryName) {
        auditor.logAction("Creating " + directoryName);
        sleep(200);
        auditor.logAction("Created " + directoryName);
    }

    @NameLock("PublicDomain")
    public void indexDirectoriesInPublicDomain() {
        auditor.logAction("Indexing");
        sleep(200);
        auditor.logAction("Indexed");
    }
}
