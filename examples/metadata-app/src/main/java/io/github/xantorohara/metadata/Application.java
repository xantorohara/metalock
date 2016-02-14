package io.github.xantorohara.metadata;

import io.github.xantorohara.metalock.MetaLockAspect;
import io.github.xantorohara.metalock.NameLockAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    @Bean
    public MetaLockAspect getMetaLockAspect() {
        return new MetaLockAspect();
    }

    @Bean
    public NameLockAspect getNamedLockAspect() {
        return new NameLockAspect();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
