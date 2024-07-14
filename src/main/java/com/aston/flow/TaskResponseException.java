package com.aston.flow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

@JsonIgnoreType
@JsonIgnoreProperties({"stackTrace", "cause", "localizedMessage", "suppressed"})
public class TaskResponseException extends RuntimeException {

    public TaskResponseException(String message) {
        super("response error "+message);
    }

    public String getErrorType() {
        return TaskResponseException.class.getSimpleName();
    }
}
