groovy-validator
================

A flexible validation framework that cooperates with JSR-303, inspired by the Play! Reads API

Motivation
----------

* The desire to define field-level validation by composing sets of simpler validation methods in a way that is independent of the class itself.

* Validation should continue until all rules have been tested rather than stopping at the first failure.

* Separate validation from deserialization.  When they are tied together, as in a combination of JSR-303 and Jackson, Jackson stops with an exception at the first validation failure, which does not allow a comprehensive list of failures to be discovered.

Introduction
------------

For all examples, we will use the simple model shown here:

```groovy
static class Person {
    @NotNull String lastName
    String firstName
    String email
    String email2
    Integer age
    String gender
    String phone
    Optional<String> middleName
    @Valid Address address
    LocalDate birthDate
}
```

Built-in validation checks
---------------------------
Built-in checks exist for all of the following:

* after/before
* alphaNumeric
* dateRange
* digits
* email
* equalTo
* length
* min/max
* minLength/maxLength
* nonEmpty
* oneOf
* pattern
* range


Single field validation
-----------------------

```groovy
def emailValidator = new Validator<Person>().has("email", email)
def validationResult = emailValidator.validate(new Person(email: "jon@jon.com"))

assert validationResult.passed()
validationResult.ifPassed({ person -> assert person.email == 'jon@jon.com' })

assert emailValidator.validate(new Person()).failed("email")
```

The returned `ValidationResult` provides pass-thru access to the object being validated or a collection of validation failures in the event that the object does not pass.


Multiple field validation
-------------------------

Validators can be easily be chained together to achieve multiple field validation.

```groovy
def comprehensiveValidator = new Validator<Person>()
                .has("age", min(16))
                .has("gender", oneOf("male", "female"))
                .ifPresent("email", check({ it.contains("@") }, "must contain a @"))
```

Validation of optional fields
-----------------------------

Validation rules can be applied only in the event that a field is present.

```groovy
def ageValidator = new Validator<Person>().ifPresent("age", min(16))
```

Applying multiple validation rules to a single field
----------------------------------------------------

When multiple checks are applied to a single field, they are both applied to the field ('and' behavior)

```groovy
def ageValidator = new Validator<Person>().has("age", min(16), max(50))
```

Defining custom validation checks
---------------------------------
```groovy
def customValidator = new Validator<Person>().has("lastName", check({ lastName -> lastName == 'smith' }, "custom failure message"))
```

Defining validation rules that apply to a composite of several fields
---------------------------------------------------------------------

```groovy
def compositeValidator = new Validator<Person>().and(
       check({ Person person -> /* do some fancy work with the whole person object at once */ }))
```


Defining 'or' criteria
-----------------------

``` groovy
def genderValidator = new Validator<Person>().has("gender",
                or(check({ it == "male" }, "must be a male"), check({ it == "female" }, "must be a female")))
```


Mixing in JSR-303 validation
----------------------------

JSR-303 validation can be used in combination with other validation rules

```groovy
def lastNameValidator = new Validator<Person>().jsr303(Person.class).has('age', min(16))
```
