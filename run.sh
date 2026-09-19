#!/usr/bin/env bash
# MailNotifier'ı .env dosyasındaki ayarlarla başlatır.
#   ./run.sh
set -euo pipefail

cd "$(dirname "$0")"

if [ -f .env ]; then
    set -a
    # shellcheck disable=SC1091
    source .env
    set +a
    echo "✓ .env yüklendi"
else
    echo "⚠ .env bulunamadı. Oluşturmak için: cp .env.example .env"
fi

if [ -z "${GOOGLE_CLIENT_ID:-}" ] || [ -z "${GOOGLE_CLIENT_SECRET:-}" ]; then
    echo "✗ GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET tanımlı değil — Google ile giriş çalışmaz."
    exit 1
fi

if [ -n "${AI_API_KEY:-}" ]; then
    echo "✓ AI_API_KEY tanımlı — NLP + AI anlamsal analiz aktif"
else
    echo "⚠ AI_API_KEY tanımlı değil — yalnızca yerel NLP çalışacak"
fi

if ! docker compose ps --status running postgres 2>/dev/null | grep -q postgres; then
    echo "→ PostgreSQL başlatılıyor..."
    docker compose up -d postgres
fi

exec ./mvnw spring-boot:run
