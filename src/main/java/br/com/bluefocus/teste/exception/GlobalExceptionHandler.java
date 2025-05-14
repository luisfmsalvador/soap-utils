package br.com.bluefocus.teste.exception;

import br.com.bluefocus.teste.dto.SoapResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<SoapResponse> handleRuntimeException(RuntimeException ex) {
        SoapResponse response = new SoapResponse();
        response.setStatusCode(500);

        String errorMsg = ex.getMessage();
        if (ex.getCause() != null) {
            errorMsg += " Causa: " + ex.getCause().getMessage();
        }

        response.setErrorMessage(errorMsg);
        return ResponseEntity.internalServerError().body(response);
    }
}