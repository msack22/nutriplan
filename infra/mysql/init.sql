-- Database-per-service: cada microservicio es dueño de su esquema.
-- Ningún servicio lee tablas de otro: se comunican por API REST o eventos Kafka.
-- ⚠️ Passwords solo para desarrollo local.
CREATE DATABASE IF NOT EXISTS recipes_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS planner_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS pricing_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Principio de menor privilegio: un usuario por servicio, solo con acceso a SU base.
CREATE USER IF NOT EXISTS 'recipe_svc'@'%'  IDENTIFIED BY 'recipe_dev_pw';
CREATE USER IF NOT EXISTS 'planner_svc'@'%' IDENTIFIED BY 'planner_dev_pw';
CREATE USER IF NOT EXISTS 'pricing_svc'@'%' IDENTIFIED BY 'pricing_dev_pw';
GRANT ALL PRIVILEGES ON recipes_db.* TO 'recipe_svc'@'%';
GRANT ALL PRIVILEGES ON planner_db.* TO 'planner_svc'@'%';
GRANT ALL PRIVILEGES ON pricing_db.* TO 'pricing_svc'@'%';
FLUSH PRIVILEGES;
