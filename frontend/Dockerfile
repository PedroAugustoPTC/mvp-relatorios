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
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
