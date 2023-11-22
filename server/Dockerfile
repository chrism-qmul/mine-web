FROM python:3.10
#COPY --from=build /usr/src/app/public/js/main.js /main.js
RUN pip install Flask Flask-SocketIO pip install Flask-Session gevent gevent-websocket celery[redis] openai numpy openai[datalib]
#simple-websocket
VOLUME /app
WORKDIR /app
EXPOSE 5000
CMD ["python", "app.py"]
