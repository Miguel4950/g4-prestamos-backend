## Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Copiar wrapper y POM primero para cachear dependencias
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
 && ./mvnw -q -DskipTests dependency:go-offline

# Copiar código fuente y compilar
COPY src ./src
RUN ./mvnw -q -DskipTests clean package

## Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Opcional: flags de memoria via JAVA_OPTS
ENV JAVA_OPTS=""

# Copiar JAR generado
COPY --from=build /workspace/target/*.jar /app/app.jar

# Exponer por compatibilidad local (Render usa $PORT)
EXPOSE 8080

# Usar el puerto que asigna Render via $PORT
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]

