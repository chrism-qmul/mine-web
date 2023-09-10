FROM python:3.10-alpine3.18
#COPY --from=build /usr/src/app/public/js/main.js /main.js
RUN pip install Flask Flask-SocketIO gevent gevent-websocket celery[redis]
#simple-websocket
VOLUME /app
WORKDIR /app
EXPOSE 5000
CMD ["python", "app.py"]
