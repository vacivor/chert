#!/bin/sh
set -eu

java ${JAVA_OPTS:-} -jar /app/chert-server.jar &
APP_PID=$!

cleanup() {
  kill "$APP_PID" 2>/dev/null || true
}

trap cleanup INT TERM

nginx -g 'daemon off;' &
NGINX_PID=$!

EXIT_CODE=0

while kill -0 "$APP_PID" 2>/dev/null && kill -0 "$NGINX_PID" 2>/dev/null; do
  sleep 1
done

if ! kill -0 "$APP_PID" 2>/dev/null; then
  wait "$APP_PID" || EXIT_CODE=$?
elif ! kill -0 "$NGINX_PID" 2>/dev/null; then
  wait "$NGINX_PID" || EXIT_CODE=$?
fi

cleanup
kill "$NGINX_PID" 2>/dev/null || true
wait "$APP_PID" 2>/dev/null || true
wait "$NGINX_PID" 2>/dev/null || true

exit "$EXIT_CODE"
