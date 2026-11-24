# 03 · Seguridad en microservicios

Este documento recorre los temas de la guía *"Security in Microservices — Spring Security, JWT, OAuth2, Keycloak & mTLS"* y dónde está cada uno en el código.

## 1. Fundamentos

- **Autenticación**: ¿quién sos? (Keycloak verifica usuario y contraseña).
- **Autorización**: ¿qué podés hacer? (roles `USER`/`ADMIN`/`SERVICE`, y además "¿este plan es tuyo?").
- **OAuth2**: protocolo de **autorización delegada**: una app obtiene un *access token* para llamar APIs.
- **OpenID Connect (OIDC)**: capa de **identidad** sobre OAuth2. Agrega el *ID token* (quién es el usuario) y el endpoint `userinfo`.

## 2. JWT

Un JWT es `header.payload.signature` en Base64URL:

```json
// header
{ "alg": "RS256", "kid": "abc123" }
// payload (claims) — así se ve un token de NutriPlan
{ "iss": "http://localhost:8180/realms/nutriplan", "sub": "3cc479ca-...", "aud": "nutriplan-api",
  "exp": 1795556500, "azp": "nutriplan-bff", "realm_access": { "roles": ["USER"] }, "scope": "openid profile email" }
```

- Firmado con **RS256** (clave privada de Keycloak). Los servicios verifican con la **clave pública** que descargan del endpoint JWKS → no necesitan llamar a Keycloak en cada request (stateless).
- **Access token** (5 min, va a las APIs) · **Refresh token** (renueva el access sin re-login) · **ID token** (identidad, lo consume el cliente, nunca se manda a una API).
- Validaciones en `ResourceServerSecurityAutoConfiguration` (módulo `common`): firma, `exp`/`nbf`, `iss` y **`aud`** (que el token sea para `nutriplan-api`, evita aceptar tokens emitidos para otra app del mismo Keycloak).
- Un JWT **no se puede revocar** antes de expirar: por eso vida corta (5 min) + refresh token.

## 3. Spring Security

| Concepto | Dónde |
|---|---|
| `SecurityFilterChain` | `common/.../ResourceServerSecurityAutoConfiguration` (servicios) y `gateway/SecurityConfig` |
| Resource Server JWT | `oauth2ResourceServer(o -> o.jwt(...))` |
| Conversión de roles | `KeycloakRealmRoleConverter`: `realm_access.roles` → `ROLE_USER`, `ROLE_ADMIN` |
| `AuthenticationManager` / `UserDetailsService` / `PasswordEncoder` | **no los necesitamos**: la autenticación con contraseña la hace Keycloak. Se usarían en una app con login propio (formLogin + BCrypt). Saber explicarlo es clave. |
| Starter propio | `common` registra la configuración como **auto-configuration** (`AutoConfiguration.imports`), igual que los starters oficiales, y la ordena *antes* de la de Spring Boot |

## 4. OAuth2 + Keycloak

### Authorization Code + PKCE (login de usuarios)

1. Angular navega a `/oauth2/authorization/keycloak` (gateway).
2. El gateway genera `code_verifier` (aleatorio) y `code_challenge = SHA256(verifier)`, y redirige a Keycloak con el challenge.
3. El usuario se loguea en Keycloak (la app nunca ve la contraseña).
4. Keycloak redirige con un `code`. El gateway lo canjea por tokens enviando el `code_verifier` + su client secret.
5. PKCE evita que un `code` interceptado sirva: sin el verifier no se puede canjear.

### Client Credentials (máquina a máquina)

`planner-service` y `pricing-service` se autentican como **ellos mismos** (service accounts con rol `SERVICE`). Ver `ClientsConfig` y `RecipeCatalogClient`: `OAuth2ClientHttpRequestInterceptor` pide el token, lo cachea y lo renueva.

### SSO y gestión centralizada

Toda la identidad vive en Keycloak: registro, recuperación de contraseña, fuerza bruta (`bruteForceProtected`), roles. Si mañana se suma otra app al realm, el usuario entra con la misma sesión (SSO). El logout del gateway es **RP-initiated**: cierra la sesión local y la de Keycloak.

## 5. Seguridad de API y métodos

- **RBAC** por URL: `/internal/** → hasRole('SERVICE')`, `/actuator/** → ADMIN`.
- **Method security**: `@PreAuthorize("hasRole('ADMIN')")` en `RecipeAdminController` y en el refresh de precios.
- **Autorización a nivel de objeto (anti-IDOR / BOLA, OWASP API #1)**: `PlanService.owned()` verifica que el plan sea del usuario del token y responde **404** (no 403) para no revelar que el recurso existe. Test: `PlanControllerSecurityTest`.
- **No hay `userId` en las URLs del perfil**: se toma del JWT (`@AuthenticationPrincipal`).
- **Validación de entrada**: Bean Validation (`@Valid`, `@Min`, `@Size`...) → 400 con ProblemDetail.

## 6. Seguridad entre microservicios

- **Token relay** (gateway → servicio): `TokenRelayFilterFunctions.tokenRelay()`.
- **Token propagation** (planner → recipe): interceptor que copia el JWT del usuario.
- **Client credentials** (planner → pricing, pricing → recipe).
- **Defensa en profundidad**: aunque el gateway ya autenticó, **cada servicio valida el JWT de nuevo** (zero trust). El gateway no enruta `/internal/**`.

### mTLS (mutual TLS)

En TLS normal solo el servidor presenta certificado. En **mTLS ambos** lo hacen: el servicio llamado verifica que quien llama tenga un certificado firmado por nuestra CA interna. Protege la red interna aunque alguien robe un token.

Guía para activarlo (perfil `mtls`, **no habilitado por defecto ni probado end-to-end en este repo**):

```bash
./infra/certs/generate-certs.sh   # CA propia + un keystore PKCS12 por servicio + truststore
```

```yaml
# application-mtls.yml (servidor)
spring.ssl.bundle.jks.server:
  keystore: { location: file:infra/certs/out/recipe-service.p12, password: changeit, type: PKCS12 }
  truststore: { location: file:infra/certs/out/truststore.p12, password: changeit, type: PKCS12 }
server:
  ssl: { bundle: server, client-auth: need }   # need = el cliente DEBE presentar certificado
```

Del lado cliente se usa el mismo bundle en el `RestClient` (`RestClientSsl` / `ClientHttpRequestFactoryBuilder` con `HttpClientSettings`). En Kubernetes esto normalmente lo resuelve un **service mesh** (Istio/Linkerd) sin tocar código.

## 7. Buenas prácticas de producción

| Práctica | En el proyecto |
|---|---|
| Menor privilegio | usuario MySQL por servicio; rol SERVICE solo para `/internal`; scopes mínimos |
| Secretos | nada real en git; variables de entorno con default de dev (`${PLANNER_CLIENT_SECRET:...}`); en AWS → Secrets Manager |
| CORS | innecesario: Angular y API comparten origen vía gateway/proxy (menos superficie) |
| CSRF | activo en el gateway (usa cookies): `csrf.spa()` + `withXsrfConfiguration` en Angular. Desactivado en los servicios (stateless, token en header) |
| Cookies | `HttpOnly`, `SameSite=Lax`, `Secure` en producción |
| Headers | CSP, `X-Frame-Options: DENY`, `X-Content-Type-Options`, HSTS, Referrer-Policy |
| Errores | ProblemDetail sin stack traces; el detalle va al log |
| Audit log | `AuditLoggingFilter`: usuario, método, path, status, duración + **correlation id** propagado por el gateway |
| Monitoreo | Actuator + Micrometer + `/actuator/prometheus`, health probes (liveness/readiness) |
| Deserialización segura | caché Redis con serializador JSON **tipado** (sin default typing) |

## ¿Por qué BFF y no tokens en Angular?

Con una SPA que guarda tokens (localStorage / memoria), un XSS puede robarlos. Con **BFF**, el navegador solo tiene una cookie `HttpOnly` que JavaScript no puede leer; los tokens viven en la sesión del servidor (Redis). Es la recomendación actual del borrador *OAuth 2.0 for Browser-Based Apps* de la IETF.
