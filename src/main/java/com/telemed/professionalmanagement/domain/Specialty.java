package com.telemed.professionalmanagement.domain;

public class Specialty {

    private final Long id;
    private final String name;
    private final String description;

    public Specialty(Long id, String name, String description) {
        validate(name, description);
        this.id = id;
        this.name = name.trim();
        this.description = description == null ? null : description.trim();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Specialty update(String name, String description) {
        return new Specialty(this.id, name, description);
    }

    private void validate(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new InvalidBusinessDataException("name is required");
        }
        if (name.trim().length() > 100) {
            throw new InvalidBusinessDataException("name must not exceed 100 characters");
        }
        if (description != null && description.trim().length() > 500) {
            throw new InvalidBusinessDataException("description must not exceed 500 characters");
        }
    }
}
