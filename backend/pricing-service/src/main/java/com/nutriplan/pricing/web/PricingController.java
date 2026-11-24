package com.nutriplan.pricing.web;

import com.nutriplan.pricing.application.dto.EstimateRequest;
import com.nutriplan.pricing.application.dto.PriceEstimate;
import com.nutriplan.pricing.application.dto.PricePointDto;
import com.nutriplan.pricing.application.dto.ProductPriceDto;
import com.nutriplan.pricing.application.dto.RefreshReport;
import com.nutriplan.pricing.application.usecase.PriceEstimationService;
import com.nutriplan.pricing.application.usecase.PriceRefreshService;
import com.nutriplan.pricing.domain.port.SupermarketCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Precios", description = "Precios de supermercados argentinos (API VTEX pública)")
public class PricingController {

    private final Function<EstimateRequest, PriceEstimate> estimatePrices;
    private final PriceEstimationService prices;
    private final PriceRefreshService refresher;
    private final List<SupermarketCatalog> catalogs;

    public PricingController(Function<EstimateRequest, PriceEstimate> estimatePrices, PriceEstimationService prices,
                             PriceRefreshService refresher, List<SupermarketCatalog> catalogs) {
        this.estimatePrices = estimatePrices;
        this.prices = prices;
        this.refresher = refresher;
        this.catalogs = catalogs;
    }

    @PostMapping("/internal/prices/estimate")
    @Operation(summary = "[SERVICE] Estima el costo de una lista de compras (lo llama planner-service)")
    public PriceEstimate estimate(@RequestBody @Valid EstimateRequest request) {
        return estimatePrices.apply(request);
    }

    @GetMapping("/api/prices/chains")
    @Operation(summary = "Supermercados configurados")
    public List<Map<String, String>> chains() {
        return catalogs.stream().map(c -> Map.of("id", c.chainId(), "name", c.displayName())).toList();
    }

    @GetMapping("/api/prices/products")
    @Operation(summary = "Precio actual de un ingrediente en cada cadena")
    public List<ProductPriceDto> products(@RequestParam @NotBlank String ingredient) {
        return prices.pricesFor(ingredient.toLowerCase());
    }

    @GetMapping("/api/prices/history")
    @Operation(summary = "Historial semanal de precios de un ingrediente")
    public List<PricePointDto> history(@RequestParam @NotBlank String ingredient) {
        return prices.history(ingredient.toLowerCase());
    }

    @PostMapping("/api/admin/prices/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Ejecuta ahora la actualización de precios (asíncrona)")
    public ResponseEntity<Map<String, Object>> refresh(@RequestParam(required = false) Integer limit) {
        CompletableFuture<RefreshReport> unused = CompletableFuture.supplyAsync(() -> refresher.refreshAll(limit));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("started", true));
    }
}
