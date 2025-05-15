package br.com.bluefocus.teste.controller;

import br.com.bluefocus.teste.dto.SoapRequest;
import br.com.bluefocus.teste.dto.SoapResponse;
import br.com.bluefocus.teste.service.NfeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/nfe")
public class NfeController {

    private static final Logger logger = LoggerFactory.getLogger(NfeController.class);
    private final NfeService nfeService;

    @Autowired
    public NfeController(NfeService nfeService) {
        this.nfeService = nfeService;
    }

    @PostMapping(value = "/enviar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SoapResponse> enviarNfe(@RequestBody SoapRequest request) {
        try {
            logger.info("Recebida requisição para /api/nfe/enviar");
            // Validação básica manual, já que removemos as anotações de validação por enquanto
            if (request.getEndpointUrl() == null || request.getEndpointUrl().isEmpty()) {
                throw new IllegalArgumentException("A URL do endpoint (endpointUrl) não pode ser vazia.");
            }
            if (request.getDigitalCertificate() == null || request.getDigitalCertificate().isEmpty()) {
                throw new IllegalArgumentException("O certificado digital (digitalCertificate) não pode ser vazio.");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new IllegalArgumentException("A senha do certificado (password) não pode ser vazia.");
            }
            if (request.getSignedXml() == null || request.getSignedXml().isEmpty()) {
                throw new IllegalArgumentException("O XML assinado (signedXml) não pode ser vazio.");
            }


            String xmlRespostaSefaz = nfeService.enviarNfe(request);
            SoapResponse successResponse = new SoapResponse(xmlRespostaSefaz);
            logger.info("Requisição NFe processada com sucesso (comunicação com SEFAZ OK).");
            return ResponseEntity.ok(successResponse);

        } catch (IllegalArgumentException e) {
            logger.warn("Erro de argumento inválido na requisição NFe: {}", e.getMessage());
            SoapResponse.ErroDetalhe erro = new SoapResponse.ErroDetalhe("DADOS_INVALIDOS", "VALIDACAO_ENTRADA", e.getMessage());
            SoapResponse errorResponse = new SoapResponse(List.of(erro));
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IOException e) {
            logger.error("Erro de comunicação ao processar NFe: {}", e.getMessage());
            SoapResponse.ErroDetalhe erro = new SoapResponse.ErroDetalhe(
                    "ERRO_COMUNICACAO_SEFAZ",
                    "FALHA_IO",
                    "Erro de comunicação com o serviço externo: " + e.getMessage()
            );
            SoapResponse errorResponse = new SoapResponse(List.of(erro));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
        catch (Exception e) {
            logger.error("Erro crítico ao processar requisição NFe: {}", e.getMessage(), e);
            SoapResponse.ErroDetalhe erro = new SoapResponse.ErroDetalhe(
                    "ERRO_INTERNO_API",
                    e.getClass().getSimpleName(),
                    "Ocorreu um erro interno na API ao processar sua solicitação."
            );
            SoapResponse errorResponse = new SoapResponse(List.of(erro));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
