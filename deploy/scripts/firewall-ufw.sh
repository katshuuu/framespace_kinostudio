#!/usr/bin/env bash
# Правила брандмауэра для Linux-сервера (UFW).
# На macOS для учебного стенда используется привязка портов к 127.0.0.1
# в docker-compose — внешний периметр закрыт без UFW.
#
# Запускать от root на VPS: sudo ./firewall-ufw.sh
set -euo pipefail

if ! command -v ufw >/dev/null 2>&1; then
  echo "ufw не установлен. На Ubuntu/Debian: apt-get install -y ufw" >&2
  exit 1
fi

echo "Настраиваю UFW для FrameSpace..."
ufw default deny incoming
ufw default allow outgoing

# SSH — обязательно до включения firewall, иначе можно потерять доступ к VPS.
ufw allow OpenSSH || ufw allow 22/tcp

# Публикуем только reverse proxy (HTTP redirect + HTTPS).
# Прямой доступ к PostgreSQL и внутреннему порту Spring снаружи запрещён.
ufw allow 80/tcp comment 'FrameSpace HTTP redirect'
ufw allow 443/tcp comment 'FrameSpace HTTPS'

ufw --force enable
ufw status verbose

echo
echo "Готово. PostgreSQL и :8080 не должны быть открыты наружу."
echo "Проверьте: ss -tulpen | grep -E ':(5432|8080|443|80)\\s'"
