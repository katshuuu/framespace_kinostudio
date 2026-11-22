#!/usr/bin/env bash
# Генерирует самоподписанный TLS-сертификат для локального/учебного reverse proxy.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SSL_DIR="$ROOT/nginx/ssl"
DAYS="${1:-825}"

mkdir -p "$SSL_DIR"

if [[ -f "$SSL_DIR/framespace.crt" && -f "$SSL_DIR/framespace.key" ]]; then
  echo "TLS-сертификат уже существует: $SSL_DIR/framespace.crt"
  openssl x509 -in "$SSL_DIR/framespace.crt" -noout -subject -dates || true
  exit 0
fi

openssl req -x509 -nodes -newkey rsa:2048 -days "$DAYS" \
  -keyout "$SSL_DIR/framespace.key" \
  -out "$SSL_DIR/framespace.crt" \
  -subj "/C=RU/ST=Moscow/L=Moscow/O=FrameSpace/OU=SPO-KR3/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

chmod 600 "$SSL_DIR/framespace.key"
chmod 644 "$SSL_DIR/framespace.crt"

echo "Создан самоподписанный сертификат:"
openssl x509 -in "$SSL_DIR/framespace.crt" -noout -subject -dates
echo "Файлы: $SSL_DIR/framespace.crt , $SSL_DIR/framespace.key"
