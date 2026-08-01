# ================================================
# Dockerfile para Render - Malva Pastry Shop
# Basado en la documentación oficial de Render
# https://docs.render.com/docker
# ================================================

# ================== CSS STAGE ==================
# Regenera el CSS de Tailwind desde las plantillas.
#
# El compilado tambien esta commiteado, para que `mvn spring-boot:run` y los
# tests funcionen sin Node. Pero produccion no puede depender de que alguien se
# haya acordado de correr `npm run css` antes de commitear: esta etapa lo vuelve
# a generar siempre, asi que una clase nueva en una plantilla no puede llegar al
# deploy sin su CSS.
FROM node:22-alpine AS css

WORKDIR /app

# Manifiestos primero: la capa de npm ci se cachea mientras no cambien.
COPY package.json package-lock.json tailwind.config.js ./
RUN npm ci

# Solo lo que Tailwind escanea (content globs) mas el CSS de entrada.
COPY src/main/css/ src/main/css/
COPY src/main/resources/templates/ src/main/resources/templates/
COPY src/main/resources/static/js/ src/main/resources/static/js/

RUN npm run css

# ================== BUILD STAGE ==================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar Maven wrapper y pom.xml primero (para cache de dependencias)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Dar permisos de ejecución al Maven Wrapper
RUN chmod +x mvnw

# Descargar dependencias (cacheado si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY src/ src/

# Pisar el CSS commiteado con el recién compilado (ver etapa `css`).
COPY --from=css /app/src/main/resources/static/css/admin.css src/main/resources/static/css/admin.css

# Compilar aplicación
RUN ./mvnw clean package -DskipTests -B

# ================== RUN STAGE ==================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar JAR desde etapa de build
COPY --from=builder /app/target/*.jar app.jar

# Usuario no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Variables de entorno para JVM
ENV JAVA_OPTS="-Xmx256m -Xms128m"

# Puerto expuesto (Render usa $PORT, default 10000)
EXPOSE 10000

# Comando de inicio con perfil de producción
# Render inyecta PORT automáticamente
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-10000} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar"]
