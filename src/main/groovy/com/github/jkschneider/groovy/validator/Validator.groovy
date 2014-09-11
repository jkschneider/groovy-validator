package com.github.jkschneider.groovy.validator

import javax.validation.Valid
import javax.validation.constraints.NotNull

import static com.github.jkschneider.groovy.validator.ValidationCheck.*

class Validator<E> {
    Collection<ValidationCheck> checks = []

    @SuppressWarnings(["GroovyAssignabilityCheck"])
    ValidationResult<E> validate(E obj) {
        Map failures = checks.inject([:].withDefault({[]})) { Map<String,ValidationFailure> acc, ValidationCheck c ->
            try {
                if (!c.check(obj) && c.failureMessage) {
                    if (acc.containsKey(c.field))
                        acc[c.field].messages.add(c.failureMessage)
                    else
                        acc.put(c.field, new ValidationFailure(c.field, c.failureMessage))
                }
            }
            catch (Exception e) {
                e.printStackTrace()
            }
            acc
        }

        new ValidationResult(obj, failures.values())
    }

    Validator<E> ifPresent(String field, ValidationCheck... cs) {
        checks.add(or(checkQuietly({ it == null }), ValidationCheck.and(cs)).againstField(field))
        this
    }

    Validator<E> has(String field, ValidationCheck... cs) {
        checks.add(check({ it != null }, "cannot be null").againstField(field))
        checks.addAll(cs*.againstField(field))
        this
    }

    /**
     * For when you want to embed validator(s) inside another
     * @param field - prepended onto each field rule of the embedded validator(s)
     * @param nested
     * @return
     */
    Validator<E> hasNested(String field, Validator<E>... nested){
        checks.add(check({ it != null }, "cannot be null").againstField(field))
        checks.addAll(nested*.checks.flatten{ chk -> chk.againstField(field + "." + chk.field)})
        this
    }

    Validator<E> hasNonEmpty(String field, ValidationCheck... cs) {
        checks.add(check({ it != null }, "cannot be null").againstField(field))
        checks.add(check({ !it.isEmpty() }, "cannot be empty").againstField(field))
        checks.addAll(cs*.againstField(field))
        this
    }

    Validator<E> and(ValidationCheck... cs) {
        checks.addAll(cs)
        this
    }

    Validator<E> jsr303(Class<E> eClass) {
        def tests = recurseApplyJsr303(eClass, [], "")
        checks.addAll(tests)
        this
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    private static Collection<ValidationCheck> recurseApplyJsr303(Class clazz, Collection<Closure> tests, String path) {
        clazz.declaredFields.each { field ->
            def fieldPath = ((path.isEmpty()) ? "" : (path + ".")) + field.getName()
            field.setAccessible(true)
            field.annotations.each { ann ->
                if (ann.annotationType() == NotNull.class)
                    tests.add(check({ obj -> fieldVal(fieldPath, obj) != null }, "cannot be null"))
                if (ann.annotationType() == Valid.class)
                    recurseApplyJsr303(Class.forName(field.getGenericType().typeName), tests, fieldPath)
            }
        }
        tests
    }
}
