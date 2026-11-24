package com.nutriplan.planner.infrastructure.client;

import com.nutriplan.planner.domain.port.PricingPort;
import com.nutriplan.planner.domain.service.ShoppingListBuilder.ShoppingItem;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter hacia pricing-service usando <b>Client Credentials</b>: planner-service se autentica
 * con SU propia identidad (service account con rol SERVICE), no con la del usuario. Spring
 * Security obtiene el token de Keycloak, lo cachea y lo renueva al expirar (ClientsConfig).
 *
 * <p><b>Degradación elegante</b>: si pricing-service no responde, la lista de compras se muestra
 * igual, sin precios. Un servicio secundario caído no debe romper la funcionalidad principal.
 */
@Component
public class PricingServiceClient implements PricingPort {

    private static final Logger log = LoggerFactory.getLogger(PricingServiceClient.class);

    private final RestClient http;

    public PricingServiceClient(RestClient pricingRestClient) {
        this.http = pricingRestClient;
    }

    @Override
    public Optional<PriceEstimate> estimate(List<ShoppingItem> items) {
        List<Map<String, Object>> lines = items.stream()
                .filter(i -> !i.toTaste())
                .map(i -> Map.<String, Object>of("name", i.name(), "quantity", i.quantity(), "unit", i.unit()))
                .toList();
        try {
            return Optional.ofNullable(http.post().uri("/internal/prices/estimate")
                    .body(Map.of("items", lines)).retrieve().body(PriceEstimate.class));
        } catch (RestClientException e) {
            log.warn("pricing-service no disponible, la lista sale sin precios: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
