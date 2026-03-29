#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: .env file not found"
    exit 1
fi

while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" =~ ^# ]] && continue
    key=$(echo "$key" | xargs)
    value=$(echo "$value" | xargs)
    export "$key=$value"
done < "$ENV_FILE"

CONTAINER="auth_postgres"
IMAGE="postgres:17.5"

case "$1" in
    create)
        docker run --name $CONTAINER \
            -e POSTGRES_USER=$DB_USER \
            -e POSTGRES_PASSWORD=$DB_PASSWORD \
            -e POSTGRES_DB=$DB_NAME \
            -p $DB_PORT:5432 -d $IMAGE
        ;;
    start)   docker start $CONTAINER ;;
    stop)    docker stop $CONTAINER ;;
    status)  docker ps -a --filter name=$CONTAINER --format "table {{.ID}}\t{{.Status}}\t{{.Ports}}" ;;
    connect) docker exec -it $CONTAINER psql -U $DB_USER -d $DB_NAME ;;
    query)   docker exec -i $CONTAINER psql -U $DB_USER -d $DB_NAME -c "$2" ;;
    tables)  docker exec -i $CONTAINER psql -U $DB_USER -d $DB_NAME -c "\dt" ;;
    migrate)
        docker exec -i $CONTAINER psql -U $DB_USER -d $DB_NAME <<-SQL
            CREATE TABLE IF NOT EXISTS users (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                email VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                full_name VARCHAR(255) NOT NULL,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
            );
            CREATE TABLE IF NOT EXISTS refresh_tokens (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                token VARCHAR(512) UNIQUE NOT NULL,
                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
            );
            CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
            CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
SQL
        ;;
    clean)   docker exec -i $CONTAINER psql -U $DB_USER -d $DB_NAME -c "TRUNCATE refresh_tokens, users CASCADE;" ;;
    destroy)
        docker stop $CONTAINER 2>/dev/null || true
        docker rm $CONTAINER 2>/dev/null || true
        ;;
    reset)
        $0 destroy && $0 create && sleep 3 && $0 migrate
        ;;
    logs)    docker logs $CONTAINER --tail 50 ;;
    *)
        echo "Usage: ./db.sh <command>"
        echo ""
        echo "  create   Create container"
        echo "  start    Start container"
        echo "  stop     Stop container"
        echo "  status   Show status"
        echo "  connect  Attach to psql"
        echo "  query    Run SQL (./db.sh query \"SELECT...\")"
        echo "  tables   List tables"
        echo "  migrate  Create tables & indexes"
        echo "  clean    Delete all table data (truncate)"
        echo "  destroy  Delete container entirely"
        echo "  reset    Recreate container from scratch"
        echo "  logs     Tail real-time logs"
        ;;
esac
