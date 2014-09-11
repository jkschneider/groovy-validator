groovy-validator
================

A flexible validation framework that cooperates with JSR-303, inspired by the Play! Reads API

Motivation
----------

* The desire to define field-level validation by composing sets of simpler validation methods in a way that is independent of the class itself.

* Validation should continue until all rules have been tested rather than stopping at the first failure.

* Separate validation from deserialization.  When they are tied together, as in a combination of JSR-303 and Jackson, Jackson stops with an exception at the first validation failure, which does not allow a comprehensive list of failures to be discovered.

