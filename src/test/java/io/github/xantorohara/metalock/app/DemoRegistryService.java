package io.github.xantorohara.metalock.app;

import io.github.xantorohara.metalock.MetaLock;
import io.github.xantorohara.metalock.NameLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This dummy service maintains some Registry.
 * This Registry has Domains, Directories, Records and Indexes.
 * Other service use methods provided by this Registry.
 * <p/>
 * This service just demonstrates usage of @NameLock and @MetLock annotations.
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoRegistryService {

    private static final String PUBLIC_DOMAIN = "PublicDomain";
    private static final String PERSONAL_DOMAIN = "PersonalDomain";

    private final Map<String, String> recordsDummyStorage = new ConcurrentHashMap<>();

    @Autowired
    private DummyAuditor auditor;
    @Autowired
    private DummyWorker worker;

    public DummyAuditor getAuditor() {
        return auditor;
    }

    public Map<String, String> getRecordsDummyStorage() {
        return recordsDummyStorage;
    }

    /**
     * Create a new Directory in the Public Domain.
     * <p/>
     * For a some reason we can't create new Directories in parallel.
     * We have to acquire a lock before write operations.
     */
    @NameLock(PUBLIC_DOMAIN)
    public void createDirectoryInThePublicDomain(String directoryName) {
        auditor.logAction("Creating Public " + directoryName);
        worker.doSomeWork(200);
        auditor.logAction("Created Public " + directoryName);
    }

    /**
     * Create an Index for the Records in the Public Domain.
     * <p/>
     * We have to acquire a lock for this Domain before write operations.
     */
    @NameLock(PUBLIC_DOMAIN)
    public void indexPublicDomain() {
        auditor.logAction("Indexing Public");
        worker.doSomeWork(200);
        auditor.logAction("Indexed Public");
    }

    /**
     * Create an Index for the Records in the Personal Domain.
     * <p/>
     * We have to acquire a lock for this Domain before write operations.
     */
    @NameLock(PERSONAL_DOMAIN)
    public void indexPersonalDomain() {
        auditor.logAction("Indexing Personal");
        worker.doSomeWork(200);
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
        worker.doSomeWork(200);
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
        worker.doSomeWork(200);
        if (value == null) {
            auditor.logAction("Save insert " + recordKey + " " + recordValue);
            recordsDummyStorage.put(recordKey, recordValue);
        } else {
            auditor.logAction("Save update " + recordKey + " " + recordValue);
            recordsDummyStorage.put(recordKey, recordValue);
        }
    }

    /**
     * Remove all records from the Registry
     */
    public void clearRecords() {
        recordsDummyStorage.clear();
    }

    /**
     * In our magic demo world users have unique combination of first and last names
     *
     * @param firstName
     * @param lastName
     */
    @MetaLock(name = "User", param = {"firstName", "lastName"})
    public void addMoneyForUser(String firstName, String lastName, int amountOfMoney) {
        auditor.logAction("Add " + amountOfMoney + " money for " + firstName + " " + lastName);
        worker.doSomeWork(200);
        auditor.logAction("Added " + amountOfMoney + " money for " + firstName + " " + lastName);
    }
}
