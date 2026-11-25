# 08 · IA: embeddings y búsqueda semántica

## Embeddings
Un *embedding* convierte texto en un vector (768 números con `nomic-embed-text`) de forma que textos con **significado** parecido quedan cerca. Por eso *"algo liviano con pollo para la cena"* encuentra *"Ensalada griega con pollo a la parrilla"* sin compartir palabras exactas.

Flujo (`recipe-service`):
1. `Recipe.toEmbeddingText()` arma "título + categoría + ingredientes + etiquetas".
2. `OllamaEmbeddingGenerator` llama `POST /api/embed` de Ollama (local, gratis). Usa prefijos `search_document:` / `search_query:` que recomienda el modelo.
3. Se guarda en `recipe_embedding` (BLOB) — MySQL es la fuente de verdad.
4. `InMemoryRecipeVectorIndex` carga los vectores y busca por **similitud coseno** (fuerza bruta: con cientos de recetas son microsegundos).

¿Por qué no una base vectorial? Para 133 recetas no hace falta. El puerto `RecipeVectorIndex` permite cambiar a **pgvector, Qdrant, OpenSearch o MySQL HeatWave** (índices ANN tipo HNSW) sin tocar los casos de uso.

## Uso en la generación de planes
`POST /api/recipes/candidates` filtra por comida/tags/ingredientes excluidos y **rankea por similitud** con la preferencia del usuario; la estrategia del planner suma ese score en su función de costo.

## Estimación de nutrición con LLM
Solo ~17 recetas traían tabla nutricional. `OllamaNutritionEstimator` le pide a `qwen2.5:7b` los macros por porción con **structured outputs** (JSON Schema en `format`), `temperature: 0`, y valida rangos plausibles (los LLM alucinan). Se guarda como `NutritionSource.ESTIMATED` y la UI lo marca "IA". Un admin puede corregir (`MANUAL`).

## Modo sin IA
`AI_PROVIDER=hashing` usa `HashingEmbeddingGenerator` (bag-of-words con feature hashing): determinístico, ideal para tests/CI.

## Borradores de recetas con IA (trabajo asíncrono)

`POST /api/recipes/drafts` recibe los ingredientes que tiene el usuario y responde **202 Accepted** con un id. Generar una receta con `qwen2.5:7b` en CPU tarda 2-4 minutos, así que:

- el trabajo corre en un **virtual thread** y el frontend hace **polling** (`GET /api/recipes/drafts/{id}` cada 3 s con `timer + switchMap + takeWhile`);
- **`LlmGate`: una sola cola hacia el LLM con prioridad.** Problema real encontrado: sin GPU, Ollama atendía 3-4 pedidos en paralelo (estimación de fondo + borradores) y cada uno pasaba de ~1,5 min a ~10 min, superando el timeout. Ahora va un pedido a la vez y los del usuario pasan antes que los de fondo (test: `interactiveCallsGoBeforeQueuedBackgroundWork`);
- timeout del cliente HTTP a Ollama: 10 min (con 2 min el pedido se cortaba y fallaba);
- cada usuario puede tener un borrador en curso;
- el borrador **no se guarda**: el usuario lo revisa y lo confirma con el formulario (`POST /api/recipes`).

Se probó también `qwen2.5:1.5b`: tarda ~1,5 min pero inventa cantidades ("2 cucharadas de zapallito"); se priorizó calidad.

## Recetas propias y privacidad

`recipe.owner_id` = `sub` del JWT (null = catálogo). La regla `Recipe.isVisibleTo(userId)` se aplica en el detalle, el listado (`RecipeSpecifications.visibleTo`), la búsqueda semántica, las candidatas del plan automático y el batch de la lista de compras. Para otro usuario una receta privada **no existe** (404).

## Extracción de recetas desde PDFs (OCR)

Varios PDFs del recetario eran imágenes. Se extrajo el texto con **Apple Vision** (OCR local, `VNRecognizeTextRequest` en español) y se cruzaron los títulos contra el catálogo: 8 recetas no estaban en el export y se agregaron (archivo privado en `data/private/`, que el importador lee completo).
