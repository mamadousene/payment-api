package sn.com.developer.paymentapi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.zalando.problem.AbstractThrowableProblem;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler gère les exceptions globalement pour l'application.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@RestControllerAdvice
@Slf4j
public class CustomGlobalExceptionHandler {

    /**
     * Gère MethodArgumentNotValidException levé lorsque la validation des arguments de méthode échoue.
     * @param ex L'instance de MethodArgumentNotValidException.
     * @param webRequest L'instance de WebRequest.
     * @return ResponseEntity avec une réponse d'erreur.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest webRequest) {
        List<String> messages = ex
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        ErrorResponse errorResponse = _getErrorResponse(webRequest, HttpStatus.BAD_REQUEST, messages);

        return new ResponseEntity<>(errorResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }


    /**
     * Gère ResponseStatusException levé lorsqu'une erreur avec un statut HTTP spécifique est nécessaire.
     * @param ex L'instance de ResponseStatusException.
     * @param webRequest L'instance de WebRequest.
     * @return ResponseEntity avec une réponse d'erreur.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex, WebRequest webRequest) {
        List<String> messages = Collections.singletonList(ex.getReason());
        ErrorResponse errorResponse = _getErrorResponse(webRequest, HttpStatus.valueOf(ex.getStatusCode().value()), messages);

        return new ResponseEntity<>(errorResponse, new HttpHeaders(),HttpStatus.valueOf(ex.getMessage()));
    }

    /**
     * Gère BadRequestAlertException lordqu'elle est levée
     * @param ex L'instance de BadRequestAlertException.
     * @param webRequest L'instance de WebRequest.
     * @return ResponseEntity avec une réponse d'erreur.
     */
    @ExceptionHandler(BadRequestAlertException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestAlertException(BadRequestAlertException ex, WebRequest webRequest) {
        List<String> messages = Collections.singletonList(ex.getMessage());
        ErrorResponse errorResponse = _getErrorResponse(webRequest, HttpStatus.BAD_REQUEST, messages);

        return new ResponseEntity<>(errorResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Gère AbstractThrowableProble lordqu'elle est levée
     * @param ex L'instance de AbstractThrowableProblem.
     * @param webRequest L'instance de WebRequest.
     * @return ResponseEntity avec une réponse d'erreur.
     */
    @ExceptionHandler(AbstractThrowableProblem.class)
    public ResponseEntity<ErrorResponse> NestedServletException(AbstractThrowableProblem ex, WebRequest webRequest) {
        List<String> messages = Collections.singletonList(ex.getMessage());
        ErrorResponse errorResponse = _getErrorResponse(webRequest, HttpStatus.BAD_REQUEST, messages);

        return new ResponseEntity<>(errorResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Gère AccessDeniedException lordqu'elle est levée
     * @param ex L'instance de AccessDeniedException.
     * @param webRequest L'instance de WebRequest.
     * @return ResponseEntity avec une réponse d'erreur.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest webRequest) {
        List<String> messages = Collections.singletonList(ex.getMessage());
        ErrorResponse errorResponse = _getErrorResponse(webRequest, HttpStatus.FORBIDDEN, messages);

        return new ResponseEntity<>(errorResponse, new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    /**
     * Gère l'Exception générique.
     * @param ex L'instance de Exception.
     * @param webRequest L'instance de WebRequest.
     * @return ResponseEntity avec une réponse d'erreur.
     */
    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest webRequest) {
        List<String> messages = Collections.singletonList("Une erreur est survenue");
        log.info("Exception : {}", ex.getMessage());
        ErrorResponse errorResponse = _getErrorResponse(webRequest, HttpStatus.INTERNAL_SERVER_ERROR, messages);

        return new ResponseEntity<>(errorResponse, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorResponse _getErrorResponse(
            WebRequest webRequest,
            HttpStatus httpStatus,
            List<String> messages
    ) {
        String path = webRequest.getDescription(false).substring(webRequest.getDescription(false).indexOf("=") + 1);
        return ErrorResponse
                .builder()
                .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                .path(path)
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .messages(messages)
                .build();
    }
}
