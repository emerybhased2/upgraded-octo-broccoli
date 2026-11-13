@echo off
rem Build and run without Maven: only a JDK (17+) is needed.
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\main\java\puffpatrol\*.java || exit /b 1
java -cp out puffpatrol.Main %*
