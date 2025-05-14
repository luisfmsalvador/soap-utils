package br.com.bluefocus.teste.dto;

import lombok.Data;

@Data
public class SoapResponse {
    private int statusCode;
    private String responseBody;
    private String errorMessage;
}