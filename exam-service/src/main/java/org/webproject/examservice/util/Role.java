package org.webproject.examservice.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    STUDENT,TEACHER,ADMIN;

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null) return null;
        return Role.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
