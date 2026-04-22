# Docker

## Build image

Build backend jar and frontend dist first:

```bash
./gradlew clean :chert-server:bootJar -x test
cd chert-console && npm ci && npm run build && cd ..
```

Then build the standalone image:

```bash
docker build -t chert:latest .
```

This image contains:

- `chert-server` Spring Boot app
- `chert-console` frontend bundle
- `nginx` serving the frontend and proxying `/api/*` to the local backend process

The Dockerfile assumes these artifacts already exist:

- `chert-server/build/libs/*.jar`
- `chert-console/dist`

## Run with Docker Compose

```bash
./gradlew :chert-server:bootJar
cd chert-console && npm ci && npm run build && cd ..
docker compose up --build
```

Services:

- Chert: `http://localhost:8080`
- Postgres: `localhost:5432`

## Notes

- The standalone container starts both `nginx` and the Spring Boot server.
- Frontend is served by Nginx and proxies `/api/*` to `127.0.0.1:8080` inside the container.
- Backend reads database settings from `CHERT_DB_URL`, `CHERT_DB_USERNAME`, `CHERT_DB_PASSWORD`.
- `docker-compose.yml` enables `CHERT_JPA_DDL_AUTO=update` for container startup convenience.
