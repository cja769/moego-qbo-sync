#!/bin/sh
set -eu

if [ "${SYNC_RENDER_WORKER:-false}" = true ]; then
    export SYNC_DATA_DIRECTORY=/data/sync
    install -d -m 0700 -o app -g app "$SYNC_DATA_DIRECTORY"

    if [ "${SYNC_MOCK_ENABLED:-true}" = true ]; then
        SYNC_DATA_DIRECTORY="$SYNC_DATA_DIRECTORY/mock" \
            setpriv --reuid=app --regid=app --init-groups java -jar /app/app.jar
        echo "Fixture run complete. Worker awaiting live configuration."
        exec setpriv --reuid=app --regid=app --init-groups sleep infinity
    fi

    case "${SYNC_CRON:-}" in
        ''|-) echo "Set SYNC_CRON to keep the Render worker running." >&2; exit 1 ;;
    esac
    case "${QBO_REALM_ID:-}" in
        ''|*[!0-9]*) echo "Set a numeric QBO_REALM_ID." >&2; exit 1 ;;
    esac

    token_file="$SYNC_DATA_DIRECTORY/qbo-$QBO_REALM_ID.token"
    if [ ! -e "$token_file" ] && [ -s /etc/secrets/qbo-refresh-token ]; then
        install -m 0600 -o app -g app /etc/secrets/qbo-refresh-token "$token_file"
    fi
    if [ ! -s "$token_file" ]; then
        echo "The QuickBooks refresh token has not been seeded." >&2
        exit 1
    fi
fi

exec setpriv --reuid=app --regid=app --init-groups java -jar /app/app.jar "$@"
