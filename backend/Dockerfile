# Estágio de build
FROM azul/zulu-openjdk:21-latest AS build
RUN apt-get update && apt-get install -y maven

WORKDIR /app

# Copia o arquivo de build do Maven e baixa as dependências para aproveitar o cache do Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código fonte e compila a aplicação
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio de execução
FROM azul/zulu-openjdk:17-jre

WORKDIR /app

# Copia o arquivo JAR compilado do estágio de build
COPY --from=build /app/target/*.jar app.jar

# Configura variáveis de ambiente para a JVM
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV TZ=America/Sao_Paulo
ENV LANG=pt_BR.UTF-8

# Usuário não-root para segurança
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup appuser
USER appuser

# Executável (Project Loom está habilitado via parâmetros no POM)
ENTRYPOINT exec java $JAVA_OPTS \
   -XX:+UseContainerSupport \
   -XX:MaxRAMPercentage=75.0 \
   --enable-preview \
   -jar app.jar

EXPOSE 8888

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8888/actuator/health || exit 1