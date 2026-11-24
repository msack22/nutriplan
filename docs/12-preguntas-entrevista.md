# 12 · Preguntas de entrevista (respondidas con este proyecto)

Respuestas cortas para practicar en voz alta. En cada una, mencioná **dónde lo hiciste** en NutriPlan: es lo que diferencia "lo leí" de "lo usé".

## Java
**¿Qué es un `record` y cuándo lo usás?** Clase inmutable de datos con constructor, getters, `equals/hashCode/toString` generados. Los uso para DTOs, eventos y value objects (`Nutrition`, `RecipeRef`). Con el *compact constructor* valido o normalizo (`CandidateRequest`).

**Virtual threads (Java 21).** Hilos livianos gestionados por la JVM: miles de requests bloqueantes (I/O) sin un pool gigante. Activados con `spring.threads.virtual.enabled=true`.

**`Optional`, streams, `switch` con patrones.** `findByExternalId(...).orElse(null)`, `switch` de enums con flechas en `RecipeClassifier`, pattern matching `instanceof Map<?, ?> map` en `KeycloakRealmRoleConverter`.

**`equals`/`hashCode` en entidades JPA.** Evitar basarlos en el id autogenerado (es null antes de persistir). Los records como value objects los traen bien resueltos.

## Spring
**¿Qué hace `@SpringBootApplication`?** `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`. La auto-configuración lee `AutoConfiguration.imports` y aplica beans condicionales (`@ConditionalOnMissingBean`...). Yo escribí una propia en `common`.

**Inyección por constructor vs. `@Autowired` en campo.** Constructor: dependencias explícitas, inmutables (`final`), testeable sin Spring. Es lo que uso en todo el proyecto.

**¿Por qué `@Transactional` no funciona al llamar un método de la misma clase?** Se aplica por proxy AOP; la llamada interna no pasa por el proxy. Lo resolví separando `ShoppingListCalculator` (`@Cacheable`).

**`@Transactional(readOnly = true)`.** Sin dirty checking y con posibilidad de ir a una réplica de lectura.

**Perfiles y configuración.** `application.yml` + variables de entorno con default (`${DB_HOST:localhost}`), `@ConfigurationProperties` tipadas (`ImportProperties`, `SupermarketProperties`) en lugar de `@Value` sueltos.

**Manejo de errores.** `@RestControllerAdvice` + `ProblemDetail` (RFC 9457) en `GlobalExceptionHandler`.

**RestTemplate vs WebClient vs RestClient.** RestTemplate: legado. WebClient: reactivo. **RestClient** (Spring 6.1+): síncrono, fluido, lo que uso.

**Resiliencia.** `@Retryable` nativo de Spring Framework 7 (backoff exponencial, sin reintentar 4xx) y Circuit Breaker de Resilience4j en las cadenas de súper. Fallback: lista de compras sin precios.

## JPA / Hibernate
**N+1.** Ver [04](04-jpa-hibernate.md): `@EntityGraph`, `@BatchSize`, DTOs.
**LAZY vs EAGER.** LAZY por defecto; `@ManyToOne` hay que marcarlo LAZY explícitamente.
**LazyInitializationException.** Acceder a algo LAZY fuera de la sesión. Me pasó con `open-in-view=false`: copié la colección dentro de la transacción.
**Optimistic vs pessimistic locking.** `@Version` (optimista, sin bloqueos, falla al chocar) vs `SELECT ... FOR UPDATE` (pesimista). Uso optimista en `WeeklyPlan`.
**`ddl-auto`.** `validate` + Flyway. Nunca `update` en producción.
**Cascade y orphanRemoval.** `WeeklyPlan.meals`: guardar el plan guarda las comidas; quitar una de la lista la borra.

## Seguridad
**Autenticación vs autorización**, **JWT** (estructura, firma RS256, por qué no se revoca), **access vs refresh vs ID token**, **Authorization Code + PKCE**, **client credentials**, **token relay vs propagation**, **BFF**, **CSRF con cookies**, **IDOR/BOLA**, **audience** → todo en [03](03-seguridad.md).

**¿Dónde validás los permisos si ya pasó por el gateway?** En cada servicio también (defensa en profundidad / zero trust).

**¿Cómo guardás passwords?** No las guardo: las maneja Keycloak (hash PBKDF2/Argon2). En una app propia: `BCryptPasswordEncoder` / `Argon2PasswordEncoder` vía `PasswordEncoder`.

## Microservicios
**¿Cómo mantenés consistencia entre servicios?** Sin 2PC: consistencia eventual con eventos + **Transactional Outbox** + consumidores idempotentes. Para procesos largos, **Saga** (coreografía con eventos u orquestación).
**Sincrónico vs asincrónico.** Sync si necesito la respuesta ya (candidatas para el plan); async para notificar (precios actualizados).
**Database per service.** Cada servicio es dueño de sus datos; snapshots para no depender del otro en lecturas.
**Service discovery / config server.** No los usé (Docker DNS y variables de entorno alcanzan). En Kubernetes lo resuelven los Services y ConfigMaps; Eureka/Spring Cloud Config son la alternativa clásica.
**Observabilidad.** Correlation id en logs, Actuator, Micrometer/Prometheus, health probes. Siguiente paso: tracing distribuido con OpenTelemetry.

## Kafka / Redis
Ver [06](06-kafka-mensajeria.md) y [07](07-redis-cache-sesiones.md): particiones y orden, consumer groups, at-least-once, DLT, idempotencia, cache-aside, invalidación por versión, sesiones distribuidas.

## Testing
Pirámide, `@WebMvcTest` vs `@SpringBootTest`, `@MockitoBean` (reemplaza a `@MockBean`), Testcontainers, ArchUnit, tests parametrizados → [10](10-testing.md).

## Diseño / Clean code
Patrones con ejemplos concretos → [05](05-patrones-diseno.md). SOLID, inmutabilidad, nombres con intención, dominio sin frameworks.

## Angular
**Signals vs RxJS.** Signals para estado síncrono de la UI (`signal`, `computed`, `effect`); RxJS para flujos asíncronos (HTTP). **Standalone components** (sin NgModules), **control flow** `@if/@for/@let`, **lazy loading** con `loadComponent`, **guards e interceptores funcionales**, **Reactive Forms** tipados (`ProfilePage`), `input()`/`output()`, `withComponentInputBinding()` para recibir params de ruta como inputs.
