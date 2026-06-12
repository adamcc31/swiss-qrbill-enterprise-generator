@echo off
setlocal
set "MAVEN_HOME=%~dp0maven\apache-maven-3.9.6"
call "%MAVEN_HOME%\bin\mvn.cmd" %*
