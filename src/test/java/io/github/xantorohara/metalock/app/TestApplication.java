package io.github.xantorohara.metalock.app;


import io.github.xantorohara.metalock.MetaLockAspect;
import io.github.xantorohara.metalock.NameLockAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("io.github.xantorohara.metalock")
public class TestApplication {
    @Bean
    public MetaLockAspect getMetaLockAspect() {
        return new MetaLockAspect();
    }

    @Bean
    public NameLockAspect getNamedLockAspect() {
        return new NameLockAspect();
    }

}
