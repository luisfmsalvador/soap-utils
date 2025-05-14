package br.com.bluefocus.teste.controller;

import br.com.bluefocus.teste.dto.SoapRequest;
import br.com.bluefocus.teste.dto.SoapResponse;
import br.com.bluefocus.teste.service.NfeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nfe")
public class NfeController {

    private final NfeService nfeService;

    @Autowired
    public NfeController(NfeService nfeService) {
        this.nfeService = nfeService;
    }

    @PostMapping("/enviar")
    public ResponseEntity<SoapResponse> enviarNfe(@RequestBody SoapRequest request) {
        try {
            SoapResponse response = nfeService.enviarNfe(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            SoapResponse errorResponse = new SoapResponse();
            errorResponse.setStatusCode(500);
            errorResponse.setErrorMessage("Erro ao processar a requisição: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
