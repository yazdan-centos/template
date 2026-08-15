package org.example.template.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.template.person.Person;
import org.example.template.person.PersonRepository;
import org.example.template.user.AppUser;
import org.example.template.user.Role;
import org.example.template.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final Resource mockPersonsResource;
    private final boolean adminEnabled;
    private final boolean personsEnabled;
    private final String username;
    private final String password;

    public BootstrapAdminInitializer(
            UserRepository userRepository,
            PersonRepository personRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            @Value("classpath:static/persons.json") Resource mockPersonsResource,
            @Value("${app.bootstrap-admin.enabled}") boolean adminEnabled,
            @Value("${app.bootstrap-persons.enabled:true}") boolean personsEnabled,
            @Value("${app.bootstrap-admin.username}") String username,
            @Value("${app.bootstrap-admin.password}") String password
    ) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.mockPersonsResource = mockPersonsResource;
        this.adminEnabled = adminEnabled;
        this.personsEnabled = personsEnabled;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPersons();
        seedAdmin();
    }

    private void seedPersons() {
        if (!personsEnabled) {
            return;
        }

        List<MockPerson> mockPersons;
        try (var inputStream = mockPersonsResource.getInputStream()) {
            mockPersons = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read classpath:static/persons.json", exception);
        }

        Set<String> processedEmails = new HashSet<>();
        List<Person> newPersons = mockPersons.stream()
                .map(this::toPerson)
                .filter(person -> processedEmails.add(person.getEmail()))
                .filter(person -> !personRepository.existsByEmailIgnoreCase(person.getEmail()))
                .toList();

        personRepository.saveAll(newPersons);
    }

    private Person toPerson(MockPerson mockPerson) {
        if (!StringUtils.hasText(mockPerson.firstName())
                || !StringUtils.hasText(mockPerson.lastName())
                || !StringUtils.hasText(mockPerson.email())) {
            throw new IllegalStateException("persons.json contains a person with a blank required field");
        }

        return new Person(
                mockPerson.firstName().trim(),
                mockPerson.lastName().trim(),
                mockPerson.email().trim().toLowerCase(Locale.ROOT)
        );
    }

    private void seedAdmin() {
        if (!adminEnabled) {
            return;
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "Bootstrap admin is enabled, but APP_BOOTSTRAP_ADMIN_USERNAME or "
                            + "APP_BOOTSTRAP_ADMIN_PASSWORD is empty"
            );
        }
        if (!userRepository.existsByUsernameIgnoreCase(username.trim())) {
            userRepository.save(new AppUser(
                    username.trim(),
                    passwordEncoder.encode(password),
                    Role.ADMIN
            ));
        }
    }

    private record MockPerson(
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String email
    ) {
    }
}
