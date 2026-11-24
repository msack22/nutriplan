package com.nutriplan.pricing.application.usecase;

import com.nutriplan.pricing.application.dto.EstimateRequest;
import com.nutriplan.pricing.application.dto.PriceEstimate;
import com.nutriplan.pricing.application.dto.PriceEstimate.ChainTotal;
import com.nutriplan.pricing.application.dto.PriceEstimate.ItemPrice;
import com.nutriplan.pricing.application.dto.PricePointDto;
import com.nutriplan.pricing.application.dto.ProductPriceDto;
import com.nutriplan.pricing.domain.model.ProductPrice;
import com.nutriplan.pricing.domain.port.SupermarketCatalog;
import com.nutriplan.pricing.domain.repository.PriceHistoryRepository;
import com.nutriplan.pricing.domain.repository.ProductPriceRepository;
import com.nutriplan.pricing.domain.service.CostCalculator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estima cuánto cuesta la lista de compras en cada cadena usando los precios guardados.
 * Si a un ingrediente le falta precio, se busca EN VIVO (máximo N por pedido para acotar la
 * latencia) y se empieza a seguir para el próximo lunes.
 */
@Service
public class PriceEstimationService {

    private final ProductPriceRepository prices;
    private final PriceHistoryRepository history;
    private final List<SupermarketCatalog> catalogs;
    private final PriceRefreshService refresher;
    private final IngredientTrackingService tracking;
    private final int maxLiveLookups;

    public PriceEstimationService(ProductPriceRepository prices, PriceHistoryRepository history,
                                  List<SupermarketCatalog> catalogs, PriceRefreshService refresher,
                                  IngredientTrackingService tracking,
                                  @Value("${pricing.estimate.max-live-lookups:10}") int maxLiveLookups) {
        this.prices = prices;
        this.history = history;
        this.catalogs = catalogs;
        this.refresher = refresher;
        this.tracking = tracking;
        this.maxLiveLookups = maxLiveLookups;
    }

    public PriceEstimate estimate(EstimateRequest request) {
        List<EstimateRequest.Item> items = request.items() == null ? List.of() : request.items();
        Map<String, Map<String, ProductPrice>> byIngredient = new HashMap<>();
        prices.findByIngredientIn(items.stream().map(EstimateRequest.Item::name).toList())
                .forEach(p -> byIngredient.computeIfAbsent(p.getIngredient(), k -> new HashMap<>()).put(p.getChain(), p));

        int liveLookups = 0;
        List<ItemPrice> lines = new ArrayList<>();
        Map<String, double[]> totals = new HashMap<>();   // chain -> [total, priced]
        for (EstimateRequest.Item item : items) {
            Map<String, ProductPrice> perChain = byIngredient.computeIfAbsent(item.name(), k -> new HashMap<>());
            if (perChain.isEmpty() && liveLookups < maxLiveLookups) {
                liveLookups++;
                tracking.track(List.of(item.name()));
                for (SupermarketCatalog c : catalogs) {
                    refresher.lookupAndStore(c, item.name()).ifPresent(p -> perChain.put(c.chainId(), p));
                }
            }
            for (ProductPrice p : perChain.values()) {
                int packages = CostCalculator.packagesNeeded(item.quantity(), item.unit(), p.packageSize());
                double subtotal = Math.round(p.getPrice() * packages * 100) / 100.0;
                lines.add(new ItemPrice(item.name(), p.getChain(), p.getProductName(), p.getPrice(), packages,
                        subtotal, p.getProductUrl()));
                double[] t = totals.computeIfAbsent(p.getChain(), k -> new double[2]);
                t[0] += subtotal;
                t[1]++;
            }
        }
        List<ChainTotal> chainTotals = catalogs.stream()
                .map(c -> {
                    double[] t = totals.getOrDefault(c.chainId(), new double[2]);
                    return new ChainTotal(c.chainId(), Math.round(t[0] * 100) / 100.0, (int) t[1],
                            items.size() - (int) t[1]);
                })
                .sorted(Comparator.comparingInt(ChainTotal::itemsMissing).thenComparingDouble(ChainTotal::total))
                .toList();
        return new PriceEstimate(chainTotals, lines, prices.lastUpdate().orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ProductPriceDto> pricesFor(String ingredient) {
        return prices.findByIngredientOrderByPriceAsc(ingredient).stream()
                .map(p -> new ProductPriceDto(p.getChain(), p.getIngredient(), p.getProductName(), p.getBrand(),
                        p.getPrice(), p.getProductUrl(), p.getFetchedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PricePointDto> history(String ingredient) {
        return history.findByIngredientOrderByDateAsc(ingredient).stream()
                .map(h -> new PricePointDto(h.getChain(), h.getDate(), h.getPrice(), h.getProductName()))
                .toList();
    }

    public Optional<SupermarketCatalog> catalog(String chain) {
        return catalogs.stream().filter(c -> c.chainId().equals(chain)).findFirst();
    }
}
