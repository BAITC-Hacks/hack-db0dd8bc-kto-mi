package com.qadam.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.qadam.dto.ErrorResponse;
import com.qadam.dto.FieldErrorResponse;
import com.qadam.service.InvalidStateException;
import com.qadam.service.LlmFailureException;
import com.qadam.service.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns every API error into an {@link ErrorResponse} with a message in Russian for the user.
 * Standard Spring MVC errors (unsupported method, unreadable JSON, ...) are handled by the base class
 * and rendered in the same format through {@link #handleExceptionInternal}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String VALIDATION_MESSAGE = "Проверьте правильность заполнения полей";
    static final String LLM_FAILURE_MESSAGE =
            "Не удалось обработать задачу с помощью ИИ. Попробуйте ещё раз чуть позже";
    static final String INTERNAL_ERROR_MESSAGE = "Внутренняя ошибка сервера. Попробуйте ещё раз позже";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getUserMessage());
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidStateException e) {
        return error(HttpStatus.CONFLICT, e.getUserMessage());
    }

    @ExceptionHandler(LlmFailureException.class)
    public ResponseEntity<ErrorResponse> handleLlmFailure(LlmFailureException e) {
        log.error("AI processing failed: {}", e.getMessage(), e);
        return error(HttpStatus.BAD_GATEWAY, LLM_FAILURE_MESSAGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> fieldErrors = e.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldErrorResponse(
                        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                        error.getDefaultMessage()))
                .toList();
        return body(HttpStatus.BAD_REQUEST, VALIDATION_MESSAGE, fieldErrors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (e.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            FieldErrorResponse fieldError = new FieldErrorResponse(fieldPath(mapping), invalidValueMessage(mapping));
            return body(HttpStatus.BAD_REQUEST, VALIDATION_MESSAGE, List.of(fieldError));
        }
        if (e.getCause() instanceof JsonProcessingException) {
            return body(HttpStatus.BAD_REQUEST, "Тело запроса не является корректным JSON", List.of());
        }
        return body(HttpStatus.BAD_REQUEST, "Тело запроса отсутствует или не может быть прочитано", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = e instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : e.getPropertyName();
        FieldErrorResponse fieldError = new FieldErrorResponse(field, "Недопустимое значение: " + e.getValue());
        return body(HttpStatus.BAD_REQUEST, "Некорректный параметр запроса", List.of(fieldError));
    }

    /**
     * Renders the remaining standard Spring MVC errors in the unified format.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (status.is5xxServerError()) {
            log.error("Request failed", e);
        }
        return ResponseEntity.status(status)
                .headers(headers)
                .body(errorResponse(status, standardMessage(status), List.of()));
    }

    private static String standardMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Некорректный запрос";
            case NOT_FOUND -> "Запрошенный адрес не найден";
            case METHOD_NOT_ALLOWED -> "Этот HTTP-метод не поддерживается для данного адреса";
            case NOT_ACCEPTABLE -> "Сервер не может вернуть ответ в запрошенном формате";
            case UNSUPPORTED_MEDIA_TYPE -> "Неподдерживаемый формат запроса: отправляйте данные в формате JSON";
            case CONFLICT -> "Действие недоступно в текущем состоянии";
            case PAYLOAD_TOO_LARGE -> "Запрос слишком большой";
            default -> status.is5xxServerError() ? INTERNAL_ERROR_MESSAGE : "Не удалось выполнить запрос";
        };
    }

    /**
     * Builds a path like {@code answers[0].field} from the location of a JSON mapping error.
     */
    private static String fieldPath(JsonMappingException e) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference reference : e.getPath()) {
            if (reference.getFieldName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static String invalidValueMessage(JsonMappingException e) {
        if (e instanceof InvalidFormatException format && format.getTargetType() != null
                && format.getTargetType().isEnum()) {
            String allowed = Arrays.stream(format.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "Недопустимое значение «" + format.getValue() + "». Допустимые значения: " + allowed;
        }
        return "Недопустимое значение или неверный тип поля";
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(errorResponse(status, message, List.of()));
    }

    private static ResponseEntity<Object> body(HttpStatus status, String message, List<FieldErrorResponse> fieldErrors) {
        return ResponseEntity.status(status).body(errorResponse(status, message, fieldErrors));
    }

    private static ErrorResponse errorResponse(HttpStatus status, String message, List<FieldErrorResponse> fieldErrors) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, Instant.now(), fieldErrors);
    }
}
