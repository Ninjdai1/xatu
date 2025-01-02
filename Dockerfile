FROM openjdk:21-slim

ENV JAVA_HOME=/usr/local/openjdk-21
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /app

COPY target/bot.jar /app/bot.jar

CMD ["java", "-jar", "bot.jar"]
