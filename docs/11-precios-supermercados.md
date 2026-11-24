# 11 · Precios de supermercados

## Fuentes evaluadas

| Fuente | Tipo | Pros | Contras |
|---|---|---|---|
| **API de catálogo VTEX** (Día, Jumbo, Carrefour, Vea, Disco) | pública, JSON, sin auth: `/api/catalog_system/pub/products/search?ft=` | precios online actuales, links a producto | no oficial para terceros; usar con moderación |
| **Precios Claros – Base SEPA** (datos.gob.ar) | dataset oficial diario (zip/CSV) | oficial, miles de comercios | archivos grandes, hay que procesarlos; no es API REST |

Implementado: **VTEX** (`VtexCatalogAdapter`), una instancia por cadena configurada en `pricing.chains`. SEPA queda como mejora: sería otro *adapter* de `SupermarketCatalog` (ideal para un job batch / Lambda que descargue el zip del día).

## Scraping responsable
Una vez por semana, pausa de 150 ms entre requests, User-Agent identificable, 3 cadenas activas, circuit breaker por cadena, caché en base. Revisar los términos de uso de cada sitio antes de un uso no educativo.

## Cómo se estima
1. `ProductMatcher` elige el producto: debe contener las palabras del ingrediente, prefiere los que **empiezan** con él, descarta ruido ("huevo de pascua"), y a igual relevancia el más barato.
2. `PackageSizeParser` lee el envase del nombre ("1 L" → 1000 ml, "x 6 Un" → 6 u.).
3. `CostCalculator` calcula envases necesarios (680 ml de leche → 1 envase de 1 L).
4. Se suma por cadena y se informa cuántos productos no tienen precio. La UI marca la cadena **más barata** y la **más completa** por separado (comparar solo totales engaña).

Es una **estimación**: matcheo heurístico, compras por envase completo y sin promociones.
