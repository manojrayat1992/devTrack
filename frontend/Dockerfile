# Multi-stage Dockerfile for DevTrack Frontend
# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Cache package dependencies
COPY package.json ./
RUN npm install

ARG VITE_API_BASE_URL=http://localhost:8081/api
ARG VITE_KEYCLOAK_URL=http://localhost:8080
ARG VITE_KEYCLOAK_REALM=devtrack
ARG VITE_KEYCLOAK_CLIENT_ID=devtrack-frontend

ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID

# Copy source and compile production bundle
COPY . .
RUN npm run build

# Stage 2: Minimal Nginx Runtime
FROM nginx:1.27-alpine

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration for SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
