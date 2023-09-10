#FROM node:20-alpine
FROM clojure:temurin-20-lein-2.10.0-bullseye
RUN apt-get update && apt-get install -y ca-certificates curl gnupg
RUN mkdir -p /etc/apt/keyrings
RUN curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
ENV NODE_MAJOR=20
RUN echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_$NODE_MAJOR.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list
RUN apt-get update && apt-get install nodejs -y
RUN mkdir -p /app
COPY package*.json /app
WORKDIR /app
VOLUME /app
EXPOSE 8080
EXPOSE 9630
EXPOSE 9000
RUN ls
RUN npm install
CMD ["npx", "shadow-cljs", "release", "app"]
