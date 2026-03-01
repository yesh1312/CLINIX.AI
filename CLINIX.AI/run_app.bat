@echo off
set "JAVA_HOME=C:\Program Files\Processing\app\resources\jdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using JAVA_HOME: %JAVA_HOME%
java -version
"C:\Users\yeshv\Downloads\maven-mvnd-1.0.3-windows-amd64\maven-mvnd-1.0.3-windows-amd64\mvn\bin\mvn.cmd" spring-boot:run > startup.log 2>&1
