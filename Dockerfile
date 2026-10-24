# ==========================================================
# DOCKERFILE - Cómo construye y arranca Render la app.
# Dos fases:
#   1) "build": con el JDK y Maven compila el proyecto y saca el .jar
#      (sin tests: los nuestros necesitan la base de datos de local).
#   2) "final": una imagen pequeña solo con Java para ejecutar ese .jar.
# Las credenciales NO van aquí: Render las pasa como variables de entorno.
# ==========================================================

# ---------- 1) Compilar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Primero solo lo necesario para descargar dependencias: así Docker reutiliza
# esta capa mientras no cambie el pom.xml y los siguientes builds van más rápido
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B package -DskipTests

# ---------- 2) Ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Sin permisos de administrador dentro del contenedor (por seguridad)
RUN useradd --system --uid 1001 gymtracker
COPY --from=build /app/target/gymtracker-*.jar app.jar
USER gymtracker

# Render gratis da 512 MB de memoria: Java usa como mucho el 70 %, con el
# recolector de basura más ligero y arranque rápido (importante tras "dormirse")
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
