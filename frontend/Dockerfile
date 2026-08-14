# syntax=docker/dockerfile:1

# ---- Stage 1: build (Node) ----
FROM node:20-alpine AS build
WORKDIR /build

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# ---- Stage 2: runtime (Nginx serving static files) ----
FROM nginx:1.27-alpine AS runtime

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --retries=10 --start-period=10s \
    # 127.0.0.1 e nao `localhost`: dentro do container `localhost` resolve primeiro para o IPv6
    # [::1], mas o nginx escuta apenas em IPv4 (`listen 80;` em nginx.conf). Com `localhost` o
    # healthcheck falha sempre com "connection refused" e o container aparece como unhealthy mesmo
    # servindo normalmente.
    CMD wget --no-verbose --tries=1 --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
