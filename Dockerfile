FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN apk add --no-cache nginx

COPY docker/standalone/nginx.conf /etc/nginx/http.d/default.conf
COPY docker/standalone/entrypoint.sh /entrypoint.sh
COPY chert-server/build/libs/*.jar /app/chert-server.jar
COPY chert-console/dist /usr/share/nginx/html

RUN chmod +x /entrypoint.sh \
    && mkdir -p /run/nginx

EXPOSE 80

ENTRYPOINT ["/entrypoint.sh"]
