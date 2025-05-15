package br.com.bluefocus.teste.service;

import br.com.bluefocus.teste.dto.SoapRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.stream.Stream;

@Service
public class NfeService {

    private static final Logger logger = LoggerFactory.getLogger(NfeService.class);

    public String enviarNfe(SoapRequest request) throws Exception {
        SSLContext sslContext = configureSslContext(request);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build()) {

            HttpPost httpPost = new HttpPost(request.getEndpointUrl());
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(httpPost::addHeader);
            } else {
                logger.warn("Cabeçalhos (headers) da requisição SOAP estão nulos.");
                // Você pode querer lançar uma exceção aqui ou definir cabeçalhos padrão se aplicável
            }

            if (request.getSignedXml() == null || request.getSignedXml().isEmpty()) {
                logger.error("O XML assinado (signedXml) está vazio ou nulo.");
                throw new IllegalArgumentException("O XML assinado (signedXml) não pode ser vazio.");
            }
            httpPost.setEntity(new StringEntity(request.getSignedXml(), StandardCharsets.UTF_8));

            logger.info("Enviando requisição NFe para endpoint: {}", request.getEndpointUrl());

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                int httpStatusCode = response.getStatusLine().getStatusCode();

                logger.info("Resposta recebida da SEFAZ. Status HTTP: {}", httpStatusCode);
                if (logger.isDebugEnabled() && responseBody != null) {
                    logger.debug("Corpo da resposta SEFAZ (truncado): {}", responseBody.substring(0, Math.min(responseBody.length(), 1000)));
                } else if (responseBody == null) {
                    logger.warn("Corpo da resposta da SEFAZ está nulo.");
                }


                if (httpStatusCode >= 200 && httpStatusCode < 300) {
                    return responseBody;
                } else {
                    String errorDetails = responseBody != null ? responseBody.substring(0, Math.min(responseBody.length(), 500)) : "Sem corpo de resposta";
                    logger.error("Erro HTTP ao comunicar com a SEFAZ: {} - Resposta: {}", httpStatusCode, errorDetails);
                    throw new IOException("Falha na comunicação HTTP com a SEFAZ. Status: " + httpStatusCode + ". Resposta: " + errorDetails);
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao enviar NFe: {}", e.getMessage(), e);
            throw e;
        }
    }

    private SSLContext configureSslContext(SoapRequest request) throws Exception {
        char[] passwordChars = null;
        try {
            if (request.getPassword() == null) {
                logger.error("Senha do certificado digital não fornecida.");
                throw new IllegalArgumentException("Senha do certificado digital não fornecida.");
            }
            passwordChars = request.getPassword().toCharArray();

            if (request.getDigitalCertificate() == null) {
                logger.error("Certificado digital (digitalCertificate) não fornecido na requisição.");
                throw new IllegalArgumentException("Certificado digital não fornecido.");
            }
            byte[] certBytes = Base64.getDecoder().decode(request.getDigitalCertificate());
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream keyStoreStream = new ByteArrayInputStream(certBytes)) {
                keyStore.load(keyStoreStream, passwordChars);
            }

            KeyStore trustStore;
            if (request.getTrustCertificatesPath() != null && !request.getTrustCertificatesPath().trim().isEmpty()) {
                trustStore = loadTrustStoreFromDirectory(request.getTrustCertificatesPath());
            } else {
                logger.info("Nenhum trustCertificatesPath fornecido, usando cacerts padrão do Java.");
                trustStore = loadDefaultJavaTrustStore();
            }

            return SSLContexts.custom()
                    .setProtocol("TLSv1.2")
                    .loadKeyMaterial(keyStore, passwordChars)
                    .loadTrustMaterial(trustStore, null)
                    .build();

        } catch (Exception e) {
            logger.error("Falha ao configurar o contexto SSL: {}", e.getMessage(), e);
            throw new Exception("Falha ao configurar o contexto SSL: " + e.getMessage(), e);
        } finally {
            if (passwordChars != null) {
                java.util.Arrays.fill(passwordChars, '\0');
            }
        }
    }

    private KeyStore loadTrustStoreFromDirectory(String trustCertificatesPath) throws Exception {
        logger.info("Carregando certificados da cadeia do diretório: {}", trustCertificatesPath);
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        Path certDirectory = Paths.get(trustCertificatesPath);
        if (!Files.isDirectory(certDirectory)) {
            logger.warn("O caminho fornecido para os certificados da cadeia não é um diretório válido: {}. Tentando carregar cacerts padrão.", trustCertificatesPath);
            return loadDefaultJavaTrustStore();
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        int certsLoaded = 0;
        try (Stream<Path> paths = Files.list(certDirectory)) {
            for (Path filePath : paths.filter(Files::isRegularFile).toList()) {
                try (InputStream certInputStream = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
                    for (Certificate certificate : cf.generateCertificates(certInputStream)) {
                        if (certificate instanceof X509Certificate) {
                            X509Certificate x509Cert = (X509Certificate) certificate;
                            String alias = filePath.getFileName().toString() + "_" + x509Cert.getSerialNumber().toString(16);
                            trustStore.setCertificateEntry(alias, x509Cert);
                            logger.debug("Certificado adicionado ao TrustStore: {} com alias: {} (Subject: {})", filePath.getFileName(), alias, x509Cert.getSubjectX500Principal());
                            certsLoaded++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Falha ao carregar o certificado do arquivo: {} - {}", filePath, e.getMessage());
                }
            }
        }

        if (certsLoaded == 0) {
            logger.warn("Nenhum certificado foi carregado do diretório: {}. Usando cacerts padrão como fallback.", trustCertificatesPath);
            return loadDefaultJavaTrustStore();
        }
        logger.info("{} certificados carregados do diretório para o TrustStore.", certsLoaded);
        return trustStore;
    }

    private KeyStore loadDefaultJavaTrustStore() throws Exception {
        String filename = System.getProperty("java.home") + "/lib/security/cacerts".replace('/', File.separatorChar);
        logger.info("Carregando TrustStore padrão Java de: {}", filename);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "changeit".toCharArray();
        try (FileInputStream is = new FileInputStream(filename)) {
            keystore.load(is, password);
        }
        logger.info("TrustStore padrão 'cacerts' carregado com {} certificados.", keystore.size());
        return keystore;
    }
}
