# Multi-stage Dockerfile for DevTrack Frontend
# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Cache package dependencies
COPY package.json ./
RUN npm install

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
