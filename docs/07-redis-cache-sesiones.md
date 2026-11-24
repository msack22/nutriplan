# 07 · Redis: sesiones y caché

## Sesiones del BFF (Spring Session)
Los microservicios son **stateless** (JWT), así que no hay "sesiones" que cachear ahí. Donde sí hay sesión es en el **gateway (BFF)**: guarda el `OAuth2AuthorizedClient` (access + refresh token) del usuario. Con `spring-boot-starter-session-data-redis` la sesión vive en Redis (`nutriplan:session:*`):

- el gateway puede tener N réplicas detrás de un load balancer (sin sticky sessions),
- un reinicio no desloguea a nadie,
- el navegador solo tiene la cookie `NUTRIPLAN_SESSION` (HttpOnly).

```bash
docker exec -it nutriplan-redis-1 redis-cli keys 'nutriplan:*'
```

## Caché de la lista de compras (planner)
`ShoppingListCalculator.calculate()` hace 2 llamadas HTTP (recipe + pricing). Con `@Cacheable` en Redis:

- **Clave = `planId:version`**: cada cambio del plan incrementa `@Version` → clave nueva → nunca se sirve una lista vieja, sin invalidación manual.
- **Invalidación por evento**: al llegar `prices-refreshed` por Kafka → `@CacheEvict(allEntries = true)`.
- **TTL 12 h** como red de seguridad.
- Serializador **tipado** (`JacksonJsonRedisSerializer<ShoppingListDto>`), evitando default typing polimórfico (vector de RCE en deserialización).

Estrategias de caché para entrevistas: cache-aside (esta), read-through, write-through, write-behind; problemas: stampede, invalidación, consistencia.
