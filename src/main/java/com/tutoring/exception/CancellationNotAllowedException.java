package com.tutoring.exception;

public class CancellationNotAllowedException extends RuntimeException{

    public CancellationNotAllowedException(String message) {
        super(message);
    }
}
