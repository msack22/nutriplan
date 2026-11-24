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
