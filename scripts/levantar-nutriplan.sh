#!/usr/bin/env zsh
# Levanta el backend de NutriPlan: Docker (Colima) + infraestructura + 4 microservicios en pm2.
# El frontend se abre aparte (tab de iTerm en levanta-todo.command):  cd frontend && npx ng serve
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$HOME/tools/env.sh"                     # Java 21, Maven, Node 24, Docker CLI, Colima
export NVM_DIR="$HOME/.nvm"; [ -s "$NVM_DIR/nvm.sh" ] && source "$NVM_DIR/nvm.sh"
source "$HOME/tools/env.sh"                     # de nuevo: que nuestro Node quede primero en el PATH

echo "▶ NutriPlan: Docker (Colima)..."
colima status >/dev/null 2>&1 || colima start --cpu 4 --memory 6 --vm-type vz

echo "▶ NutriPlan: infraestructura (MySQL, Keycloak, Kafka, Redis)..."
docker compose -f "$ROOT/infra/docker-compose.yml" up -d

# Compila los jars si faltan o si hay código más nuevo que el último build
NEED_BUILD=0
for svc in recipe-service planner-service pricing-service gateway; do
  JAR="$ROOT/backend/$svc/target/$svc-0.1.0-SNAPSHOT.jar"
  if [[ ! -f "$JAR" ]] || [[ -n "$(find "$ROOT/backend" -name '*.java' -newer "$JAR" -o -name '*.yml' -newer "$JAR" | head -1)" ]]; then
    NEED_BUILD=1
  fi
done
if [[ "$NEED_BUILD" == "1" ]]; then
  echo "▶ NutriPlan: compilando microservicios (mvn package)..."
  (cd "$ROOT/backend" && mvn -q -B package -DskipTests) || { echo "❌ Falló la compilación"; exit 1; }
fi

echo "▶ NutriPlan: microservicios en pm2..."
if pm2 describe nutriplan-gateway >/dev/null 2>&1; then
  pm2 restart "$ROOT/ecosystem.config.cjs" --update-env
else
  pm2 start "$ROOT/ecosystem.config.cjs"
fi
pm2 save --force >/dev/null 2>&1 || true
echo "  ✅ recipe :8081 · planner :8082 · pricing :8083 · gateway :8080"
