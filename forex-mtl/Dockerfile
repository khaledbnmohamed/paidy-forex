FROM hseeberger/scala-sbt:11.0.11_1.5.5_2.13.6

WORKDIR /scala-app

# Copy build.sbt and Dependencies.scala
COPY build.sbt ./
COPY project/ ./project/
COPY src/ ./src/

# Run sbt update
RUN sbt update

RUN sbt compile

EXPOSE 8081

# Start the server
CMD ["sbt", "run"]
