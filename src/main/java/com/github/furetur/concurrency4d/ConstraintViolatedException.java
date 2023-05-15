package com.github.furetur.concurrency4d;

public class ConstraintViolatedException extends RuntimeException {
    ConstraintViolatedException(String message) {
        super(message);
    }
}
