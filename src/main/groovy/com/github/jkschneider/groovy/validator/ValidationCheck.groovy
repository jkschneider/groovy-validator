package com.github.jkschneider.groovy.validator

import groovy.transform.ToString
import org.joda.time.LocalDate

import java.util.regex.Pattern

@ToString
class ValidationCheck {
    Closure closure
    String field
    String failureMessage

    ValidationCheck againstField(String field) { new ValidationCheck(closure: closure, field:field, failureMessage:failureMessage) }

    synchronized boolean check(obj) { closure(field ? fieldVal(field, obj) : obj) }

    protected static def fieldVal(field, obj) {
        def val = field.split(/\./).inject(obj) { acc, path -> acc?."$path" }
        if(val instanceof Optional) {
            def optVal = (Optional) val
            if(optVal.present) return optVal.get()
            else return null
        }
        return val
    }

    static ValidationCheck check(Closure c, String failureMessage) {
        new ValidationCheck(closure: c, failureMessage: failureMessage)
    }

    /**
     * If validation fails, does not add to validation message output
     */
    static ValidationCheck checkQuietly(Closure c) { new ValidationCheck(closure: c) }

    static ValidationCheck or(ValidationCheck... checks) {
        new ValidationCheck(
           // once acc becomes true, we skip the rest of the closure calls (to prevent NPEs and the like)
           closure: { obj -> checks.inject(false) { acc, check -> acc || (acc ?: check.closure(obj)) } },
           failureMessage: ["must meet one of the following: ${checks*.failureMessage}"]
        )
    }

    void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage
    }

    static ValidationCheck and(ValidationCheck... checks) {
        new ValidationCheck(
                // once acc is false
                closure: { obj -> checks.inject(true) { acc, check -> (!acc ? acc : check.closure(obj) && acc )} },
                failureMessage: ["must meet one of the following: ${checks*.failureMessage}"]
        )
    }

    static ValidationCheck after(LocalDate d) { check({ LocalDate obj -> obj.isAfter(d)}, "must be after $d") }

    static ValidationCheck before(LocalDate d) { check({ LocalDate obj -> obj.isBefore(d) }, "must be before $d") }

    static ValidationCheck min(Number n) { check({ obj -> obj >= n }, "must be greater than or equal to $n") }

    static ValidationCheck max(Number n) { check({ obj -> obj <= n }, "must be less than or equal to $n") }

    static ValidationCheck oneOf(Object... possibles) { check({ obj -> possibles.contains(obj)}, "must match one of $possibles") }

    static ValidationCheck pattern(Pattern pattern) { check({ obj -> pattern.matcher(obj).matches() }, "must match pattern")  }

    static ValidationCheck nonEmpty = check({ obj -> !obj.isEmpty() }, "cannot be empty")

    static ValidationCheck email = check({obj -> obj.matches(/(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/) }, "must be a valid email address")

    static ValidationCheck equalTo(Object o) { check({obj -> obj.equals(o) }, "must be equal to $o") }

    /**
     * Tests that an object or field is within a given range, i.e. [min,max]
     */
    static ValidationCheck range(Number min, Number max) { check({ obj -> obj >= min && obj <= max }, "must be within range [$min,$max]") }

    static ValidationCheck dateRange(LocalDate min, LocalDate max) { check({ obj -> obj.isAfter(min) && obj.isBefore(max) }, "must be within range [$min,$max]") }

    static ValidationCheck minLength(Number n) { check({ obj -> obj.length() >= n }, "length must be greater than or equal to $n") }

    static ValidationCheck maxLength(Number n) { check({ obj -> obj.length() <= n }, "length must be less than or equal to $n") }

    static ValidationCheck length(Number n) { check({ obj -> obj.length() == n }, "length must be equal to $n") }

    static ValidationCheck digits = check({ obj -> obj.matches(/[0-9]+/) }, "must be all digits")

    static ValidationCheck alphaNumeric = check({ obj -> obj.matches(/(?i)[A-Z0-9]+/) }, "must be alphanumeric")
}