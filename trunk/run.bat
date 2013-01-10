java -classpath
"lib/activation.jar;lib/http.jar;lib/json.jar;lib/mail.jar;lib/util.jar;lib/mysql.jar;lib/postgres.jar;lib/oracle.jar"
se.rupy.http.Daemon -threads 1 -timeout 3600 -port 9000 -pass secret -log
