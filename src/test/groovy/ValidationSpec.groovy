import com.github.jkschneider.groovy.validator.ValidationResult
import com.github.jkschneider.groovy.validator.Validator
import org.joda.time.LocalDate
import spock.lang.Specification

import javax.validation.Valid
import javax.validation.constraints.NotNull

import static com.github.jkschneider.groovy.validator.ValidationCheck.*

class ValidationSpec extends Specification {
    static class Address {
        @NotNull String address1
        String city
        String state
    }

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

    void "Validation passes with the same check on 2 fields"() {
        when:
        def validator = new Validator<Person>().has("email2", email).has("email",email)

        then:
        validator.validate(new Person(email: "place@place.com",email2: "bad")).failed()
    }

    void "Validator does not stop checking on first AND condition that is not satisfied"() {
        given:
        def lastNameValidator = new Validator<Person>().has("lastName", minLength(5),
                check({ ln -> false }, "Boom"))

        when:
        def valid = lastNameValidator.validate(new Person(lastName: "schn"))

        then:
        valid.failed()
        valid.failures.size() > 0
    }

    void "Validator `has` checks that field exists before running rules on it"() {
        when:
        def emailValidator = new Validator<Person>().has("email", email)

        then:
        emailValidator.validate(new Person(email: "jon@jon.com")).passed()
        emailValidator.validate(new Person()).failed("email")
    }

    void "Validator supports email checking"() {
        when:
        def ageValidator = new Validator<Person>().has("age", equalTo(16))

        then:
        ageValidator.validate(new Person(age: 16)).passed()
        ageValidator.validate(new Person(age: 15)).failed()
    }

    void "Validator supports range checking"() {
        when:
        def ageValidator = new Validator<Person>().has("age", range(16, 50))

        then:
        ageValidator.validate(new Person(age: 16)).passed()
        ageValidator.validate(new Person(age: 50)).passed()

        and:
        ageValidator.validate(new Person(age: 15)).failed()
        ageValidator.validate(new Person(age: 51)).failed()
    }

    void "Validator supports equality checking"() {
        when:
        def emailValidator = new Validator<Person>().has("email", email)

        then:
        emailValidator.validate(new Person(email: "jon@jon.com")).passed()
        emailValidator.validate(new Person(email: "jon@")).failed()
        emailValidator.validate(new Person()).failed()
    }

    void "Validator supports digit checking"() {
        when:
        def phoneValidator = new Validator<Person>().has("phone", digits)

        then:
        phoneValidator.validate(new Person(phone: "123")).passed()
        phoneValidator.validate(new Person(phone: "abc")).failed()
    }

    void "Validator supports alphaNumeric checking"() {
        when:
        def lastNameValidator = new Validator<Person>().has("lastName", alphaNumeric)

        then:
        lastNameValidator.validate(new Person(lastName: "jon1")).passed()
        lastNameValidator.validate(new Person(lastName: "jon1!")).failed()
    }

    void "Validator supports min and max length checking"() {
        when:
        def emailValidator = new Validator<Person>().has("email", minLength(3))
        def emailValidator2 = new Validator<Person>().has("email", maxLength(5))

        then:
        emailValidator.validate(new Person(email: "jon")).passed()
        emailValidator.validate(new Person(email: "jo")).failed()

        and:
        emailValidator2.validate(new Person(email: "jon@j")).passed()
        emailValidator2.validate(new Person(email: "jon@jon.com")).failed()
    }

    void "Iff success, validator provides the validated object"() {
        given:
        def validPerson = new Person(email: "jon@jon.com")
        def invalidPerson = new Person()

        when:
        def emailValidator = new Validator<Person>().has("email")

        then:
        validPerson == emailValidator.validate(validPerson).validated.get()
        !emailValidator.validate(invalidPerson).validated.isPresent()
    }

    void "Validator fails with null message when null object provided"() {
        when:
        def emailValidator = new Validator<Person>().has("email")

        then:
        emailValidator.validate(null).failed()
        emailValidator.validate(null).failures.size() == 1
    }

    void "Validator allows for validation on nested fields"() {
        when:
        def addressValidator = new Validator<Person>().has("address.state", check({ it == "mo" }, "must be a valid state"))

        then:
        addressValidator.validate(new Person(address: new Address(state: "mo"))).passed()
        addressValidator.validate(new Person(address: new Address(state: "il"))).failed()
        addressValidator.validate(new Person(address: new Address())).failed()
        addressValidator.validate(new Person()).failed()
    }

    void "Validator applies predicates to field validation by and-ing them together"() {
        when:
        def ageValidator = new Validator<Person>().has("age", min(16), max(50))

        then:
        ageValidator.validate(new Person(age: 16)).passed()
        ageValidator.validate(new Person(age: 15)).failed()
        ageValidator.validate(new Person(age: 51)).failed()
    }

    void "Validator supports `min` and `max` on numeric types"() {
        when:
        def ageValidator = new Validator<Person>().has("age", min(16), max(50))

        then:
        ageValidator.validate(new Person(age: 16)).passed()
        ageValidator.validate(new Person(age: 15)).failed()
        ageValidator.validate(new Person(age: 52)).failed()
    }

    void "Validator supports 'before' and 'after' on date types"() {
        when:
        def dateValidator = new Validator<Person>().has("birthDate", before(new LocalDate()), after(new LocalDate(1200, 4, 1)))

        then:
        dateValidator.validate(new Person(birthDate: new LocalDate())).failed()
        dateValidator.validate(new Person(birthDate: new LocalDate(1200, 4, 2))).passed()
        dateValidator.validate(new Person(birthDate: new LocalDate(1200, 3, 30))).failed()
    }

    void "Validator supports range on date types"() {
        when:
        def dateValidator = new Validator<Person>().has("birthDate", dateRange(new LocalDate(1200, 4, 1), new LocalDate()))

        then:
        dateValidator.validate(new Person(birthDate: new LocalDate())).failed()
        dateValidator.validate(new Person(birthDate: new LocalDate(1200, 4, 2))).passed()
        dateValidator.validate(new Person(birthDate: new LocalDate(1200, 3, 30))).failed()
    }

    void "Validator supports `nonEmpty` on string types"() {
        when:
        def emailValidator = new Validator<Person>().has("email", nonEmpty)
        def emailValidator2 = new Validator<Person>().hasNonEmpty("email")

        then:
        emailValidator.validate(new Person(email: "jon@jon.com")).passed()
        emailValidator.validate(new Person(email: "")).failed()

        and:
        emailValidator2.validate(new Person(email: "jon@jon.com")).passed()
        emailValidator2.validate(new Person(email: "")).failed()
    }

    void "Validator allows for validation on a optional field"() {
        when:
        def ageValidator = new Validator<Person>().ifPresent("age", min(16))
        def middleNameValidator = new Validator<Person>().ifPresent("middleName", nonEmpty)

        then:
        ageValidator.validate(new Person(age: 16)).passed()
        ageValidator.validate(new Person()).passed()
        ageValidator.validate(new Person(age: 15)).failed()

        and:
        middleNameValidator.validate(new Person(middleName: Optional.of("k"))).passed()
        middleNameValidator.validate(new Person(middleName: Optional.empty())).passed()
        middleNameValidator.validate(new Person(middleName: Optional.of(""))).failed()
    }

    void "Validator allows for multiple validations on a optional field"() {
        when:
        def multiValidator = new Validator<Person>().ifPresent("phone", digits,
            check({ String p -> p.contains("573")}, "Breaks at double check"),
            check({ String p -> p.length() >= 7 }, "Breaks at triple check"),
            check({ String p -> p.length() <= 10}, "Breaks at quadruple check"))

        then:
        multiValidator.validate(new Person(phone: "5731234567")).passed()
        multiValidator.validate(new Person(phone: "5734567")).passed()
        multiValidator.validate(new Person()).passed()
        multiValidator.validate(new Person(phone: "6361234567")).failed()
        multiValidator.validate(new Person(phone: "573")).failed()
        multiValidator.validate(new Person(phone: "abc1234567")).failed()
    }

    void "Validator supports `or` predicates coerced from closures"() {
        when:
        def genderValidator = new Validator<Person>().has("gender",
                or(check({ it == "male" }, "must be a male"), check({ it == "female" }, "must be a female")))

        then:
        genderValidator.validate(new Person(gender: "male")).passed()
        genderValidator.validate(new Person(gender: "female")).passed()
        genderValidator.validate(new Person(gender: "uhhh")).failed()
    }

    void "Validator supports multiple field validation"() {
        when:
        def comprehensiveValidator = new Validator<Person>().has("age", min(16))
                .has("gender", oneOf("male", "female"))
                .ifPresent("email", check({ it.contains("@") }, "must contain a @"))

        and:
        ValidationResult<Person> unknownGender = comprehensiveValidator.validate(new Person(age: 16, gender: "uhhh"))
        ValidationResult<Person> unknownGenderAndTooYoung = comprehensiveValidator.validate(new Person(age: 14, gender: "uhhh"))

        then: // age check
        comprehensiveValidator.validate(new Person(age: 16, gender: "female")).passed()
        comprehensiveValidator.validate(new Person(age: 15, gender: "male")).failed()

        and: // gender check
        unknownGender.failed()
        unknownGender.failures.size() == 1

        and: // dual check
        unknownGenderAndTooYoung.failed()
        unknownGenderAndTooYoung.failures.size() == 2

        and: // email check
        comprehensiveValidator.validate(new Person(age: 16, gender: "male", email: "jon@jon.com")).passed()
        comprehensiveValidator.validate(new Person(age: 16, gender: "male", email: "jon")).failed()
    }

    void "Validator checks supported JSR303 annotations when asked"() {
        when:
        def lastNameValidator = new Validator<Person>().jsr303(Person.class)

        then:
        lastNameValidator.validate(new Person(lastName: "schneider", address: new Address(address1: "someplace"))).passed()
        lastNameValidator.validate(new Person(lastName: "schneider", address: new Address())).failed()
        lastNameValidator.validate(new Person(address: new Address(address1: "someplace"))).failed()
        lastNameValidator.validate(new Person()).failed()
    }

    void "Validation failure messages are always instances of String (not GString)"() {
        given:
        def validator = new Validator<Person>().ifPresent("firstName", oneOf("Steve", "steve", "STEVE"))

        when:
        def validate = validator.validate(new Person(firstName: "kevin"))

        Collection fails = validate.failures[0].messages

        then:
        notThrown(Throwable)
        validate.failed()
        fails.size() == 1
        fails[0].class == String
    }

    void "`and` function accepts closures"(){
        when:
        def validator = new Validator<Address>().and({ Address address -> return address.city == "Cityville" && address.state == "Statesburg" })

        then:
        validator.validate(new Address(address1: "Street Road", city: "Cityville", state: "Statesburg")).passed()
        !validator.validate(new Address(address1: "Boulevard Avenue", city: "Township", state: "SadState")).failed()
    }
}
