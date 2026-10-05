package com.supportportal.exception;

import com.supportportal.entity.ComplaintStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(ComplaintStatus from, ComplaintStatus to) {
        super("Invalid status transition from " + from + " to " + to);
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
