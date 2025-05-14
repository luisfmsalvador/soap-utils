package br.com.bluefocus.teste.dto;

import lombok.Data;
import java.util.Map;

@Data
public class SoapRequest {
    private Map<String, String> headers;  // Coleção de headers
    private String endpointUrl;           // URL do endpoint SOAP
    private String trustStore;            // Arquivo truststore (base64)
    private String digitalCertificate;     // Certificado digital (base64)
    private String password;              // Senha do certificado
    private String signedXml;              // XML assinado
}