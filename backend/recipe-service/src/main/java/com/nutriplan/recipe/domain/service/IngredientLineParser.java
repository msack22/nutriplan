package com.nutriplan.recipe.domain.service;

import com.nutriplan.recipe.domain.model.Unit;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta una línea de ingrediente en español rioplatense:
 * <pre>
 *   "1/2 palta"                         -> 0.5  UNIT  palta
 *   "120g de pollo a la parrilla"       -> 120  G     pollo
 *   "2 cucharadas de aceite de oliva"   -> 2    TBSP  aceite de oliva
 *   "1 lata (400 ml) de leche de coco"  -> 1    CAN   leche de coco
 *   "Sal y pimienta al gusto"           -> null TO_TASTE sal y pimienta
 * </pre>
 * Patrón: <b>Interpreter</b> muy simplificado basado en expresiones regulares.
 */
public final class IngredientLineParser {

    private static final Map<String, Double> WORD_NUMBERS = Map.of(
            "½", 0.5, "¼", 0.25, "¾", 0.75, "medio", 0.5, "media", 0.5,
            "un", 1.0, "una", 1.0, "dos", 2.0, "tres", 3.0, "cuatro", 4.0);

    private static final String NUMBER =
            "(\\d+\\s+y\\s+\\d+/\\d+|\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:[.,]\\d+)?(?:\\s*(?:-|a)\\s*\\d+(?:[.,]\\d+)?)?|½|¼|¾|medio|media|una|un|dos|tres|cuatro)";

    private static final String UNIT_WORDS =
            "(kilos?|kg|gramos?|grs?|g|ml|cc|litros?|lts?|l|tazas?|cucharadas?|cdas?|cucharaditas?|cdtas?|"
                    + "rebanadas?|fetas?|latas?|dientes?|pizcas?|unidades?|u|paquetes?|cabezas?|puñados?|hojas?|ramas?|sobres?)";

    private static final Pattern QTY_UNIT = Pattern.compile(
            "^" + NUMBER + "\\s*" + UNIT_WORDS + "?(?=\\s|$|\\.|,)\\s*(?:de\\s+)?(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern TO_TASTE = Pattern.compile(
            "(al gusto|a gusto|c/n|cantidad necesaria|a elección)", Pattern.CASE_INSENSITIVE);

    private IngredientLineParser() {
    }

    public static ParsedIngredient parse(String line) {
        String raw = line == null ? "" : line.strip().replaceAll("[.;]+$", "").replaceAll("\\s+", " ");
        String text = raw;
        boolean optional = text.toLowerCase(Locale.ROOT).contains("opcional");
        text = text.replaceFirst("(?i)^opcional:?\\s*", "");

        Matcher m = QTY_UNIT.matcher(text);
        if (m.matches()) {
            Double qty = parseNumber(m.group(1));
            Unit unit = m.group(2) == null ? Unit.UNIT : toUnit(m.group(2));
            String rest = m.group(3);
            if (unit == Unit.UNIT && rest.isBlank()) {
                return new ParsedIngredient(raw, IngredientNameNormalizer.normalize(text), null, Unit.TO_TASTE, optional);
            }
            return new ParsedIngredient(raw, IngredientNameNormalizer.normalize(rest), qty, unit, optional);
        }
        if (TO_TASTE.matcher(text).find()) {
            return new ParsedIngredient(raw, IngredientNameNormalizer.normalize(text), null, Unit.TO_TASTE, optional);
        }
        return new ParsedIngredient(raw, IngredientNameNormalizer.normalize(text), null, Unit.TO_TASTE, optional);
    }

    static Double parseNumber(String token) {
        String t = token.toLowerCase(Locale.ROOT).strip();
        if (WORD_NUMBERS.containsKey(t)) {
            return WORD_NUMBERS.get(t);
        }
        if (t.matches("\\d+\\s*(-|a)\\s*\\d+(?:[.,]\\d+)?")) {          // rango "2-3" -> 2.5
            String[] parts = t.split("\\s*(-|a)\\s*");
            return (Double.parseDouble(parts[0]) + Double.parseDouble(parts[1].replace(',', '.'))) / 2;
        }
        if (t.matches("\\d+\\s+y\\s+\\d+/\\d+") || t.matches("\\d+\\s+\\d+/\\d+")) {  // "1 y 1/2"
            String[] parts = t.split("\\s+(y\\s+)?");
            return Double.parseDouble(parts[0]) + fraction(parts[parts.length - 1]);
        }
        if (t.contains("/")) {
            return fraction(t);
        }
        return Double.parseDouble(t.replace(',', '.'));
    }

    private static double fraction(String f) {
        String[] p = f.split("/");
        return Double.parseDouble(p[0]) / Double.parseDouble(p[1]);
    }

    static Unit toUnit(String word) {
        String w = word.toLowerCase(Locale.ROOT);
        if (w.matches("kilos?|kg")) return Unit.KG;
        if (w.matches("gramos?|grs?|g")) return Unit.G;
        if (w.matches("ml|cc")) return Unit.ML;
        if (w.matches("litros?|lts?|l")) return Unit.L;
        if (w.matches("tazas?")) return Unit.CUP;
        if (w.matches("cucharaditas?|cdtas?")) return Unit.TSP;
        if (w.matches("cucharadas?|cdas?")) return Unit.TBSP;
        if (w.matches("rebanadas?|fetas?")) return Unit.SLICE;
        if (w.matches("latas?")) return Unit.CAN;
        if (w.matches("dientes?")) return Unit.CLOVE;
        if (w.matches("pizcas?")) return Unit.PINCH;
        return Unit.UNIT;
    }
}
