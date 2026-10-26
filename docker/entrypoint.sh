#!/bin/sh
# Запускает виртуальный экран, VNC + noVNC (доступ из браузера) и само JavaFX-приложение.
set -e

Xvfb "$DISPLAY" -screen 0 "$SCREEN_SIZE" -nolisten tcp &
sleep 2
openbox &
x11vnc -display "$DISPLAY" -forever -shared -nopw -localhost -rfbport 5900 -quiet &
websockify --web /usr/share/novnc 6080 localhost:5900 >/dev/null 2>&1 &

echo "Откройте в браузере: http://localhost:6080  (кнопка «Подключиться»)"

# prism.order=sw — программная отрисовка (в контейнере нет видеокарты)
exec java -Dprism.order=sw -Dfile.encoding=UTF-8 -jar /app/app.jar
