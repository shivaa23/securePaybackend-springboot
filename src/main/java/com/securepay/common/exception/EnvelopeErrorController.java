package com.securepay.common.exception;

import com.securepay.common.response.ResponseFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Overrides Spring's default {@code /error} page so framework errors (404, 405, 500)
 * return the error envelope rather than the whitelabel JSON (doc 08 §8.4).
 */
@RestController
public class EnvelopeErrorController implements ErrorController {

    private final ResponseFactory responseFactory;

    public EnvelopeErrorController(ResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (statusObj != null) {
            try {
                status = HttpStatus.valueOf(Integer.parseInt(statusObj.toString()));
            } catch (IllegalArgumentException ignored) {
                // keep default 500
            }
        }
        String message = switch (status) {
            case NOT_FOUND -> "Not Found";
            case METHOD_NOT_ALLOWED -> "Method Not Allowed";
            case UNAUTHORIZED -> "Unauthenticated.";
            case FORBIDDEN -> "Forbidden";
            case TOO_MANY_REQUESTS -> "Too Many Attempts.";
            default -> "Something went wrong";
        };
        return responseFactory.error(message, "", status);
    }
}
