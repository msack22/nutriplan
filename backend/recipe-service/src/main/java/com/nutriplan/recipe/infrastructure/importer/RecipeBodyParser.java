package com.nutriplan.recipe.infrastructure.importer;

import com.nutriplan.recipe.domain.model.Nutrition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser tolerante del cuerpo markdown de una receta del recetario. Soporta los formatos
 * encontrados en los datos reales:
 * <pre>
 *  **Ingredientes:**              |  **Ingredientes**: 1 lata de atún, 1 pepino, ...
 *  - 2 huevos                     |  **Preparación**: En un tazón, combina...
 *  - 1/2 palta                    |
 *  **Procedimiento:**             |  Ingredientes: (base) 1 taza de almendras. (relleno) ...
 *  1. Batir los huevos            |
 *  **Tabla nutricional** — Calorías: 400/500 kcal · Proteínas: 30/40 g ...
 * </pre>
 * Es heurístico: lo que no se pueda interpretar se corrige luego desde el panel admin.
 */
public final class RecipeBodyParser {

    public record ParsedBody(int servings, String yieldLabel, List<String> ingredients, List<String> steps,
                             String notes, Nutrition nutrition) {
    }

    private enum Section { PREAMBLE, INGREDIENTS, STEPS, NUTRITION, NOTES }

    private static final Pattern HEADING = Pattern.compile(
            "^\\s*(ingredientes(?:\\s*\\([^)]*\\))?|procedimiento|preparaci[oó]n|tabla nutricional|"
                    + "consejos?(?: adicionales)?)\\s*[:—–-]?\\s*(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*(?:[-•*]|\\d+[.)])\\s+(.*)$");
    private static final Pattern YIELD = Pattern.compile("(?i)rinde\\s+(\\d+)\\s*([\\p{L}]+)?");
    private static final Pattern YIELD_IN_HEADING = Pattern.compile("(?i)ingredientes\\s*\\((\\d+)\\s*porci");
    private static final Pattern GROUP_LABEL = Pattern.compile("^\\((?:base|relleno|masa|cobertura|salsa)[^)]*\\)\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRIPTOR = Pattern.compile(
            "^(picad|rallad|cortad|enjuagad|en |sin |pelad|lavad|escurrid|cocid|derretid|desmenuzad|finamente|"
                    + "fresc|molid|sin piel|y picad|en rodajas|en cubos|opcional\\))", Pattern.CASE_INSENSITIVE);

    private RecipeBodyParser() {
    }

    public static ParsedBody parse(String markdown) {
        String text = markdown == null ? "" : markdown.replace("\r", "")
                .replaceAll("\\*+", "")
                .replaceAll("(?m)^-{3,}\\s*$", "");

        int servings = 1;
        String yieldLabel = null;
        Matcher y = YIELD.matcher(text);
        if (y.find()) {
            servings = Integer.parseInt(y.group(1));
            yieldLabel = y.group(0).strip();
        } else {
            Matcher yh = YIELD_IN_HEADING.matcher(text);
            if (yh.find()) {
                servings = Integer.parseInt(yh.group(1));
                yieldLabel = "Rinde " + servings + " porciones";
            }
        }

        List<String> ingredientLines = new ArrayList<>();
        List<String> stepLines = new ArrayList<>();
        List<String> noteLines = new ArrayList<>();
        Section section = Section.PREAMBLE;

        for (String line : text.split("\n")) {
            Matcher h = HEADING.matcher(line);
            if (h.matches()) {
                section = sectionOf(h.group(1));
                line = h.group(2);
            }
            if (line.isBlank() && section != Section.STEPS && section != Section.INGREDIENTS) {
                continue;
            }
            switch (section) {
                case INGREDIENTS -> ingredientLines.add(line);
                case STEPS -> stepLines.add(line);
                case NOTES -> noteLines.add(line.strip());
                case PREAMBLE -> {
                    if (!YIELD.matcher(line).find()) {
                        noteLines.add(line.strip());
                    }
                }
                case NUTRITION -> { }
            }
        }

        return new ParsedBody(servings, yieldLabel,
                splitIngredients(ingredientLines),
                splitSteps(stepLines),
                noteLines.isEmpty() ? null : String.join("\n", noteLines).strip(),
                NutritionTableParser.parse(text));
    }

    private static Section sectionOf(String heading) {
        String h = heading.toLowerCase(Locale.ROOT);
        if (h.startsWith("ingredientes")) return Section.INGREDIENTS;
        if (h.startsWith("tabla")) return Section.NUTRITION;
        if (h.startsWith("consejo")) return Section.NOTES;
        return Section.STEPS;
    }

    /** Agrupa líneas en items: con viñetas => un item por viñeta; prosa => separa por comas. */
    static List<String> splitIngredients(List<String> lines) {
        List<String> blocks = toBlocks(lines);
        List<String> items = new ArrayList<>();
        boolean isList = lines.stream().anyMatch(l -> LIST_ITEM.matcher(l).matches());
        for (String block : blocks) {
            if (isList) {
                items.add(block);
                continue;
            }
            // prosa: "1 lata de atún, 1 pepino en cubos, ... sal y pimienta al gusto."
            for (String part : block.split("\\s*,\\s*|\\.\\s+(?=\\()|\\.\\s+(?=\\d)")) {
                String p = GROUP_LABEL.matcher(part.strip()).replaceFirst("").replaceAll("[.;]+$", "").strip();
                if (p.isEmpty() || p.endsWith(":")) {
                    continue;
                }
                if (!items.isEmpty() && DESCRIPTOR.matcher(p).find()) {
                    items.set(items.size() - 1, items.getLast() + ", " + p);   // "1 cebolla" + "picada"
                } else {
                    items.add(p);
                }
            }
        }
        return items.stream().filter(i -> !i.isBlank() && !i.endsWith(":")).toList();
    }

    static List<String> splitSteps(List<String> lines) {
        boolean isList = lines.stream().anyMatch(l -> LIST_ITEM.matcher(l).matches());
        List<String> blocks = toBlocks(lines);
        if (isList) {
            return blocks;
        }
        return blocks.stream()
                .flatMap(b -> Arrays.stream(b.split("(?<=\\.)\\s+(?=[A-ZÁÉÍÓÚÑ¡¿])")))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** Une líneas cortadas (hard-wrap) y separa por viñetas o líneas en blanco. */
    private static List<String> toBlocks(List<String> lines) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String raw : lines) {
            Matcher item = LIST_ITEM.matcher(raw);
            if (item.matches()) {
                flush(blocks, current);
                current.append(item.group(1).strip());
            } else if (raw.isBlank()) {
                flush(blocks, current);
            } else {
                if (!current.isEmpty()) {
                    current.append(' ');
                }
                current.append(raw.strip());
            }
        }
        flush(blocks, current);
        return blocks;
    }

    private static void flush(List<String> blocks, StringBuilder sb) {
        if (!sb.isEmpty()) {
            blocks.add(sb.toString().replaceAll("\\s+", " ").strip());
            sb.setLength(0);
        }
    }
}
