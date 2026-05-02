# Save Puppy - API REST (Rust)

Esta es la implementación del backend de alto rendimiento para el proyecto "Save Puppy", construida con Rust utilizando el framework Axum, SQLx para persistencia segura y Nginx como proxy perimetral.

## Requisitos Previos

- **Rust**: Instalación estable de Rust (1.80+ recomendado).
- **SQLx CLI**: Para manejar las preparaciones de la base de datos offline.
  ```bash
  cargo install sqlx-cli
  ```
- **Docker & Docker Compose**: Para orquestación local.

## Comandos de Compilación y Desarrollo

### 1. Preparación de la Base de Datos (SQLx Offline Mode)
Esta aplicación utiliza el modo offline de SQLx para permitir compilaciones seguras sin necesidad de una base de datos activa durante la construcción del binario (CI/CD o Docker).

Si realizas cambios en las consultas SQL (`sqlx::query!`), debes actualizar el caché:
1. Asegúrate de que la base de datos esté activa (`docker compose up -d postgres`).
2. Ejecuta el comando de preparación:
   ```bash
   cargo sqlx prepare
   ```
Esto actualizará la carpeta `.sqlx/` con los metadatos de las consultas.

### 2. Compilación y Ejecución Local
Para desarrollo rápido:
```bash
# Compilar y ejecutar
cargo run

# Solo verificar sintaxis y tipos
cargo check
```

### 3. Compilación para Producción (Release)
Para generar el binario optimizado:
```bash
cargo build --release
```

## Despliegue con Docker

El despliegue está automatizado mediante Docker Compose desde la raíz del backend. El `Dockerfile` está optimizado para entornos **Serverless** utilizando una compilación multi-etapa.

### Construir e Iniciar todo el Stack
Desde la carpeta `src/backend/`:
```bash
docker compose up --build
```

Esto levantará:
1. **PostgreSQL**: Base de datos persistente.
2. **api-rest**: El servicio de Rust (compilado en modo offline).
3. **Nginx**: Proxy reverso con políticas de seguridad y límites de subida (20MB).

## Seguridad

- **Validación de Archivos**: El servidor valida los *Magic Bytes* de las imágenes/videos subidos para prevenir inyecciones binarias.
- **Signed Uploads**: La comunicación con Cloudinary está firmada criptográficamente con HMAC SHA-1.
- **Nginx Protection**: Límites de cuerpo de mensaje y protección contra Slowloris configurados.
