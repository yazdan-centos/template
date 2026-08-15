package org.example.template;

import org.example.template.config.BootstrapAdminInitializer;
import org.example.template.person.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.jwt.issuer=bootstrap-persons-test")
class BootstrapAdminInitializerTests {

    @Autowired
    private BootstrapAdminInitializer initializer;

    @Autowired
    private PersonRepository personRepository;

    @Test
    void importsMockPersonsAndRemainsIdempotent() {
        assertThat(personRepository.count()).isEqualTo(50);

        initializer.run(null);

        assertThat(personRepository.count()).isEqualTo(50);
        assertThat(personRepository.existsByEmailIgnoreCase("FCORY0@PCWORLD.COM")).isTrue();
    }
}
