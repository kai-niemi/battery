package io.battery.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by {@link Money} arithmetic and comparisons on amounts in different currencies. Maps to
 * an HTTP 400 response when thrown while handling a web request.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class CurrencyMismatchException extends IllegalArgumentException {
    public CurrencyMismatchException(String s) {
        super(s);
    }
}
