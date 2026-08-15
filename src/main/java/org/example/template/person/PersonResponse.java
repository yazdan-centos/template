package org.example.template.person;

public record PersonResponse(Long id, String firstName, String lastName, String email) {

    static PersonResponse from(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getEmail()
        );
    }
}

