# 02 · Infraestructura local

## Servicios de `infra/docker-compose.yml`

| Contenedor | Puerto host | Para qué |
|---|---|---|
| mysql:8.4 | 3307 | 3 bases (recipes_db, planner_db, pricing_db). `init.sql` crea bases y usuarios |
| keycloak:26.4 | 8180 | Identity Provider. Importa `infra/keycloak/nutriplan-realm.json` al arrancar |
| redis:8 | 6379 | sesiones del gateway + caché del planner |
| apache/kafka:4.1 | 9092 | broker en modo **KRaft** (sin ZooKeeper) |
| kafka-ui | 8090 | ver tópicos, mensajes y consumer groups |

Con `--profile apps` también construye y levanta los 4 servicios Java (Dockerfile multi-stage en `backend/Dockerfile`).

## Realm de Keycloak (`nutriplan`)

| Cliente | Tipo | Flujo | Uso |
|---|---|---|---|
| `nutriplan-bff` | confidencial | Authorization Code + PKCE | login del gateway (BFF) |
| `nutriplan-swagger` | público | Authorization Code + PKCE | botón *Authorize* de Swagger UI |
| `planner-service` | confidencial | Client Credentials | planner → pricing |
| `pricing-service` | confidencial | Client Credentials | pricing → recipe `/internal` |
| `nutriplan-dev-cli` | público | Password (ROPC) ⚠️ | **solo dev**: `scripts/token.sh` para probar con curl |

Roles de realm: `USER` (por defecto al registrarse), `ADMIN`, `SERVICE` (asignado a los service accounts). Todos los clientes tienen un *audience mapper* que agrega `nutriplan-api` al claim `aud`; los servicios rechazan tokens sin esa audiencia.

> Los secrets del realm (`*-dev-secret-change-me`) son de desarrollo. En producción se generan en Keycloak y se inyectan por variable de entorno / AWS Secrets Manager.

## Datos de recetas

`recipe-service` al arrancar con la base vacía importa el primer archivo que exista de `nutriplan.import.paths`:

1. `data/private/recipes-export.json` → recetario real (privado, **ignorado por git**)
2. `data/sample/recipes-sample.json` → muestra pública con el mismo formato

Después genera los embeddings (Ollama). Si Ollama no está, usá `AI_PROVIDER=hashing` (embeddings determinísticos sin IA).

Para completar la nutrición faltante con el LLM (lento en CPU: ~1-3 min por receta con qwen2.5:7b; para algo más rápido cambiá `nutriplan.ai.ollama.chat-model` a `qwen2.5:1.5b`):

```bash
export TOKEN=$(./scripts/token.sh admin admin123)
curl -X POST -H "Authorization: Bearer $TOKEN" localhost:8081/api/admin/recipes/estimate-nutrition
```

## Probar la API con curl

```bash
export TOKEN=$(./scripts/token.sh demo demo123)
curl -H "Authorization: Bearer $TOKEN" "localhost:8081/api/recipes/semantic-search?query=algo%20liviano%20con%20pollo"
curl -H "Authorization: Bearer $TOKEN" localhost:8082/api/profile
```

## Problemas comunes

- **Keycloak no arranca**: `docker logs nutriplan-keycloak-1`. Si cambiás el realm JSON, recreá el contenedor (`up -d --force-recreate keycloak`): la importación ignora realms existentes.
- **401 en los servicios**: el `iss` del token debe ser exactamente `http://localhost:8180/realms/nutriplan`.
- **Puerto 3306 ocupado**: por eso MySQL expone 3307.
