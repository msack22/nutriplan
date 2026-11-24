package com.nutriplan.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway + BFF (Backend For Frontend). Único punto de entrada para el navegador.
 * Ver docs/03-seguridad.md (sección BFF).
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
