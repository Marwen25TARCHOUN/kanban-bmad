# syntax=docker/dockerfile:1.7
#
# Multistage frontend build — AD-15.
# Build on node:22-alpine, serve on nginx:1.27-alpine.
# Target image size: <50 MB.

# ---- Build stage ----
FROM node:22-alpine AS build
WORKDIR /workspace

# Cache npm deps when only source changes.
COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . ./
RUN npm run build -- --configuration production

# ---- Runtime stage ----
FROM nginx:1.27-alpine AS runtime

# Angular 17+ emits to dist/<app-name>/browser/. If your Angular app name is not
# "kanban-frontend", override with: --build-arg DIST_DIR=dist/<your-name>/browser
ARG DIST_DIR=dist/kanban-frontend/browser

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/${DIST_DIR}/ /usr/share/nginx/html/

EXPOSE 80

# nginx:alpine already logs access/errors to stdout/stderr.
