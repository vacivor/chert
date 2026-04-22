FROM node:24-alpine AS builder

WORKDIR /app

COPY chert-console/package.json ./
RUN npm install

COPY chert-console .

RUN npm run build

FROM nginx:1.29-alpine

COPY chert-console/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
