# ---- build stage: produce the static frontend bundle ----
FROM node:20-alpine AS build
WORKDIR /web
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- serve stage: Caddy serves the static site and (via mounted Caddyfile) proxies /api ----
FROM caddy:2-alpine
COPY --from=build /web/dist /srv
