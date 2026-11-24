package com.nutriplan.pricing.infrastructure.vtex;

import com.nutriplan.pricing.domain.model.ProductOffer;
import com.nutriplan.pricing.domain.port.SupermarketCatalog;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter para supermercados que usan la plataforma VTEX (Día, Jumbo, Carrefour, Vea, Disco).
 * Todos exponen la misma API pública de catálogo:
 * {@code GET https://<host>/api/catalog_system/pub/products/search?ft=<término>&_from=0&_to=14}
 *
 * <p>Una instancia por cadena (configuradas en application.yml). Cada una con su propio
 * <b>Circuit Breaker</b> (Resilience4j): si una cadena falla repetidamente, el circuito se "abre"
 * y dejamos de llamarla un rato => no castigamos a un servicio caído ni bloqueamos el job.
 */
public class VtexCatalogAdapter implements SupermarketCatalog {

    private static final Logger log = LoggerFactory.getLogger(VtexCatalogAdapter.class);

    private final String chainId;
    private final String displayName;
    private final String host;
    private final RestClient http;
    private final CircuitBreaker breaker;

    public VtexCatalogAdapter(String chainId, String displayName, String host, RestClient http, CircuitBreaker breaker) {
        this.chainId = chainId;
        this.displayName = displayName;
        this.host = host;
        this.http = http;
        this.breaker = breaker;
    }

    @Override
    public String chainId() {
        return chainId;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public List<ProductOffer> search(String term, int limit) {
        try {
            return breaker.executeSupplier(() -> doSearch(term, limit));
        } catch (CallNotPermittedException e) {
            log.debug("Circuito abierto para {}: se omite la búsqueda de '{}'", chainId, term);
            return List.of();
        } catch (RestClientException e) {
            log.debug("Error consultando {}: {}", chainId, e.getMessage());
            return List.of();
        }
    }

    private List<ProductOffer> doSearch(String term, int limit) {
        List<VtexProduct> products = http.get()
                .uri("https://{host}/api/catalog_system/pub/products/search?ft={term}&_from=0&_to={to}",
                        Map.of("host", host, "term", term, "to", Math.max(0, limit - 1)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        if (products == null) {
            return List.of();
        }
        return products.stream()
                .filter(p -> p.items() != null && !p.items().isEmpty())
                .map(this::toOffer)
                .filter(o -> o != null)
                .toList();
    }

    private ProductOffer toOffer(VtexProduct p) {
        VtexProduct.Item item = p.items().getFirst();
        if (item.sellers() == null || item.sellers().isEmpty()) {
            return null;
        }
        VtexProduct.Offer offer = item.sellers().getFirst().commertialOffer();
        if (offer == null) {
            return null;
        }
        return new ProductOffer(chainId, p.productName(), p.brand(), offer.price(), offer.isAvailable(),
                p.link(), item.measurementUnit(), item.unitMultiplier() == null ? 1 : item.unitMultiplier());
    }

    /** Subconjunto del JSON de VTEX (Jackson ignora el resto de los campos). */
    record VtexProduct(String productName, String brand, String link, List<Item> items) {
        record Item(String ean, String measurementUnit, Double unitMultiplier, List<Seller> sellers) {
        }

        record Seller(Offer commertialOffer) {
        }

        record Offer(@com.fasterxml.jackson.annotation.JsonProperty("Price") double price,
                     @com.fasterxml.jackson.annotation.JsonProperty("ListPrice") double listPrice,
                     @com.fasterxml.jackson.annotation.JsonProperty("IsAvailable") boolean isAvailable) {
        }
    }
}
