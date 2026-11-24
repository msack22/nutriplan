#!/usr/bin/env bash
# Obtiene un access token de Keycloak para probar la API con curl (SOLO DESARROLLO).
#   export TOKEN=$(./scripts/token.sh demo demo123)
#   curl -H "Authorization: Bearer $TOKEN" localhost:8081/api/recipes
USER=${1:-demo}; PASS=${2:-demo123}
curl -s -d grant_type=password -d client_id=nutriplan-dev-cli -d username="$USER" -d password="$PASS" \
  http://localhost:8180/realms/nutriplan/protocol/openid-connect/token |
  python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])"
