# Seguridad

NutriPlan es un **proyecto de portafolio / aprendizaje** pensado para correr en una máquina local o una red doméstica. No está desplegado en internet.

## Credenciales de este repositorio

Todos los valores que aparecen en el código son **de desarrollo** y están marcados como tales (`*-dev-secret-change-me`, `*_dev_only`, `demo123`). No hay secretos reales en el repo ni en su historial.

- Los secrets del realm de Keycloak usan *placeholders* (`${BFF_CLIENT_SECRET}`, `${NUTRIPLAN_ADMIN_PASSWORD}`...) que se resuelven con variables de entorno.
- Los valores reales de cada instalación van en `infra/.env` (ignorado por git; ver `.env.example`).
- Un hook de *pre-commit* y el *secret scanning / push protection* de GitHub bloquean secretos en commits.

## Superficie expuesta (instalación local)

Solo dos puertos escuchan en la red: **4200** (la app) y **8180** (pantalla de login de Keycloak). MySQL, Redis (sesiones), Kafka, Kafka UI y los 4 microservicios escuchan únicamente en `127.0.0.1`.

Detalle de los controles (OAuth2 + PKCE, BFF, validación de JWT con `aud`, RBAC, anti-IDOR, CSRF, headers, audit log, menor privilegio): [docs/03-seguridad.md](docs/03-seguridad.md).

## Antes de exponerlo a internet

- Keycloak en modo producción (`start` en lugar de `start-dev`) con HTTPS y base de datos propia.
- Cookies `Secure`, HTTPS en todo el tráfico (y mTLS entre servicios, ver docs).
- Deshabilitar el cliente `nutriplan-dev-cli` (password grant, solo para pruebas).
- Rotar todos los secrets y guardarlos en un gestor (AWS Secrets Manager, Vault).

## Reportar un problema

Abrí un *issue* (o, si es sensible, contactame por privado desde mi perfil de GitHub).
