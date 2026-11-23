@echo off
setlocal enabledelayedexpansion

echo Iniciando despliegue de la infraestructura PAW...

:: Verificar si podman está instalado
where podman >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: podman no esta instalado.
    exit /b 1
)

:: Verificar si la máquina de Podman está iniciada (necesario en Windows)
podman info >nul 2>nul
if %errorlevel% neq 0 (
    echo La maquina virtual de Podman no esta en ejecucion. Intentando iniciarla...
    podman machine start
    if !errorlevel! neq 0 (
        echo.
        echo Error: No se pudo iniciar la maquina de Podman.
        echo Si es la primera vez que usas Podman, necesitas inicializar la maquina ejecutando:
        echo    podman machine init
        echo Y luego vuelve a ejecutar este script.
        exit /b 1
    )
)

:: Cargar variables de entorno desde .env si existe
if exist "..\api-rest\.env" (
    echo Cargando variables de entorno desde ..\api-rest\.env...
    for /f "usebackq tokens=1,* delims==" %%a in ("..\api-rest\.env") do (
        set "line=%%a"
        :: Ignorar comentarios
        if not "!line:~0,1!"=="#" (
            set "%%a=%%b"
        )
    )
) else (
    echo Advertencia: No se encontro el archivo ..\api-rest\.env. Asegurate de que las variables esten configuradas.
)

:: Construir la imagen de la API
echo Construyendo la imagen de api-rest...
cd ..\api-rest
podman build -t localhost/api-rest:latest .
cd ..\k8s

:: Desplegar bases de datos y broker
echo Desplegando PostgreSQL...
:: En Windows, envsubst no es nativo. Usamos un script de PowerShell temporal para reemplazar variables
powershell -Command "(Get-Content postgres.yaml) -replace '\$\{POSTGRES_USER\}', $env:POSTGRES_USER -replace '\$\{POSTGRES_PASSWORD\}', $env:POSTGRES_PASSWORD -replace '\$\{POSTGRES_DB\}', $env:POSTGRES_DB | podman kube play -"

echo Desplegando MongoDB...
podman kube play mongodb.yaml

echo Desplegando RabbitMQ...
podman kube play rabbitmq.yaml

:: Esperar unos segundos para que los servicios base se inicialicen
echo Esperando 10 segundos para la inicializacion de servicios base...
timeout /t 10 /nobreak >nul

:: Desplegar la API
echo Desplegando API REST...
powershell -Command "(Get-Content api-rest.yaml) -replace '\$\{DATABASE_URL\}', $env:DATABASE_URL -replace '\$\{RABBITMQ_URI\}', $env:RABBITMQ_URI -replace '\$\{MONGODB_URI\}', $env:MONGODB_URI -replace '\$\{MONGODB_DB_NAME\}', $env:MONGODB_DB_NAME -replace '\$\{CLOUDINARY_CLOUD_NAME\}', $env:CLOUDINARY_CLOUD_NAME -replace '\$\{CLOUDINARY_API_KEY\}', $env:CLOUDINARY_API_KEY -replace '\$\{CLOUDINARY_API_SECRET\}', $env:CLOUDINARY_API_SECRET -replace '\$\{JWT_SECRET\}', $env:JWT_SECRET | podman kube play -"

echo Desplegando Nginx Reverse Proxy...
podman kube play nginx.yaml

echo ¡Despliegue completado!
echo Puedes verificar el estado con: podman pod ps
endlocal