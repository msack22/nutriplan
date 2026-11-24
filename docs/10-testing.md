# 10 · Testing

```bash
cd backend && mvn test     # todos los módulos
```

| Tipo | Ejemplos | Qué aprendés |
|---|---|---|
| Unitarios de dominio (sin Spring) | `EnergyCalculatorTest`, `BalancedCalorieStrategyTest`, `WeeklyPlanTest`, `ShoppingListBuilderTest`, `ProductMatcherTest` | lógica pura = tests rápidos y deterministas (`Clock` y `seed` inyectables) |
| Parametrizados | `IngredientLineParserTest` (`@CsvSource`), `ProductMatcherTest` | muchos casos con un solo test |
| `@Nested` | `RecipeBodyParserTest` | agrupar escenarios |
| Slice web | `PlanControllerSecurityTest` (`@WebMvcTest` + `jwt()` de spring-security-test + `@MockitoBean`) | probar seguridad y HTTP sin DB ni Keycloak |
| Arquitectura | `ArchitectureTest` (ArchUnit) en cada servicio | las capas se verifican en el build |
| Integración (dependencias listas) | Testcontainers MySQL + `@ServiceConnection` | probar repositorios contra MySQL real |
| Frontend | Vitest (`ng test`) | — |

Bugs reales que encontraron los tests durante el desarrollo:
- el matcher elegía "Fideos al huevo" para *huevo* (era más barato) → regla de "empieza con el ingrediente";
- sin la auto-config ordenada, el starter oauth2-client devolvía **302** en vez de **401**;
- con pocas recetas candidatas el generador no llegaba a las kcal objetivo (dato del test, no del algoritmo).

Pirámide: muchos unitarios, algunos de integración/slice, pocos end-to-end.
