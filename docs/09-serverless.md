# 09 · Serverless: Spring Cloud Function y AWS Lambda

`pricing-service` expone su lógica como **funciones** (`PricingFunctions`):

```java
@Bean Supplier<RefreshReport> refreshPrices(...)                     // job semanal
@Bean Function<EstimateRequest, PriceEstimate> estimatePrices(...)   // cotizar lista
```

El mismo código corre:
- **como microservicio** (lo que hacemos en local): `WeeklyPriceRefreshJob` llama a `refreshPrices` con `@Scheduled(cron = "0 0 6 * * MON")` y el controller usa `estimatePrices`;
- **como AWS Lambda**: agregando `spring-cloud-function-adapter-aws`, el handler genérico `org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest` ejecuta la función de `spring.cloud.function.definition`. **EventBridge Scheduler** la dispara los lunes (`cron(0 9 ? * MON *)` en UTC = 06:00 ART). En Lambda se desactiva el scheduler interno (`PRICING_SCHEDULER_ENABLED=false`).

Ver `infra/aws/template.yaml` (AWS SAM) como punto de partida.

## Cold start
Spring en Lambda tarda en arrancar. Mitigaciones: **SnapStart** (Java), GraalVM native image (Spring AOT), memoria suficiente, mantener la función chica.

## ¿Cuándo serverless?
Ideal para cargas **esporádicas o programadas** (este job corre 10 min por semana: pagar un servidor 24/7 no tiene sentido). Menos ideal para APIs con tráfico constante y baja latencia.

## Cron de Spring
`segundo minuto hora díaMes mes díaSemana` → `0 0 6 * * MON` (lunes 6:00). Diario: `0 0 6 * * *`. Con varias réplicas, usar **ShedLock** para que el job corra en una sola.
