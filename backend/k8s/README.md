# Despliegue con Kubernetes y Podman

Este directorio contiene los manifiestos de Kubernetes para desplegar la aplicación `api-rest` y sus dependencias (PostgreSQL, MongoDB, RabbitMQ) utilizando Podman.

## Recomendaciones y Mejoras de Seguridad Implementadas

1. **Ejecución sin privilegios (Non-Root):** El `Dockerfile` ha sido modificado para crear y utilizar un usuario `appuser` sin privilegios de root. El manifiesto de Kubernetes (`api-rest.yaml`) refuerza esto mediante `securityContext` (`runAsNonRoot: true`, `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true`).
2. **Reducción de la superficie de ataque:** Se cambió la imagen base del runtime en el `Dockerfile` de `debian:bookworm-slim` a `alpine:3.19`, que es mucho más ligera y segura.
3. **CORS Seguro en Nginx:** Se modificó `nginx.conf` para validar los orígenes permitidos mediante un bloque `map`, evitando reflejar ciegamente cualquier origen (`$http_origin`), lo cual es una vulnerabilidad común.
4. **Separación de Contenedores:** Se ha migrado de `docker-compose.yml` a manifiestos individuales de Kubernetes, separando claramente los volúmenes persistentes (PVC), secretos, despliegues y servicios para cada componente.
5. **Gestión de Secretos mediante Variables de Entorno:** Las credenciales y variables sensibles se han eliminado de los archivos YAML. Ahora se inyectan dinámicamente utilizando variables de entorno y `envsubst`, evitando exponer secretos en el código fuente o en los manifiestos de despliegue.

## Instrucciones de Despliegue con Podman

Podman permite ejecutar manifiestos de Kubernetes localmente sin necesidad de un clúster completo (como minikube o k3s) utilizando el comando `podman kube play`.

### 1. Construir la imagen de la API con Podman

Primero, asegúrate de estar en el directorio `src/backend/api-rest` y construye la imagen localmente:

```bash
cd ../api-rest
podman build -t localhost/api-rest:latest .
```

### 2. Configurar Variables de Entorno

Antes de desplegar, debes exportar las variables de entorno necesarias. Puedes crear un archivo `.env` y cargarlo, o exportarlas directamente en tu terminal:

```bash
export POSTGRES_USER="paw_user"
export POSTGRES_PASSWORD="paw_password"
export POSTGRES_DB="paw_db"
export DATABASE_URL="postgres://paw_user:paw_password@postgres:5432/paw_db"
export RABBITMQ_URI="amqp://guest:guest@rabbitmq:5672/%2f"
export MONGODB_URI="mongodb://mongodb:27017"
export MONGODB_DB_NAME="paw_read_db"
export CLOUDINARY_CLOUD_NAME="your_cloud_name"
export CLOUDINARY_API_KEY="your_api_key"
export CLOUDINARY_API_SECRET="your_api_secret"
export JWT_SECRET="your_super_secret_jwt_key"
```

### 3. Desplegar los servicios automáticamente

Se han proporcionado scripts para automatizar la construcción de la imagen y el despliegue de todos los manifiestos. Estos scripts cargarán automáticamente las variables desde `../api-rest/.env` si el archivo existe.

**En Linux/macOS:**
```bash
cd ../k8s
chmod +x deploy.sh
./deploy.sh
```

**En Windows:**
```cmd
cd ..\k8s
deploy.bat
```

### 4. Verificar el estado

Puedes verificar los pods y contenedores en ejecución con:

```bash
podman pod ps
podman ps
```

### 5. Detener y limpiar

Para detener y eliminar los recursos creados por los manifiestos:

```bash
podman kube down postgres.yaml
podman kube down mongodb.yaml
podman kube down rabbitmq.yaml
podman kube down api-rest.yaml
```

*Nota: Los volúmenes persistentes creados por Podman para los PVCs se mantendrán. Si deseas eliminarlos, deberás usar `podman volume rm <nombre_volumen>`.*