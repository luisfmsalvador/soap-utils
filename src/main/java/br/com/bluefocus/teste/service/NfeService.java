package br.com.bluefocus.teste.service;

import br.com.bluefocus.teste.dto.SoapRequest;
import br.com.bluefocus.teste.dto.SoapResponse;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Base64;

@Service
public class NfeService {

    public SoapResponse enviarNfe(SoapRequest request) throws Exception {
        // Ativar logs SSL (apenas para debug)
        System.setProperty("javax.net.debug", "ssl:handshake:verbose");

        SSLContext sslContext = configureSslContext(request);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE) // Apenas para testes
                .build()) {

            HttpPost httpPost = new HttpPost(request.getEndpointUrl());

            // Adicionar headers
            request.getHeaders().forEach(httpPost::addHeader);

            // Configurar corpo
            httpPost.setEntity(new StringEntity(request.getSignedXml(), StandardCharsets.UTF_8));

            // Executar com logs detalhados
            System.out.println("=== INÍCIO DA REQUISIÇÃO ===");
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("=== RESPOSTA RECEBIDA ===");
                System.out.println("Status: " + response.getStatusLine());
                System.out.println("Body: " + responseBody.substring(0, Math.min(responseBody.length(), 1000)));

                SoapResponse soapResponse = new SoapResponse();
                soapResponse.setStatusCode(response.getStatusLine().getStatusCode());
                soapResponse.setResponseBody(responseBody);
                return soapResponse;
            }
        }
    }

    private SSLContext configureSslContext(SoapRequest request) throws Exception {
        try {
            // 1. Configurar certificado cliente
            byte[] certBytes = Base64.getDecoder().decode(request.getDigitalCertificate());
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certBytes), request.getPassword().toCharArray());

            // 2. Configurar truststore (certificados da SEFAZ)
            KeyStore trustStore = loadTrustStore(request);

            // 3. Configurar SSLContext com propriedades específicas
            SSLContext sslContext = SSLContexts.custom()
                    .setProtocol("TLSv1.2")
                    .loadKeyMaterial(keyStore, request.getPassword().toCharArray())
                    .loadTrustMaterial(trustStore, null)
                    .build();

            // 4. Configurar propriedades adicionais do sistema
            System.setProperty("https.protocols", "TLSv1.2");
            System.setProperty("jdk.tls.client.protocols", "TLSv1.2");

            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao configurar SSL: " + e.getMessage(), e);
        }
    }

    private KeyStore loadTrustStore(SoapRequest request) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");

        // Se truststore foi fornecido na requisição
        if (request.getTrustStore() != null && !request.getTrustStore().isEmpty()) {
            byte[] tsBytes = Base64.getDecoder().decode(request.getTrustStore());
            trustStore.load(new ByteArrayInputStream(tsBytes), request.getPassword().toCharArray());
        }
        // Usar truststore padrão do Java
        else {
            String defaultTrustStore = System.getProperty("java.home") + "/lib/security/cacerts";
            try (InputStream is = new FileInputStream(defaultTrustStore)) {
                trustStore.load(is, "changeit".toCharArray());
            }
        }
        return trustStore;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SoapResponse> handleAllExceptions(Exception ex) {
        SoapResponse response = new SoapResponse();
        response.setStatusCode(500);

        String errorMsg = "Erro na requisição: ";
        if (ex.getCause() != null) {
            errorMsg += ex.getCause().getMessage();
        } else {
            errorMsg += ex.getMessage();
        }

        // Log detalhado para diagnóstico
        System.err.println("=== ERRO DETALHADO ===");
        ex.printStackTrace();

        response.setErrorMessage(errorMsg);
        return ResponseEntity.internalServerError().body(response);
    }
}