# Étape 1 : Construction
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# 1. Copier les fichiers de build d'abord (pour meilleur cache Docker)
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw

# 2. Télécharger les dépendances (sans les plugins problématiques)
RUN ./mvnw dependency:resolve

# 3. Copier le code source et builder
COPY src ./src
RUN ./mvnw clean package -DskipTests -Dcheckstyle.skip=true -Djacoco.skip=true

# Étape 2 : Exécution
FROM openjdk:17-slim
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# 5. Exposer le port et démarrer
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
