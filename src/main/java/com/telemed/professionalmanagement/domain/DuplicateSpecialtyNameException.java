package com.telemed.professionalmanagement.domain;

public class DuplicateSpecialtyNameException extends DomainException {

    public DuplicateSpecialtyNameException(String name) {
        super("A specialty with name already exists: " + name);
    }
}
