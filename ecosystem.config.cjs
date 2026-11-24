// pm2: levanta los 4 microservicios Java de NutriPlan (igual que los backends Node del resto del stack).
//   pm2 start ecosystem.config.cjs     · pm2 logs nutriplan-planner     · pm2 restart nutriplan-gateway
// Cada microservicio es un proceso (una JVM) independiente: se reinicia/escala por separado.
// Requisitos: jars compilados (scripts/levantar-nutriplan.sh lo hace) e infraestructura Docker arriba.
const fs = require('fs');
const os = require('os');
const path = require('path');

// Host público (IP de esta Mac en la LAN) para el issuer de Keycloak: se lee de infra/.env
const envFile = path.join(__dirname, 'infra', '.env');
const vars = fs.existsSync(envFile)
  ? Object.fromEntries(fs.readFileSync(envFile, 'utf8').split('\n')
      .filter((l) => l.includes('=') && !l.startsWith('#')).map((l) => l.split('=').map((s) => s.trim())))
  : {};
const host = vars.NUTRIPLAN_HOST || 'localhost';

const java = path.join(os.homedir(), 'tools', 'jdk-21.0.13.1+2', 'Contents', 'Home', 'bin', 'java');
// Los secrets reales viven en infra/.env (ignorado por git); si no están, se usan los de desarrollo.
const pick = (...keys) => Object.fromEntries(keys.filter((k) => vars[k]).map((k) => [k, vars[k]]));
const commonEnv = {
  KEYCLOAK_ISSUER: `http://${host}:8180/realms/nutriplan`,
  ...pick('BFF_CLIENT_SECRET', 'PLANNER_CLIENT_SECRET', 'PRICING_CLIENT_SECRET'),
};

const service = (name, dir, xmx = '384m') => ({
  name: `nutriplan-${name}`,
  cwd: path.join(__dirname, 'backend', dir),
  script: java,
  args: [`-Xmx${xmx}`, '-XX:+UseSerialGC', '-jar', `target/${dir}-0.1.0-SNAPSHOT.jar`],
  interpreter: 'none',
  env: commonEnv,
  autorestart: true,
  exp_backoff_restart_delay: 5000, // si Docker todavía no arrancó, reintenta con backoff
  max_memory_restart: '700M',
  time: true,
});

module.exports = {
  apps: [
    service('recipe', 'recipe-service', '512m'),
    service('planner', 'planner-service'),
    service('pricing', 'pricing-service'),
    service('gateway', 'gateway'),
  ],
};
