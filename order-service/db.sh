#!/bin/bash

# Auth-service'ten bağımsız Order veritabanı. Port .env'den beslenir.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "HATA: .env dosyasi bulunamadi!"
    exit 1
fi

while IFS='=' read -r key value; do
    # Yorum satirlarini atla
    [[ -z "$key" || "$key" =~ ^# ]] && continue
    key=$(echo "$key" | xargs)
    value=$(echo "$value" | xargs)
    export "$key=$value"
done < "$ENV_FILE"

CONTAINER="order_postgres"
IMAGE="postgres:17.5"
# DIKKAT: DB_PORT, DB_NAME, DB_PASSWORD artik .env'den otomatik devralinir.

case "$1" in
    start)
        docker run --name $CONTAINER \
            -e POSTGRES_USER=$DB_USER \
            -e POSTGRES_PASSWORD=$DB_PASSWORD \
            -e POSTGRES_DB=$DB_NAME \
            -p $DB_PORT:5432 -d $IMAGE
        echo "✅ Order PostgreSQL DB $DB_PORT portunda çalışıyor."
        ;;
    stop)    docker stop $CONTAINER ;;
    destroy)
        docker stop $CONTAINER 2>/dev/null || true
        docker rm $CONTAINER 2>/dev/null || true
        ;;
    reset)
        sh db.sh destroy && sh db.sh start
        ;;
    *)
        echo "Kullanım: ./db.sh {start|stop|destroy|reset}"
        ;;
esac
