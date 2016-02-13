package io.github.xantorohara.metalock.app;


import io.github.xantorohara.metalock.MetaLockAspect;
import io.github.xantorohara.metalock.NameLockAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan("io.github.xantorohara.metalock")
public class DemoApplication {
    @Bean
    public MetaLockAspect getMetaLockAspect() {
        return new MetaLockAspect();
    }

    @Bean
    public NameLockAspect getNamedLockAspect() {
        return new NameLockAspect();
    }
}
