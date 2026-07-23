package com.amol.microservices.gateway.exception;

import com.amol.microservices.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps exceptions to uniform {@link ErrorResponse} bodies. Client errors are logged at WARN with a
 * safe reason; unexpected errors are logged at ERROR and never leak internal detail to the caller.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Invalid request");
        log.warn("request_failed reason=validation detail={}", detail);
        return ResponseEntity.badRequest().body(new ErrorResponse("bad_request", detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("request_failed reason=unreadable_body");
        return ResponseEntity.badRequest().body(new ErrorResponse("bad_request", "Malformed request body"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("request_failed reason=bad_request message={}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse("bad_request", ex.getMessage()));
    }

    @ExceptionHandler({InvalidCredentialsException.class, MfaRequiredException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex) {
        String code = ex instanceof MfaRequiredException ? "mfa_required" : "unauthorized";
        log.warn("request_failed reason={}", code);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(code, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("request_failed reason=internal_error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("internal_error", "An unexpected error occurred"));
    }
}
