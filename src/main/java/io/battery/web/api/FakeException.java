package io.battery.web.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.EXPECTATION_FAILED)
public class FakeException extends RuntimeException {
    public FakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
