# Stage 1: Build — compilación estática con musl (sin dependencia de GLIBC)
FROM rust:slim as builder
WORKDIR /app

# Instalar musl-tools + dependencias necesarias
RUN apt-get update && apt-get install -y \
    musl-tools \
    pkg-config \
    libssl-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Agregar el target musl para Linux x86_64
RUN rustup target add x86_64-unknown-linux-musl

COPY Cargo.toml Cargo.lock ./
COPY src ./src
COPY .sqlx ./.sqlx

# Activar compilación offline para sqlx
ENV SQLX_OFFLINE=true


# Compilar binario estático en modo release
RUN cargo build --release --target x86_64-unknown-linux-musl

# Stage 2: Runtime mínimo — no necesita GLIBC
FROM debian:bookworm-slim
WORKDIR /app

# Instalar certificados CA para peticiones HTTPS
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

# Copiar el binario estático compilado
COPY --from=builder /app/target/x86_64-unknown-linux-musl/release/api-rest /app/api-rest

EXPOSE 8080

CMD ["./api-rest"]
