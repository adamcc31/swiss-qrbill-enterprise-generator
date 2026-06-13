# ==========================================
# Stage 1: Build Angular Application
# ==========================================
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package-lock.json and package.json to leverage Docker layer caching
COPY package*.json ./
RUN npm ci

# Copy source code and config files
COPY . .

# Build for production
RUN npm run build -- --configuration=production

# ==========================================
# Stage 2: Serve Statically using Nginx
# ==========================================
FROM nginx:alpine

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy production bundle assets from builder
COPY --from=builder /app/dist/swiss-qrbill-frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
