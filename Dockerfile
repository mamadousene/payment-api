# Dockerfile
FROM eclipse-temurin:17-jdk-alpine as builder

# Répertoire de travail
WORKDIR /app

# Copier les fichiers Maven/Gradle
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Télécharger les dépendances
RUN ./mvnw dependency:go-offline

# Copier le code source
COPY src ./src

# Construire l'application
RUN ./mvnw clean package -DskipTests

# Image finale optimisée
FROM eclipse-temurin:17-jre-alpine

# Créer utilisateur non-root
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Répertoire de travail
WORKDIR /app

# Copier le JAR depuis le builder
COPY --from=builder /app/target/*.jar app.jar

# Exposer le port
EXPOSE 8080

# Variables d'environnement
ENV SPRING_PROFILES_ACTIVE=docker

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]

# Labels pour la documentation
LABEL maintainer="votre-email@example.com"
LABEL description="Payment API Spring Boot Application"
LABEL version="1.0.0"