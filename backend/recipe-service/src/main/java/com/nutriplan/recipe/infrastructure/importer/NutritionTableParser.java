package com.nutriplan.recipe.infrastructure.importer;

import com.nutriplan.recipe.domain.model.Nutrition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee "Calorías: 400/500 kcal · Carbohidratos: 10/15 g · Proteínas: 30/40 g · Grasas: - g".
 * Los rangos "a/b" se promedian; "-" significa sin dato (null).
 */
public final class NutritionTableParser {

    private static final String VALUE = "([\\d.,]+(?:\\s*/\\s*[\\d.,]+)?|-)";
    private static final Pattern KCAL = Pattern.compile("(?i)calor[ií]as:\\s*" + VALUE);
    private static final Pattern CARBS = Pattern.compile("(?i)carbohidratos:\\s*" + VALUE);
    private static final Pattern PROTEIN = Pattern.compile("(?i)prote[ií]nas:\\s*" + VALUE);
    private static final Pattern FAT = Pattern.compile("(?i)grasas:\\s*" + VALUE);

    private NutritionTableParser() {
    }

    public static Nutrition parse(String text) {
        if (text == null || !text.toLowerCase().contains("calor")) {
            return Nutrition.EMPTY;
        }
        return new Nutrition(find(KCAL, text), find(PROTEIN, text), find(CARBS, text), find(FAT, text));
    }

    static Double find(Pattern p, String text) {
        Matcher m = p.matcher(text);
        if (!m.find() || m.group(1).equals("-")) {
            return null;
        }
        String[] parts = m.group(1).split("/");
        double sum = 0;
        for (String part : parts) {
            sum += Double.parseDouble(part.strip().replace(',', '.'));
        }
        return sum / parts.length;
    }
}
