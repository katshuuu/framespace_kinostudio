# ---------- 1. Сборка приложения (Maven + JDK 21) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# Кеш ~/.m2 сохраняется между сборками: при обрыве сети повторный запуск продолжит скачивание
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

# ---------- 2. Запуск: JRE 21 + виртуальный экран ----------
# JavaFX — оконное приложение, поэтому в контейнере поднимается виртуальный экран (Xvfb),
# а его картинка отдаётся в браузер через noVNC: http://localhost:6080
# Debian вместо Ubuntu: у него заметно меньше индексы пакетов, apt-get update идёт быстрее.
FROM debian:bookworm-slim
COPY --from=eclipse-temurin:21-jre /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk PATH=/opt/java/openjdk/bin:$PATH

RUN apt-get -o Acquire::Retries=10 -o Acquire::Languages=none update \
    && apt-get -o Acquire::Retries=10 install -y --no-install-recommends \
        xvfb x11vnc novnc websockify openbox \
        libgtk-3-0 libxtst6 libxrender1 libxi6 libgl1 \
        fonts-dejavu-core fontconfig ca-certificates \
    && ln -s /usr/share/novnc/vnc.html /usr/share/novnc/index.html \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/target/kinostudio-crm.jar /app/app.jar
COPY docker/entrypoint.sh /app/entrypoint.sh
# sed убирает переводы строк Windows (CRLF), иначе скрипт не запустится в Linux-контейнере
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh && mkdir -p /exports

ENV DISPLAY=:99 \
    SCREEN_SIZE=1440x900x24 \
    EXPORT_DIR=/exports
EXPOSE 6080
ENTRYPOINT ["/app/entrypoint.sh"]
