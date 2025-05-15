package br.com.bluefocus.teste.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor; // Removido para ErroDetalhe

import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SoapResponse {

    private String xml;
    private List<ErroDetalhe> erro;

    // Construtor para sucesso
    public SoapResponse(String xml) {
        this.xml = xml;
    }

    // Construtor para erro com uma lista de ErroDetalhe
    public SoapResponse(List<ErroDetalhe> erro) {
        this.erro = erro;
    }

    // Construtor para erro com código, tipo e descrição
    public SoapResponse(String codigoErro, String tipoErro, String descricaoErro) {
        if (descricaoErro != null && !descricaoErro.isEmpty()) {
            this.erro = new ArrayList<>();
            this.erro.add(new ErroDetalhe(codigoErro, tipoErro, descricaoErro));
        }
    }

    @Data
    @NoArgsConstructor
    // @AllArgsConstructor // Removido para usar construtor manual
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErroDetalhe {
        private String codigo;
        private String tipo;
        private String descricao;

        // Construtor manual
        public ErroDetalhe(String codigo, String tipo, String descricao) {
            this.codigo = codigo;
            this.tipo = tipo;
            this.descricao = descricao;
        }
    }
}
