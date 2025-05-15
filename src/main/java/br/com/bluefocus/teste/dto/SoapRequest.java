// src/main/java/br/com/bluefocus/teste/dto/SoapRequest.java
package br.com.bluefocus.teste.dto;

// import lombok.Data; // Removido para adicionar getters/setters manualmente
import java.util.Map;

// @Data // Removido para adicionar getters/setters manualmente
public class SoapRequest {

    private Map<String, String> headers;
    private String endpointUrl;
    private String trustCertificatesPath;
    private String digitalCertificate;
    private String password;
    private String signedXml;

    // Construtor padrão (necessário se outros construtores forem adicionados)
    public SoapRequest() {
    }

    // Getters
    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getTrustCertificatesPath() {
        return trustCertificatesPath;
    }

    public String getDigitalCertificate() {
        return digitalCertificate;
    }

    public String getPassword() {
        return password;
    }

    public String getSignedXml() {
        return signedXml;
    }

    // Setters
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public void setTrustCertificatesPath(String trustCertificatesPath) {
        this.trustCertificatesPath = trustCertificatesPath;
    }

    public void setDigitalCertificate(String digitalCertificate) {
        this.digitalCertificate = digitalCertificate;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setSignedXml(String signedXml) {
        this.signedXml = signedXml;
    }
}
