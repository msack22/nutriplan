package com.nutriplan.pricing.domain.service;

import com.nutriplan.pricing.domain.model.ProductOffer;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Elige qué producto del súper corresponde a un ingrediente. La búsqueda del supermercado
 * devuelve ruido ("huevos" -> "Huevo de Pascua"), así que puntuamos:
 * <ul>
 *   <li>el nombre debe contener todas las palabras del ingrediente</li>
 *   <li>primero los que EMPIEZAN con el ingrediente ("Huevos de gallina" antes que
 *       "Fideos al huevo"): en los catálogos el sustantivo principal va primero</li>
 *   <li>penalizamos palabras "sospechosas" (pascua, juguete, alimento para perro...)</li>
 *   <li>preferimos nombres cortos (menos palabras extra = producto más genérico)</li>
 *   <li>a igual relevancia, el más barato</li>
 * </ul>
 */
public final class ProductMatcher {

    private static final Set<String> NOISE = Set.of("pascua", "juguete", "perro", "gato", "mascota", "shampoo",
            "jabon", "detergente", "golosina", "chupetin", "sorpresa", "decoracion", "maquillaje", "vela");

    private ProductMatcher() {
    }

    public static Optional<ProductOffer> bestMatch(String ingredient, List<ProductOffer> offers) {
        Set<String> wanted = tokens(ingredient);
        String firstWord = firstToken(ingredient);
        return offers.stream()
                .filter(ProductOffer::available)
                .filter(o -> o.price() > 0)
                .filter(o -> tokens(o.name()).stream().noneMatch(NOISE::contains))
                .filter(o -> containsAll(tokens(o.name()), wanted))
                .min(Comparator.comparingInt((ProductOffer o) -> stem(firstToken(o.name())).equals(stem(firstWord)) ? 0 : 1)
                        .thenComparingInt(o -> tokens(o.name()).size() / 3)
                        .thenComparingDouble(ProductOffer::price));
    }

    /** "Huevos" debe matchear "Huevo": comparamos por raíz (primeros 4 caracteres). */
    private static boolean containsAll(Set<String> name, Set<String> wanted) {
        return wanted.stream().allMatch(w -> name.stream().anyMatch(n -> stem(n).equals(stem(w))));
    }

    private static String stem(String w) {
        return w.length() > 4 ? w.substring(0, 4) : w;
    }

    private static String firstToken(String text) {
        String clean = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9 ]", " ").strip();
        return clean.isEmpty() ? "" : clean.split("\\s+")[0];
    }

    static Set<String> tokens(String text) {
        String clean = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9 ]", " ");
        return Arrays.stream(clean.split("\\s+"))
                .filter(t -> t.length() > 2 && !Set.of("con", "del", "las", "los", "sin", "para").contains(t))
                .collect(Collectors.toSet());
    }
}
