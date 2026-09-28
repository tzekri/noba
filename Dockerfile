# Image unique : le backend Spring Boot sert aussi le frontend React (même domaine, pas de CORS,
# flux temps réel SSE sans proxy intermédiaire). Utilisée par Render (voir render.yaml).

# --- 1. Frontend ---
FROM node:20-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# --- 2. Backend (embarque le build du frontend dans /static) ---
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/dist ./src/main/resources/static
RUN ./mvnw -B -q clean package -DskipTests

# --- 3. Exécution ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
# Render fournit la variable PORT ; 8090 par défaut ailleurs.
EXPOSE 8090
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
