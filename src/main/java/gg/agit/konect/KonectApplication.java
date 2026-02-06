package gg.agit.konect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableRetry
@SpringBootApplication
@ConfigurationPropertiesScan
public class KonectApplication {

    public static void main(String[] args) {
        SpringApplication.run(KonectApplication.class, args);
    }

}
