# 04 · JPA / Hibernate

Spring Boot 4 trae **Hibernate 7** (Jakarta Persistence 3.2). JPA es la *especificación*; Hibernate la *implementación*; Spring Data JPA genera los repositorios.

## Mapeos usados (ver `Recipe.java`, `WeeklyPlan.java`)

| Anotación | Ejemplo | Nota |
|---|---|---|
| `@ManyToOne(fetch = LAZY)` | `Recipe.category` | Por defecto `@ManyToOne` es EAGER: **siempre ponerlo LAZY** |
| `@OneToMany(mappedBy, cascade = ALL, orphanRemoval = true)` | `Recipe.ingredients`, `WeeklyPlan.meals` | el hijo es dueño de la FK; quitar de la lista = DELETE |
| `@ElementCollection` | `mealTypes`, `tags`, `steps` | colecciones de valores en tabla propia |
| `@Embeddable` **record** | `Nutrition`, `NutritionSnapshot` | value objects inmutables (soportado desde Hibernate 6.2) |
| `@OneToOne` + `@MapsId` | `RecipeEmbedding` | PK compartida con la receta |
| `@Converter(autoApply = true)` | `FloatArrayConverter` | `float[]` ↔ BLOB |
| `@Enumerated(STRING)` | todos los enums | ORDINAL se rompe si reordenás el enum |
| `@Version` | `Recipe`, `WeeklyPlan`, `UserProfile` | **optimistic locking** |
| `@CreatedDate` / `@LastModifiedDate` | `Recipe`, `WeeklyPlan` | auditoría con `@EnableJpaAuditing` |

## El problema N+1

Listar 24 recetas y acceder a `recipe.getCategory()` en cada una dispara 1 query + 24 queries. Soluciones aplicadas:

1. **`@EntityGraph(attributePaths = "category")`** en `RecipeRepository.findAll(spec, pageable)`: hace JOIN FETCH solo en esa consulta.
2. **`@BatchSize(size = 50)`** / `hibernate.default_batch_fetch_size`: las colecciones LAZY se cargan con `WHERE recipe_id IN (...)` (1 query por colección, no por receta).
3. **DTOs** dentro de la transacción (no se serializan entidades).

### MultipleBagFetchException
Hibernate no puede hacer JOIN FETCH de **dos `List` sin índice** ("bags") a la vez (producto cartesiano). Por eso `steps` usa `@OrderColumn` (deja de ser bag) y solo `ingredients` va en el entity graph.

## `open-in-view: false`
Spring Boot por defecto mantiene la sesión abierta hasta renderizar la respuesta (OSIV), lo que esconde N+1 y retiene conexiones. Lo desactivamos: todo el mapeo a DTO ocurre en métodos `@Transactional(readOnly = true)`. Bug real que encontramos: devolver `r.getMealTypes()` (una `PersistentSet` LAZY) en el DTO producía `LazyInitializationException` al serializar → se copia con `Set.copyOf(...)` dentro de la transacción.

## Specifications (consultas dinámicas)
`RecipeSpecifications` construye filtros combinables con Criteria API: `active().and(titleContains(q)).and(forMeal(m))...`. Alternativas: QueryDSL, `@Query` JPQL, Query by Example.

## Transacciones
- `@Transactional(readOnly = true)` en lecturas (sin dirty checking).
- `Propagation.MANDATORY` en `OutboxEventPublisher`: exige estar dentro de la transacción del caso de uso.
- `TransactionTemplate` en `NutritionEstimationService`: transacciones **cortas** programáticas para no retener una conexión mientras el LLM piensa.
- Self-invocation: `@Transactional`/`@Cacheable` funcionan por **proxy**; llamar un método anotado desde la misma clase no pasa por el proxy (por eso `ShoppingListCalculator` es un bean aparte).

## Flyway
El esquema lo versiona Flyway (`db/migration/V1__*.sql`) y Hibernate solo **valida** (`ddl-auto: validate`). Nunca editar una migración aplicada: se agrega `V2__...`. `ddl-auto: update` está prohibido en producción.
