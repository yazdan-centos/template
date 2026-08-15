package org.example.template.person;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public List<PersonResponse> findAll() {
        return personRepository.findAllByOrderByLastNameAscFirstNameAsc()
                .stream()
                .map(PersonResponse::from)
                .toList();
    }

    @Transactional
    public PersonResponse create(PersonRequest request) {
        Person person = new Person(
                request.firstName().trim(),
                request.lastName().trim(),
                request.email().trim().toLowerCase()
        );
        return PersonResponse.from(personRepository.save(person));
    }

    @Transactional
    public PersonResponse update(long id, PersonRequest request) {
        Person person = findById(id);
        person.update(
                request.firstName().trim(),
                request.lastName().trim(),
                request.email().trim().toLowerCase()
        );
        return PersonResponse.from(person);
    }

    @Transactional
    public void delete(long id) {
        personRepository.delete(findById(id));
    }

    private Person findById(long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
    }
}

