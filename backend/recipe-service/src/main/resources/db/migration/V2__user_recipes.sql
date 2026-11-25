-- Recetas creadas por usuarios: owner_id = "sub" del JWT. NULL = receta del catálogo (visible para todos).
ALTER TABLE recipe ADD COLUMN owner_id VARCHAR(64) NULL;
CREATE INDEX idx_recipe_owner ON recipe (owner_id);
