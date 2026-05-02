# Arquitectura del Backend (API REST en Rust)

Este directorio contiene la orquestación y el código fuente del ecosistema del backend principal de **PAW**, diseñado para Alta Disponibilidad (HA) y un despliegue en la nube bajo el modelo **Serverless**.

## Consideraciones de Arquitectura y Seguridad

La arquitectura orquestada en el archivo `docker-compose.yml` plasma físicamente las decisiones tomadas en el diseño del sistema. A continuación se detallan los principios aplicados:

### 1. Defensa en Profundidad (Nginx Reverse Proxy)
- **Aislamiento de Red:** El contenedor de la API en Rust (`api_rest`) **no expone** ningún puerto hacia el exterior. Todas las peticiones del usuario o la aplicación móvil deben pasar obligatoriamente a través del servicio `nginx`.
- **Mitigación de Ataques:** 
  - Nginx está configurado con **Rate Limiting** (limitando las peticiones por segundo por IP) para evitar ataques de denegación de servicio (DDoS) y fuerza bruta.
  - Implementa restricciones agresivas de **Timeout** para abortar clientes lentos (ataques Slowloris) antes de que saturen los recursos de la API.
- **Cabeceras de Seguridad:** Inyección automática de cabeceras seguras (XSS Protection, No-Sniff) aislando el código de negocio de las responsabilidades de red.

### 2. Cómputo "Serverless-Ready" (Rust + Docker Multi-stage)
- **Monolito Modular:** El servicio `api_rest` carga un binario compilado de Rust utilizando **Axum** y **Tokio**. Se estructuró como un monolito para agilidad de desarrollo inicial, pero la orquestación actual permite escalarlo dividiendo los contenedores en un "Command Service" y "Query Service" en el futuro sin reescribir código.
- **Ligereza Extrema:** El `Dockerfile` utiliza un patrón *multi-stage*. La etapa final omite todas las herramientas de compilación y utiliza una imagen mínima (`debian:bookworm-slim`), logrando que el contenedor pese menos de 30MB y tenga un "Cold Start" de escasos milisegundos.

### 3. Resiliencia del Código (Circuit Breaker)
- Aunque Nginx filtra ataques externos, la API en sí está protegida internamente. Las librerías de Rust (`failsafe` y `tower`) se encargarán de envolver las llamadas a la base de datos (PostgreSQL) o colas de mensajería futuras (RabbitMQ). Si estos servicios colapsan, la API hará "Cortocircuito" (Circuit Breaker) para fallar rápidamente y no consumir memoria ram de manera infinita.

### 4. Base de Datos Integrada
- El `docker-compose.yml` levanta automáticamente un contenedor de **PostgreSQL 15**. Esta base de datos es la misma hacia la cual apuntarán los scripts de migración y seed de Prisma (en la carpeta `design`). El volumen persistente (`pgdata`) asegura que los datos no se pierdan entre reinicios de los contenedores.

## Uso (Desarrollo Local)

Para levantar toda la arquitectura de manera transparente:

```bash
# Navegar al directorio del backend
cd src/backend

# Construir las imágenes y levantar los contenedores en segundo plano
docker compose up -d --build

# Revisar los logs
docker compose logs -f
```

La API quedará expuesta de manera segura en `http://localhost:80` (A través de Nginx).
