#!/bin/sh
# start Redis in the background
redis-server --daemonize yes
# now launch your JVM app
exec java -jar /app/app.jar
