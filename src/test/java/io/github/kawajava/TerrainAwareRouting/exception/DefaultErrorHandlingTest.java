package io.github.kawajava.TerrainAwareRouting.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DefaultErrorHandlingTest {

    private DefaultErrorHandling errorHandling;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        errorHandling = new DefaultErrorHandling();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/path");
    }

    @Test
    void shouldReturnNotFoundErrorDtoForNoSuchElementException() {

        var exception = new NoSuchElementException();

        ResponseEntity<?> response = errorHandling.handleNoSuchElementException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(DefaultErrorDto.class);

        DefaultErrorDto body = (DefaultErrorDto) response.getBody();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("Not Found");
        assertThat(body.getMessage()).isEqualTo("Zasób nie istnieje");
        assertThat(body.getPath()).isEqualTo("/test/path");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void shouldReturnBadRequestErrorDtoForIllegalArgumentException() {

        var exception = new IllegalArgumentException("Invalid data");

        ResponseEntity<?> response = errorHandling.handleIllegalArgument(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(DefaultErrorDto.class);

        DefaultErrorDto body = (DefaultErrorDto) response.getBody();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Bad Request");
        assertThat(body.getMessage()).isEqualTo("Invalid data");
        assertThat(body.getPath()).isEqualTo("/test/path");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void shouldReturnInternalServerErrorDtoForGeneralException() {
        var exception = new Exception("Unexpected");

        ResponseEntity<?> response = errorHandling.handleGeneral(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(DefaultErrorDto.class);

        DefaultErrorDto body = (DefaultErrorDto) response.getBody();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getError()).isEqualTo("Internal Server Error");
        assertThat(body.getMessage()).isEqualTo("Wewnętrzny błąd serwera");
        assertThat(body.getPath()).isEqualTo("/test/path");
        assertThat(body.getTimestamp()).isNotNull();
    }
}
