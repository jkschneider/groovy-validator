package com.github.jkschneider.groovy.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Validation errors resulting from either a malformed command or a violation
 * of business rules associated with the command
 */
public class ValidationFailure {
    String field;
    Collection<String> messages;

    public ValidationFailure(String field, Collection<String> messages) {
        this.field = field;
        this.messages = messages;
    }

    public ValidationFailure(String field, String message) {
        this(field, new ArrayList<>(Arrays.asList(message)));
    }

    /* For Jackson Only */
    public ValidationFailure() {}

    public void setField(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public void setMessages(Collection<String> messages) {
        this.messages = messages;
    }

    public Collection<String> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return "ValidationFailure{" +
                "field='" + field + '\'' +
                ", messages=" + messages +
                '}';
    }
}
