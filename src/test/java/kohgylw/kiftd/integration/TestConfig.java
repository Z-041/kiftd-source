package kohgylw.kiftd.integration;

import kohgylw.kiftd.printer.Printer;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class TestConfig {

    @PostConstruct
    public void init() {
        if (Printer.instance == null) {
            Printer.init(false);
        }
    }
}
