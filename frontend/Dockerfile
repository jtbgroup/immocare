# Multi-stage build for Angular frontend

# Stage 1: Build
FROM node:18-alpine AS build
WORKDIR /app

# Copy package files and install dependencies (cached layer)
COPY package*.json ./
RUN npm ci

# Copy all source files (Angular needs angular.json, tsconfig.json, etc.)
COPY . .

# Build for production
RUN npm run build --configuration=production

# Stage 2: Serve with Nginx
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static files
RUN rm -rf ./*

# Copy built app from build stage
COPY --from=build /app/dist/immocare-frontend .

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
