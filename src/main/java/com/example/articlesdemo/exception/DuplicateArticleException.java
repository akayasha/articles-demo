package com.example.articlesdemo.exception;

public class DuplicateArticleException extends RuntimeException {

    public DuplicateArticleException(String message) {
        super(message);
    }

    public DuplicateArticleException(String message, Throwable cause) {
        super(message, cause);
    }
}

