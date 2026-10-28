package dev.portfolio.ratings.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ProblemDetail domain(ApiException ex) {
    return ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail conflict() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "A review already exists for this user and product, or the operation violates a data constraint.");
  }

  @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
  ProblemDetail invalid(Exception ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Invalid parameter. Check UUIDs, page and size limits.");
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      org.springframework.web.context.request.WebRequest request) {
    var problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problem.setProperty(
        "errors",
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList());
    return ResponseEntity.badRequest().body(problem);
  }
}
