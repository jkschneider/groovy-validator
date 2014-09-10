package com.github.jkschneider.groovy.validator;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

public class ValidationResult<E> {
    Optional<E> validated;
    Collection<ValidationFailure> failures;

    public ValidationResult(E validated, Collection<ValidationFailure> failures) {
        this.validated = Optional.ofNullable(failures.isEmpty() ? validated : null);
        this.failures = failures;
    }

    public void either(Consumer<E> successHandler, Consumer<Collection<ValidationFailure>> failureHandler) {
        validated.ifPresent(successHandler);
        if(!validated.isPresent())
            failureHandler.accept(failures);
    }

    public boolean passed() { return failures.isEmpty(); }

    public boolean passed(String field) { return !failed(field); }

    public boolean failed() { return !passed(); }

    public boolean failed(String field) {
        return failures.stream().anyMatch(f -> f.getField().equals(field));
    }

    public Optional<E> getValidated() {
        return validated;
    }

    public void setValidated(Optional<E> validated) {
        this.validated = validated;
    }

    public Collection<ValidationFailure> getFailures() {
        return failures;
    }

    public void setFailures(Collection<ValidationFailure> failures) {
        this.failures = failures;
    }
}
