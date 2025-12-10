package io.github.kawajava.TerrainAwareRouting.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.NoSuchElementException;

@ControllerAdvice
public class DefaultErrorHandling {

    @ExceptionHandler({NoSuchElementException.class})
    @ResponseBody
    public ResponseEntity<?> handleNoSuchElementException(NoSuchElementException e, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new DefaultErrorDto(
                        new Date(),
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        "Zasób nie istnieje",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseBody
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new DefaultErrorDto(
                        new Date(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler({Exception.class})
    @ResponseBody
    public ResponseEntity<?> handleGeneral(Exception e, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DefaultErrorDto(
                        new Date(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "Wewnętrzny błąd serwera",
                        request.getRequestURI()
                ));
    }
}
