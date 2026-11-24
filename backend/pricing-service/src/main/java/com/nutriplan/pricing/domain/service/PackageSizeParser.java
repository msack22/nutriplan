package com.nutriplan.pricing.domain.service;

import com.nutriplan.pricing.domain.model.PackageSize;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae el tamaño del envase desde el nombre del producto:
 * "Leche Entera La Serenísima 1 L" -> 1000 ml, "Huevos x 6 Un" -> 6 unit, "Queso 300gr" -> 300 g.
 */
public final class PackageSizeParser {

    private static final Pattern SIZE = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(kg|kilo|grs?|g|lts?|litros?|l|ml|cc|un|u|unidades|ud)\\b",
            Pattern.CASE_INSENSITIVE);

    private PackageSizeParser() {
    }

    public static Optional<PackageSize> parse(String productName) {
        Matcher m = SIZE.matcher(productName.toLowerCase(Locale.ROOT));
        PackageSize last = null;
        while (m.find()) {   // el último suele ser el contenido ("Pack 2 x 500 g" -> 500 g)
            double n = Double.parseDouble(m.group(1).replace(',', '.'));
            String u = m.group(2);
            last = switch (u) {
                case "kg", "kilo" -> new PackageSize(n * 1000, "g");
                case "g", "gr", "grs" -> new PackageSize(n, "g");
                case "l", "lt", "lts", "litro", "litros" -> new PackageSize(n * 1000, "ml");
                case "ml", "cc" -> new PackageSize(n, "ml");
                default -> new PackageSize(n, "unit");
            };
        }
        return Optional.ofNullable(last);
    }
}
