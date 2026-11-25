@echo off
chcp 65001 >nul
title UrbanRide - Servidor Completo (PostgreSQL + Spring Boot)
echo ===============================================================
echo   1/2 Verificando e iniciando PostgreSQL (D:\pgsql - puerto 5432)
echo ===============================================================
"d:\pgsql\bin\pg_isready.exe" -h localhost -p 5432 >nul 2>&1
if errorlevel 1 (
    "d:\pgsql\bin\pg_ctl.exe" -D "d:\pgsql\data" -l "d:\pgsql\server.log" start
)
"d:\pgsql\bin\pg_isready.exe" -h localhost -p 5432
echo.
echo ===============================================================
echo   2/2 Iniciando UrbanRide en http://localhost:8080
echo ===============================================================
start "" "http://localhost:8080"
"C:\Program Files\Java\jdk-24\bin\java.exe" -jar "%~dp0target\proyecto-marcos-de-desarrollo-web-1.0-SNAPSHOT.jar"
pause
