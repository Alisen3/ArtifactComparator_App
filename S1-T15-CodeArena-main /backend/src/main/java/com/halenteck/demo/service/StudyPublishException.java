package com.halenteck.demo.service;

import java.util.List;

public class StudyPublishException extends RuntimeException {

    private final List<String> errors;

    public StudyPublishException(List<String> errors) {
        super("Study cannot be published");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}


