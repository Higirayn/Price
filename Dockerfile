
FROM openjdk:11-jre-slim

WORKDIR /app

COPY build/libs/untitled2-1.0-SNAPSHOT.jar app.jar

RUN mkdir -p logs

EXPOSE 8080

ENV DB_HOST=localhost
ENV DB_PORT=5432
ENV DB_NAME=price_service
ENV DB_USER=postgres
ENV DB_PASSWORD=password

CMD ["java", "-jar", "app.jar"] 