package com.nutriplan.recipe.domain.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lleva "2 tazas de Espinacas frescas, lavadas" a un nombre canónico ("espinaca") para poder
 * AGRUPAR ingredientes de distintas recetas en la lista de compras.
 *
 * <p>Es una heurística simple (no NLP): quita descriptores de preparación, paréntesis, pasa a
 * singular y aplica sinónimos rioplatenses. Pura (sin dependencias) => fácil de testear.
 */
public final class IngredientNameNormalizer {

    private static final List<String> CUT_MARKERS = List.of(
            ",", ";", " o ", " para ", " en cubos", " en rodajas", " en mitades", " en tiras", " en hebras",
            " picad", " rallad", " cortad", " desmenuzad", " enjuagad", " escurrid", " cocid", " derretid",
            " a la parrilla", " al gusto", " a gusto", " c/n", " a elección", " de tu preferencia", " fresc",
            " sin piel", " sin semillas", " pelad", " lavad", " machacad", " triturad", " tostad",
            // tamaños y formas de corte: "cebolla grande" y "cebolla mediana" son la misma compra
            " grande", " median", " pequeñ", " chic", " en florete", " en juliana", " en trozos", " en cuartos",
            " madur", " virgen extra", " crud");

    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("aguacate", "palta"),
            Map.entry("calabacin", "zapallito"),
            Map.entry("zucchini", "zapallito"),
            Map.entry("zapallito verde", "zapallito"),
            Map.entry("jitomate", "tomate"),
            Map.entry("tomate cherry", "tomate cherry"),
            Map.entry("mantequilla", "manteca"),
            Map.entry("frutilla", "frutilla"),
            Map.entry("fresa", "frutilla"),
            Map.entry("ajo picado", "ajo"),
            Map.entry("clara de huevo", "huevo"),
            Map.entry("yema", "huevo"),
            Map.entry("aceite de oliva virgen extra", "aceite de oliva"),
            Map.entry("pechuga de pollo", "pollo"),
            Map.entry("pollo a la parrilla", "pollo"));

    private IngredientNameNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = text.toLowerCase(Locale.ROOT)
                .replaceAll("\\([^)]*\\)", " ")   // "(400 ml)", "(opcional)"
                .replaceAll("^opcional:?\\s*", "")
                .replaceAll("\\s+", " ")
                .strip();
        for (String marker : CUT_MARKERS) {
            int idx = s.indexOf(marker);
            if (idx > 0) {
                s = s.substring(0, idx);
            }
        }
        s = s.replaceAll("^(de|del|la|el|los|las|unos|unas)\\s+", "").replaceAll("[.:]+$", "").strip();
        String singular = Arrays.stream(s.split(" "))
                .map(IngredientNameNormalizer::singular)
                .collect(Collectors.joining(" "));
        String plain = stripAccents(singular);
        return SYNONYMS.getOrDefault(plain, SYNONYMS.getOrDefault(singular, singular));
    }

    static String singular(String word) {
        if (word.length() <= 3 || !word.endsWith("s") || List.of("de", "del", "las", "los").contains(word)) {
            return word;
        }
        if (word.endsWith("ces")) {
            return word.substring(0, word.length() - 3) + "z";          // nueces -> nuez
        }
        if (word.endsWith("es") && "lnrdj".indexOf(word.charAt(word.length() - 3)) >= 0) {
            return word.substring(0, word.length() - 2);                // limones -> limon
        }
        return word.substring(0, word.length() - 1);                    // huevos -> huevo
    }

    static String stripAccents(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
