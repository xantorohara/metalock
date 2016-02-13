package io.github.xantorohara.metalock.app;

import io.github.xantorohara.metalock.MetaLock;
import io.github.xantorohara.metalock.NameLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.xantorohara.metalock.app.ThreadUtils.sleep;

/**
 * This dummy service maintains some Registry.
 * This Registry has Domains, Directories, Records and Indexes.
 * Other service use methods provided by this Registry.
 * <p>
 * This service just demonstrates usage of @NameLock and @MetLock annotations.
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoRegistryService {

    private static final String PUBLIC_DOMAIN = "PublicDomain";
    private static final String PERSONAL_DOMAIN = "PersonalDomain";

    private final Map<String, String> recordsDummyStorage = new ConcurrentHashMap<>();

    @Autowired
    private Auditor auditor;

    public Auditor getAuditor() {
        return auditor;
    }

    public Map<String, String> getRecordsDummyStorage() {
        return recordsDummyStorage;
    }

    /**
     * Create a new Directory in the Public Domain.
     * <p>
     * For a some reason we can't create new Directories in parallel.
     * We have to acquire a lock before write operations.
     */
    @NameLock(PUBLIC_DOMAIN)
    public void createDirectoryInThePublicDomain(String directoryName) {
        auditor.logAction("Creating Public " + directoryName);
        sleep(200); //do some work
        auditor.logAction("Created Public " + directoryName);
    }

    /**
     * Create an Index for the Records in the Public Domain.
     * <p>
     * We have to acquire a lock for this Domain before write operations.
     */
    @NameLock(PUBLIC_DOMAIN)
    public void indexPublicDomain() {
        auditor.logAction("Indexing Public");
        sleep(200); //do some work
        auditor.logAction("Indexed Public");
    }

    /**
     * Create an Index for the Records in the Personal Domain.
     * <p>
     * We have to acquire a lock for this Domain before write operations.
     */
    @NameLock(PERSONAL_DOMAIN)
    public void indexDomain() {
        auditor.logAction("Indexing Personal");
        sleep(200); //do some work
        auditor.logAction("Indexed Personal");
    }

    /**
     * Backup all domains.
     * All write operations should be locked until the Backup is completed
     * We have to acquire both lock (Public and Personal) before write operations.
     */
    @NameLock({PUBLIC_DOMAIN, PERSONAL_DOMAIN})
    public void backupDomains() {
        auditor.logAction("Backup started");
        sleep(200); //do some work
        auditor.logAction("Backup done");
    }

    /**
     * Save (insert or update) record in the Registry.
     *
     * @param recordKey
     * @param recordValue
     */
    @MetaLock(name = "Record", param = "recordKey")
    public void saveRecord(String recordKey, String recordValue) {
        auditor.logAction("Save select " + recordKey);
        String value = recordsDummyStorage.get(recordKey);
        sleep(200); //do some work
        if (value == null) {
            auditor.logAction("Save insert " + recordKey);
            recordsDummyStorage.put(recordKey, recordValue);
        } else {
            auditor.logAction("Save update " + recordKey);
            recordsDummyStorage.put(recordKey, recordValue);
        }
    }
}
