package com.nutriplan.pricing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutriplan.pricing.domain.model.PackageSize;
import com.nutriplan.pricing.domain.model.ProductOffer;
import com.nutriplan.pricing.domain.service.CostCalculator;
import com.nutriplan.pricing.domain.service.PackageSizeParser;
import com.nutriplan.pricing.domain.service.ProductMatcher;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProductMatcherTest {

    private static ProductOffer offer(String name, double price) {
        return new ProductOffer("dia", name, "x", price, true, "/p", "un", 1);
    }

    @Test
    void ignoresNoiseAndPrefersGenericCheapProduct() {
        List<ProductOffer> offers = List.of(
                offer("Huevos de Pascua Sorpresa Construcción", 1900),
                offer("Huevos de Gallina Color 6 Un", 2310),
                offer("Huevos de Gallina Blancos 6 Un", 2190),
                offer("Fideos al Huevo Tallarín 500 g", 1200));

        assertThat(ProductMatcher.bestMatch("huevo", offers)).get()
                .extracting(ProductOffer::name).isEqualTo("Huevos de Gallina Blancos 6 Un");
    }

    @Test
    void requiresAllIngredientWords() {
        assertThat(ProductMatcher.bestMatch("leche de coco", List.of(offer("Leche Entera 1 L", 1500)))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"Leche Entera La Serenísima 1 L,1000,ml", "Queso Cremoso x 300gr,300,g",
            "Huevos de Gallina 6 Un,6,unit", "Arroz Largo Fino 1 kg,1000,g"})
    void parsesPackageSizes(String name, double amount, String unit) {
        assertThat(PackageSizeParser.parse(name)).contains(new PackageSize(amount, unit));
    }

    @Test
    void computesPackagesNeeded() {
        assertThat(CostCalculator.packagesNeeded(680.0, "ml", new PackageSize(1000, "ml"))).isEqualTo(1);
        assertThat(CostCalculator.packagesNeeded(14.0, "unit", new PackageSize(6, "unit"))).isEqualTo(3);
        assertThat(CostCalculator.packagesNeeded(200.0, "g", new PackageSize(6, "unit"))).isEqualTo(1);
    }
}
