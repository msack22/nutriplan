#!/usr/bin/env bash
# Genera una CA interna y un certificado por servicio para mTLS (SOLO DESARROLLO).
# Salida en infra/certs/out/ (ignorada por git). Ver docs/03-seguridad.md#mtls
set -euo pipefail
cd "$(dirname "$0")"; mkdir -p out; cd out
PASS=changeit
openssl req -x509 -newkey rsa:4096 -days 365 -nodes -keyout ca.key -out ca.crt -subj "/CN=NutriPlan Dev CA"
for svc in gateway recipe-service planner-service pricing-service; do
  openssl req -newkey rsa:2048 -nodes -keyout $svc.key -out $svc.csr -subj "/CN=$svc"
  printf "subjectAltName=DNS:%s,DNS:localhost\nextendedKeyUsage=serverAuth,clientAuth\n" "$svc" > $svc.ext
  openssl x509 -req -in $svc.csr -CA ca.crt -CAkey ca.key -CAcreateserial -days 365 -out $svc.crt -extfile $svc.ext
  openssl pkcs12 -export -in $svc.crt -inkey $svc.key -name $svc -out $svc.p12 -passout pass:$PASS
done
keytool -importcert -noprompt -alias nutriplan-ca -file ca.crt -keystore truststore.p12 -storetype PKCS12 -storepass $PASS
echo "Listo: $(pwd)"
