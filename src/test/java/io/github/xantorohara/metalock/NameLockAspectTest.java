package io.github.xantorohara.metalock;

import io.github.xantorohara.metalock.app.DirectoryService;
import io.github.xantorohara.metalock.app.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestApplication.class)
public class NameLockAspectTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DirectoryService directoryService;

    @Test
    public void singleSaveTest() throws InterruptedException {
        directoryService.createDirectoryInPublicDomain("Test");

        System.out.println(1);
//        Thread thread1 = new Thread(() -> {
//            chpocService.createMetadata("KeyA", "Val_AAA");
//        });
//
//        thread1.start();
//        thread1.join();
    }


}