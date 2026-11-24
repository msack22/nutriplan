package com.nutriplan.recipe.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutriplan.recipe.infrastructure.importer.RecipeBodyParser;
import com.nutriplan.recipe.infrastructure.importer.RecipeBodyParser.ParsedBody;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecipeBodyParserTest {

    @Nested
    class ListFormat {
        private final ParsedBody body = RecipeBodyParser.parse("""
                *Rinde 2 porciones*

                *Aclaración: se puede usar cualquier pan integral o de
                semillas*

                **Ingredientes:**

                - 2 rebanadas de pan integral
                - 1/2 palta
                - Opcional: aceite de oliva para tostar el pan, semillas de sésamo para
                  decorar

                **Procedimiento:**

                - Tostar el pan en una sartén caliente (si se quiere, con unas gotas
                  de aceite de oliva).
                - Pisar la palta y untarla sobre el pan.

                **Tabla nutricional** — Calorías: 400 kcal · Carbohidratos: 35 g ·
                Proteínas: 12 g · Grasas: 20/30 g

                ---""");

        @Test
        void readsYield() {
            assertThat(body.servings()).isEqualTo(2);
            assertThat(body.yieldLabel()).isEqualTo("Rinde 2 porciones");
        }

        @Test
        void joinsWrappedListItems() {
            assertThat(body.ingredients()).containsExactly("2 rebanadas de pan integral", "1/2 palta",
                    "Opcional: aceite de oliva para tostar el pan, semillas de sésamo para decorar");
            assertThat(body.steps()).hasSize(2);
            assertThat(body.steps().getFirst()).endsWith("de aceite de oliva).");
        }

        @Test
        void averagesNutritionRanges() {
            assertThat(body.nutrition().kcal()).isEqualTo(400.0);
            assertThat(body.nutrition().fatG()).isEqualTo(25.0);
            assertThat(body.nutrition().proteinG()).isEqualTo(12.0);
        }

        @Test
        void keepsPreambleAsNotes() {
            assertThat(body.notes()).startsWith("Aclaración");
        }
    }

    @Test
    void parsesInlineProseFormat() {
        ParsedBody body = RecipeBodyParser.parse("""
                **Ingredientes**: 1 lata de garbanzos (escurridos), 1 pepino en cubos, 1
                tomate en cubos, 2 dientes de ajo, picados, sal y pimienta al gusto.

                **Preparación**: Colocar todo en un bowl grande. Mezclar y servir
                bien frío.""");

        assertThat(body.ingredients()).containsExactly("1 lata de garbanzos (escurridos)", "1 pepino en cubos",
                "1 tomate en cubos", "2 dientes de ajo, picados", "sal y pimienta al gusto");
        assertThat(body.steps()).containsExactly("Colocar todo en un bowl grande.", "Mezclar y servir bien frío.");
        assertThat(body.servings()).isEqualTo(1);
        assertThat(body.nutrition().hasNoValues()).isTrue();
    }

    @Test
    void stripsGroupLabelsAndReadsServingsFromHeading() {
        ParsedBody body = RecipeBodyParser.parse("""
                Ingredientes (2 porciones): (base) 1 taza de almendras molidas, 1/4 taza de manteca
                derretida. (relleno) 1 paquete de queso crema.""");

        assertThat(body.servings()).isEqualTo(2);
        assertThat(body.ingredients()).containsExactly("1 taza de almendras molidas",
                "1/4 taza de manteca derretida", "1 paquete de queso crema");
    }
}
