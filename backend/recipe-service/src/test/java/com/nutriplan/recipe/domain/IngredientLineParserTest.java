package com.nutriplan.recipe.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutriplan.recipe.domain.model.Unit;
import com.nutriplan.recipe.domain.service.IngredientLineParser;
import com.nutriplan.recipe.domain.service.ParsedIngredient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Test parametrizado: un mismo test, muchos casos (JUnit 5 @ParameterizedTest). */
class IngredientLineParserTest {

    @ParameterizedTest(name = "\"{0}\" -> {1} {2} {3}")
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            1/2 palta                                   | 0.5   | UNIT     | palta
            120g de pollo a la parrilla en cubos        | 120.0 | G        | pollo
            2 cucharadas de aceite de oliva             | 2.0   | TBSP     | aceite de oliva
            1 lata (400 ml) de leche de coco            | 1.0   | CAN      | leche de coco
            2 dientes de ajo, picados                   | 2.0   | CLOVE    | ajo
            Sal y pimienta al gusto                     | null  | TO_TASTE | sal y pimienta
            4 huevos                                    | 4.0   | UNIT     | huevo
            media palta                                 | 0.5   | UNIT     | palta
            1 y 1/2 taza de harina de almendras         | 1.5   | CUP      | harina de almendra
            2 limones                                   | 2.0   | UNIT     | limon
            150 gramos de carne molida cocida           | 150.0 | G        | carne molida
            Queso en hebras c/n                         | null  | TO_TASTE | queso
            2-3 tomates                                 | 2.5   | UNIT     | tomate
            1/4 taza de nueces picadas                  | 0.25  | CUP      | nuez
            1 cebolla grande picada                     | 1.0   | UNIT     | cebolla
            2 tazas de brócoli en floretes              | 2.0   | CUP      | brócoli
            2 cucharadas de aceite de oliva virgen extra| 2.0   | TBSP     | aceite de oliva
            """)
    void parsesQuantityUnitAndName(String line, Double qty, Unit unit, String name) {
        ParsedIngredient parsed = IngredientLineParser.parse(line);

        assertThat(parsed.quantity()).isEqualTo(qty);
        assertThat(parsed.unit()).isEqualTo(unit);
        assertThat(parsed.name()).isEqualTo(name);
        assertThat(parsed.rawText()).isEqualTo(line);
    }

    @ParameterizedTest
    @CsvSource({"Opcional: semillas de sésamo para decorar", "1/4 cucharadita de chile en polvo (opcional)"})
    void detectsOptionalIngredients(String line) {
        assertThat(IngredientLineParser.parse(line).optional()).isTrue();
    }
}
