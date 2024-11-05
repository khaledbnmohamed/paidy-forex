FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1

WORKDIR /scala-app

# Copy build.sbt and Dependencies.scala
COPY build.sbt ./
COPY project/ ./project/
COPY src/ ./src/

# Run sbt update
RUN sbt update

RUN sbt compile

# Expose the port your application runs on (e.g., 8080)
EXPOSE 8081

# Start the server
CMD ["sbt", "run"]
