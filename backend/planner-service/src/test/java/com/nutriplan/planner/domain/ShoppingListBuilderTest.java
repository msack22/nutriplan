package com.nutriplan.planner.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutriplan.planner.domain.port.RecipeCatalogPort.IngredientLine;
import com.nutriplan.planner.domain.service.Aisle;
import com.nutriplan.planner.domain.service.ShoppingListBuilder;
import com.nutriplan.planner.domain.service.ShoppingListBuilder.ShoppingItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShoppingListBuilderTest {

    @Test
    void sumsSameIngredientAcrossRecipesConvertingUnits() {
        List<ShoppingItem> items = new ShoppingListBuilder()
                .add("Batido", List.of(new IngredientLine("leche", 1.0, "CUP", false)), 2)      // 480 ml
                .add("Avena", List.of(new IngredientLine("leche", 200.0, "ML", false)), 1)      // 200 ml
                .add("Tortilla", List.of(new IngredientLine("huevo", 3.0, "UNIT", false),
                        new IngredientLine("sal", null, "TO_TASTE", false),
                        new IngredientLine("semilla de sesamo", 1.0, "TBSP", true)), 1.5)
                .build();

        assertThat(items).extracting(ShoppingItem::name).containsExactlyInAnyOrder("leche", "huevo", "sal");
        ShoppingItem milk = items.stream().filter(i -> i.name().equals("leche")).findFirst().orElseThrow();
        assertThat(milk.quantity()).isEqualTo(680.0);
        assertThat(milk.unit()).isEqualTo("ml");
        assertThat(milk.recipes()).containsExactly("Avena", "Batido");
        ShoppingItem eggs = items.stream().filter(i -> i.name().equals("huevo")).findFirst().orElseThrow();
        assertThat(eggs.quantity()).isEqualTo(4.5);
        assertThat(eggs.aisle()).isEqualTo(Aisle.LACTEOS);
    }

    @Test
    void classifiesAisles() {
        assertThat(Aisle.classify("pollo")).isEqualTo(Aisle.CARNICERIA);
        assertThat(Aisle.classify("palta")).isEqualTo(Aisle.VERDULERIA);
        assertThat(Aisle.classify("leche de coco")).isEqualTo(Aisle.ALMACEN);
        assertThat(Aisle.classify("comino molido")).isEqualTo(Aisle.ESPECIAS);
        assertThat(Aisle.classify("caldo de verdura")).isEqualTo(Aisle.ALMACEN);
    }
}
