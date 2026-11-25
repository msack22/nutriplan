package com.nutriplan.recipe.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Aggregate Root del catálogo de recetas (DDD).
 *
 * <p>Conceptos de JPA/Hibernate que se ven acá (ver docs/04-jpa-hibernate.md):
 * <ul>
 *   <li>{@code @ManyToOne(fetch = LAZY)}: la categoría se carga solo si se usa.</li>
 *   <li>{@code @OneToMany(mappedBy, cascade = ALL, orphanRemoval = true)}: los ingredientes
 *       viven y mueren con la receta.</li>
 *   <li>{@code @ElementCollection}: colecciones de valores (enums, strings) en tablas propias.</li>
 *   <li>{@code @BatchSize}: mitiga el problema N+1 cargando colecciones de varias recetas
 *       en un solo {@code WHERE recipe_id IN (...)}.</li>
 *   <li>{@code @Version}: optimistic locking; dos admins editando a la vez -> el segundo falla
 *       en lugar de pisar los cambios del primero.</li>
 *   <li>{@code @CreatedDate/@LastModifiedDate}: auditoría automática de Spring Data JPA.</li>
 * </ul>
 * Las colecciones nunca se exponen mutables: se modifican por métodos con intención de negocio.
 */
@Entity
@Table(name = "recipe")
@EntityListeners(AuditingEntityListener.class)
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id en la fuente de importación: hace la importación idempotente (upsert). */
    @Column(name = "external_id", unique = true, length = 64)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Cantidad de porciones que rinde la receta tal cual está escrita. */
    @Column(nullable = false)
    private int servings = 1;

    @Column(name = "yield_label", length = 120)
    private String yieldLabel;

    @Embedded
    private Nutrition nutrition = Nutrition.EMPTY;

    @Enumerated(EnumType.STRING)
    @Column(name = "nutrition_source", length = 20, nullable = false)
    private NutritionSource nutritionSource = NutritionSource.UNKNOWN;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recipe_meal_type", joinColumns = @JoinColumn(name = "recipe_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", length = 20)
    @BatchSize(size = 50)
    private Set<MealType> mealTypes = EnumSet.noneOf(MealType.class);

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recipe_tag", joinColumns = @JoinColumn(name = "recipe_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", length = 30)
    @BatchSize(size = 50)
    private Set<DietTag> tags = EnumSet.noneOf(DietTag.class);

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 50)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    /** Lista indexada (@OrderColumn) => no es un "bag" => evita MultipleBagFetchException. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recipe_step", joinColumns = @JoinColumn(name = "recipe_id"))
    @OrderColumn(name = "position")
    @Column(name = "step", columnDefinition = "TEXT")
    @BatchSize(size = 50)
    private List<String> steps = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /** null = receta del catálogo (pública); si no, el "sub" del usuario que la creó (privada). */
    @Column(name = "owner_id", length = 64)
    private String ownerId;

    private boolean active = true;

    @Version
    private long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Recipe() {
    }

    public Recipe(String externalId, String title, Category category) {
        this.externalId = externalId;
        rename(title);
        changeCategory(category);
    }

    /** Receta propia de un usuario (solo la ve él). */
    public static Recipe ownedBy(String ownerId, String title, Category category) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("Una receta propia necesita dueño");
        }
        Recipe recipe = new Recipe(null, title, category);
        recipe.ownerId = ownerId;
        return recipe;
    }

    // ----------------------------------------------------------------- comportamiento

    public boolean isCustom() {
        return ownerId != null;
    }

    /** Regla de visibilidad: catálogo para todos, recetas propias solo para su dueño. */
    public boolean isVisibleTo(String userId) {
        return active && (ownerId == null || ownerId.equals(userId));
    }

    public boolean isOwnedBy(String userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public void deactivate() {
        this.active = false;
    }

    public void rename(String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new IllegalArgumentException("El título de la receta es obligatorio");
        }
        this.title = newTitle.strip();
    }

    public void changeCategory(Category newCategory) {
        this.category = Objects.requireNonNull(newCategory, "category");
    }

    public void defineYield(int servings, String yieldLabel) {
        if (servings < 1) {
            throw new IllegalArgumentException("Una receta rinde al menos 1 porción");
        }
        this.servings = servings;
        this.yieldLabel = yieldLabel;
    }

    public void updateNutrition(Nutrition perServing, NutritionSource source) {
        this.nutrition = perServing == null ? Nutrition.EMPTY : perServing;
        this.nutritionSource = this.nutrition.hasNoValues() ? NutritionSource.UNKNOWN : source;
    }

    /** Reemplaza todos los ingredientes (orphanRemoval borra los que ya no están). */
    public void replaceIngredients(List<RecipeIngredient> newIngredients) {
        ingredients.clear();
        for (int i = 0; i < newIngredients.size(); i++) {
            RecipeIngredient ingredient = newIngredients.get(i);
            ingredient.attachTo(this, i);
            ingredients.add(ingredient);
        }
    }

    public void replaceSteps(List<String> newSteps) {
        steps.clear();
        steps.addAll(newSteps);
    }

    public void classify(Set<MealType> newMealTypes, Set<DietTag> newTags) {
        // Copia defensiva: el argumento podría ser una vista de nuestra propia colección
        Set<MealType> meals = Set.copyOf(newMealTypes);
        Set<DietTag> tagsCopy = Set.copyOf(newTags);
        this.mealTypes.clear();
        this.mealTypes.addAll(meals);
        this.tags.clear();
        this.tags.addAll(tagsCopy);
    }

    public void describe(String notes, String videoUrl) {
        this.notes = notes;
        this.videoUrl = videoUrl;
    }

    public boolean isSuitableFor(MealType mealType) {
        return mealTypes.contains(mealType);
    }

    /** Texto que se usa para generar el embedding (búsqueda semántica). */
    public String toEmbeddingText() {
        String ingredientNames = String.join(", ", ingredients.stream().map(RecipeIngredient::getName).toList());
        return "%s. Categoría: %s. Ingredientes: %s. Etiquetas: %s".formatted(
                title, category.getName(), ingredientNames, tags);
    }

    // ----------------------------------------------------------------- getters

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public Category getCategory() {
        return category;
    }

    public int getServings() {
        return servings;
    }

    public String getYieldLabel() {
        return yieldLabel;
    }

    public Nutrition getNutrition() {
        return nutrition == null ? Nutrition.EMPTY : nutrition;
    }

    public NutritionSource getNutritionSource() {
        return nutritionSource;
    }

    public Set<MealType> getMealTypes() {
        return Collections.unmodifiableSet(mealTypes);
    }

    public Set<DietTag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public List<RecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public List<String> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public String getNotes() {
        return notes;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public boolean isActive() {
        return active;
    }

    public long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
