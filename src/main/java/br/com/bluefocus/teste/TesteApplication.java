package br.com.bluefocus.teste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TesteApplication {

    public static void main(String[] args) {
        System.setProperty("javax.net.debug", "all");
        SpringApplication.run(TesteApplication.class, args);
    }

}
