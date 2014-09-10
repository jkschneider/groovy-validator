package com.github.jkschneider.groovy.validator;

public interface Validatable<V> {
    ValidationResult<V> validate();
}
