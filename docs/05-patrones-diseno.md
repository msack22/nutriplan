# 05 · Patrones de diseño (y dónde encontrarlos)

| Patrón | Dónde | Para qué |
|---|---|---|
| **Strategy** | `PlanGenerationStrategy` → `BalancedCalorieStrategy`, `RandomVarietyStrategy`; `EmbeddingGenerator` → Ollama / Hashing | intercambiar algoritmos sin tocar a quien los usa |
| **Factory / Registry** | `PlanGeneratorRegistry`, `PricingConfig.supermarketCatalogs()` | crear/obtener la implementación según nombre o configuración |
| **Adapter** | `VtexCatalogAdapter` (SupermarketCatalog), `RecipeServiceClient` (RecipeCatalogPort), `RecipeExportReader` | traducir APIs externas a interfaces propias (hexagonal) |
| **Builder** | `ShoppingListBuilder`, `TopicBuilder`, `RestClient.builder()` | construir objetos complejos paso a paso |
| **Specification** | `RecipeSpecifications` | reglas de filtrado combinables |
| **Repository** | Spring Data (`RecipeRepository`, `WeeklyPlanRepository`) | abstraer la persistencia |
| **Aggregate Root (DDD)** | `WeeklyPlan`, `Recipe` | proteger invariantes: todo cambio pasa por la raíz |
| **Value Object** | `Nutrition`, `NutritionSnapshot`, `PackageSize`, `RecipeRef` (records) | inmutables, se comparan por valor |
| **Null Object** | `noopNutritionEstimator` en `AiConfig` | evitar `if (estimator != null)` cuando no hay IA |
| **Template / Interceptor (cadena)** | `SecurityFilterChain`, `AuditLoggingFilter`, interceptores de `RestClient` y de Angular | comportamiento transversal |
| **Proxy (AOP)** | `@Transactional`, `@Cacheable`, `@Retryable`, `@PreAuthorize` | Spring envuelve el bean |
| **Observer / Pub-Sub** | Kafka (`@KafkaListener`), Spring events | reaccionar a eventos sin acoplarse |
| **Transactional Outbox** | `OutboxEvent` + `OutboxRelay` | publicar eventos consistentes con la base |
| **Circuit Breaker** | `VtexCatalogAdapter` (Resilience4j) | no insistir con un servicio caído |
| **BFF** | gateway | backend dedicado al frontend; tokens fuera del navegador |
| **API Gateway** | gateway | punto único de entrada, enrutamiento, seguridad de borde |

## SOLID en el proyecto
- **S**: `IngredientLineParser` solo parsea; `IngredientNameNormalizer` solo normaliza.
- **O**: nueva cadena de supermercado = nueva línea de YAML; nueva estrategia de plan = nueva clase.
- **L**: cualquier `EmbeddingGenerator` sirve donde se espera uno.
- **I**: puertos chicos (`PricingPort` tiene un método).
- **D**: el dominio depende de interfaces (`RecipeCatalogPort`), no de `RestClient`.

## Clean code aplicado
Nombres con intención (`markEaten`, `changePeople`), métodos cortos, sin "magic numbers" (`KCAL_PER_KG_FAT = 7700`), inmutabilidad (records, colecciones no modificables), errores de dominio expresivos (`BusinessRuleException`), sin Lombok (records + Java 21 cubren casi todo; Lombok es muy usado igual — saber qué genera `@Data`, `@Builder`, `@RequiredArgsConstructor`).
