package br.com.bluefocus.teste.exception;

import br.com.bluefocus.teste.dto.SoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
// Removido o import de Collectors, pois a lógica foi simplificada
// import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger_handler = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Este método só será chamado se você adicionar a dependência spring-boot-starter-validation
    // e usar @Valid no seu controller. Por enquanto, ele não será ativado.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        logger_handler.warn("Erro de validação nos argumentos da requisição (MethodArgumentNotValidException): {}", ex.getMessage());

        List<SoapResponse.ErroDetalhe> detalhes = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalhes.add(new SoapResponse.ErroDetalhe(
                    "VALIDACAO_" + error.getField().toUpperCase(),
                    "ERRO_VALIDACAO_CAMPO",
                    error.getDefaultMessage() + " (campo: " + error.getField() + ")"));
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            detalhes.add(new SoapResponse.ErroDetalhe(
                    "VALIDACAO_GERAL",
                    "ERRO_VALIDACAO_OBJETO",
                    error.getDefaultMessage()));
        }

        SoapResponse errorResponse = new SoapResponse(detalhes);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<SoapResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logger_handler.error("Exceção Runtime não tratada: {}", ex.getMessage(), ex);

        SoapResponse.ErroDetalhe erroDetalhe = new SoapResponse.ErroDetalhe(
                "ERRO_INESPERADO",
                ex.getClass().getSimpleName(),
                "Ocorreu um erro inesperado no servidor. Por favor, contate o suporte se o problema persistir."
        );
        SoapResponse errorResponse = new SoapResponse(List.of(erroDetalhe));
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // IOException já é tratado no NfeController, mas podemos ter um aqui como fallback
    // ou para IOExceptions de outras partes da aplicação.
    @ExceptionHandler(IOException.class)
    public ResponseEntity<SoapResponse> handleIOException(IOException ex, WebRequest request) {
        logger_handler.error("Erro de I/O não tratado pelo controller: {}", ex.getMessage(), ex);

        SoapResponse.ErroDetalhe erroDetalhe = new SoapResponse.ErroDetalhe(
                "ERRO_IO_GERAL",
                "FALHA_IO",
                "Falha de comunicação ou I/O: " + ex.getMessage()
        );
        SoapResponse errorResponse = new SoapResponse(List.of(erroDetalhe));
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SoapResponse> handleAllOtherExceptions(Exception ex, WebRequest request) {
        if (!(ex instanceof IOException || ex instanceof RuntimeException)) {
            logger_handler.error("Exceção geral não tratada: {}", ex.getMessage(), ex);
        } // Evita log duplo se já foi logado por RuntimeException ou IOException handler

        SoapResponse.ErroDetalhe erroDetalhe = new SoapResponse.ErroDetalhe(
                "ERRO_SERVIDOR_GENERICO",
                ex.getClass().getSimpleName(),
                "Ocorreu um erro geral no servidor."
        );
        SoapResponse errorResponse = new SoapResponse(List.of(erroDetalhe));
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
