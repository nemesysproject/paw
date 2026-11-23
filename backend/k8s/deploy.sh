#!/bin/bash

# Script de despliegue para Podman Kube

echo "Iniciando despliegue de la infraestructura PAW..."

# Verificar si envsubst está instalado
if ! command -v envsubst &> /dev/null; then
    echo "Error: envsubst no está instalado. Por favor, instálalo (usualmente parte del paquete gettext)."
    exit 1
fi

# Verificar si podman está instalado
if ! command -v podman &> /dev/null; then
    echo "Error: podman no está instalado."
    exit 1
fi

# Cargar variables de entorno desde .env si existe
if [ -f "../api-rest/.env" ]; then
    echo "Cargando variables de entorno desde ../api-rest/.env..."
    export $(grep -v '^#' ../api-rest/.env | xargs)
else
    echo "Advertencia: No se encontró el archivo ../api-rest/.env. Asegúrate de que las variables estén exportadas."
fi

# Construir la imagen de la API
echo "Construyendo la imagen de api-rest..."
cd ../api-rest
podman build -t localhost/api-rest:latest .
cd ../k8s

# Desplegar bases de datos y broker
echo "Desplegando PostgreSQL..."
envsubst < postgres.yaml | podman kube play -

echo "Desplegando MongoDB..."
podman kube play mongodb.yaml

echo "Desplegando RabbitMQ..."
podman kube play rabbitmq.yaml

# Esperar unos segundos para que los servicios base se inicialicen
echo "Esperando 10 segundos para la inicialización de servicios base..."
sleep 10

# Desplegar la API
echo "Desplegando API REST..."
envsubst < api-rest.yaml | podman kube play -

echo "Desplegando Nginx Reverse Proxy..."
podman kube play nginx.yaml

echo "¡Despliegue completado!"
echo "Puedes verificar el estado con: podman pod ps"