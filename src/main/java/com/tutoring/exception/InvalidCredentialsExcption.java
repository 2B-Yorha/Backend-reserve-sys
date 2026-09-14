package com.tutoring.exception;

public class InvalidCredentialsExcption extends RuntimeException{
    public InvalidCredentialsExcption(){
        super("Invalid email or password");
    }

}
