#!/usr/bin/env bash
# Levanta infraestructura + 4 servicios Java + Angular en segundo plano. Logs en ./logs/
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; mkdir -p "$ROOT/logs"
docker compose -f "$ROOT/infra/docker-compose.yml" up -d
(cd "$ROOT/backend" && mvn -q -B install -DskipTests)
for svc in recipe-service planner-service pricing-service gateway; do
  (cd "$ROOT/backend/$svc" && nohup mvn -q -B spring-boot:run > "$ROOT/logs/$svc.log" 2>&1 &)
done
(cd "$ROOT/frontend" && npm install --silent && nohup npx ng serve > "$ROOT/logs/frontend.log" 2>&1 &)
echo "Arrancando... app en http://localhost:4200 (usuario demo / demo123). Logs: $ROOT/logs"
