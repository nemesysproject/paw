# Stage 1: Build — compilación estática con musl
FROM rust:slim as builder
WORKDIR /app

# Instalar musl-tools + dependencias necesarias
RUN apt-get update && apt-get install -y \
    musl-tools \
    pkg-config \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

# Agregar el target musl para Linux x86_64
RUN rustup target add x86_64-unknown-linux-musl

COPY Cargo.toml Cargo.lock ./
COPY src ./src
COPY .sqlx ./.sqlx
COPY migrations ./migrations

# Activar compilación offline para sqlx (Evita requerir DATABASE_URL en build time)
ENV SQLX_OFFLINE=true

# Compilar binario estático en modo release
RUN cargo build --release --target x86_64-unknown-linux-musl

# Stage 2: Runtime mínimo y seguro
FROM alpine:3.19
WORKDIR /app

# Instalar certificados CA para peticiones HTTPS
RUN apk add --no-cache ca-certificates

# Crear usuario no root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiar el binario estático compilado con permisos del usuario no root
COPY --from=builder --chown=appuser:appgroup /app/target/x86_64-unknown-linux-musl/release/api-rest /app/api-rest

EXPOSE 8080

CMD ["./api-rest"]