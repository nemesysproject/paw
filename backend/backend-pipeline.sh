#!/usr/bin/env bash
# =============================================================================
# Pipeline de compilación, pruebas y despliegue para Backend y Portal Web (paw-portal)
# Scopes:  all (default), backend, web
# Targets: docker (default), podman (k8s local), gcp (Cloud Run + Firebase Hosting)
#
# Ejemplos:
#   ./backend-pipeline.sh --action all --target docker
#   ./backend-pipeline.sh --action all --target docker --scope web
#   ./backend-pipeline.sh --action test --scope all
#   ./backend-pipeline.sh --action deploy --target podman --skip-tests
#   ./backend-pipeline.sh --action deploy --target gcp --gcp-project mi-proyecto
#   ./backend-pipeline.sh --action down --target docker
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR"
API_DIR="$BACKEND_DIR/api-rest"
K8S_DIR="$BACKEND_DIR/k8s"
DESIGN_DIR="$(cd "$BACKEND_DIR/../../design" 2>/dev/null && pwd || true)"
ADMIN_WEB_DIR="$(cd "$BACKEND_DIR/../frontend/admin-web/paw-portal" 2>/dev/null && pwd || true)"

ACTION="all"
TARGET="docker"
SCOPE="all"
SKIP_TESTS=false
GCP_PROJECT="${GCP_PROJECT_ID:-}"
GCP_REGION="${GCP_REGION:-us-central1}"
GCP_REPO="paw-repo"
GCP_SERVICE="paw-api"
GCP_CLOUDSQL_INSTANCE="${GCP_CLOUDSQL_INSTANCE:-}"

usage() {
  cat <<EOF
Uso: $0 [opciones]

Opciones:
  --action <all|build|test|deploy|down>   Acción a ejecutar (default: all)
  --target <docker|podman|gcp>            Entorno destino (default: docker)
  --scope <all|backend|web>               Proyectos a incluir (default: all)
  --skip-tests                            Omitir pruebas antes de desplegar
  --gcp-project <id>                      ID del proyecto GCP (para target gcp)
  --gcp-region <region>                   Región GCP (default: us-central1)
  --gcp-sql-instance <connection_name>    Instancia Cloud SQL (ej. proj:reg:inst)
  -h, --help                              Mostrar esta ayuda
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --action) ACTION="$2"; shift 2 ;;
    --target) TARGET="$2"; shift 2 ;;
    --scope) SCOPE="$2"; shift 2 ;;
    --skip-tests) SKIP_TESTS=true; shift ;;
    --gcp-project) GCP_PROJECT="$2"; shift 2 ;;
    --gcp-region) GCP_REGION="$2"; shift 2 ;;
    --gcp-sql-instance) GCP_CLOUDSQL_INSTANCE="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Opción desconocida: $1"; usage; exit 1 ;;
  esac
done

INCLUDE_BACKEND=false
INCLUDE_WEB=false
case "$SCOPE" in
  all)     INCLUDE_BACKEND=true; INCLUDE_WEB=true ;;
  backend) INCLUDE_BACKEND=true ;;
  web)     INCLUDE_WEB=true ;;
  *) echo "Scope inválido: $SCOPE" >&2; usage; exit 1 ;;
esac

log_step() { echo -e "\n==> $1"; }
log_ok()   { echo -e "[OK] $1"; }
log_warn() { echo -e "[WARN] $1"; }

assert_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: la herramienta requerida '$1' no está instalada." >&2
    exit 1
  fi
}

load_env_file() {
  local file="$1"
  if [[ -f "$file" ]]; then
    echo "Cargando variables de entorno desde: $file"
    set -a
    # shellcheck disable=SC1090
    source <(grep -v '^[[:space:]]*#' "$file" | grep -v '^[[:space:]]*$')
    set +a
    return 0
  fi
  return 1
}

load_backend_env() {
  local loaded=false
  load_env_file "$BACKEND_DIR/.env" && loaded=true || true
  load_env_file "$API_DIR/.env" && loaded=true || true
  if [[ "$loaded" == false ]]; then
    log_warn "No se encontró .env en src/backend ni en src/backend/api-rest. Se usarán las variables de entorno actuales."
  fi
}

ensure_web_deps() {
  if [[ -z "$ADMIN_WEB_DIR" || ! -d "$ADMIN_WEB_DIR" ]]; then
    echo "Error: no se encontró el proyecto paw-portal en src/frontend/admin-web/paw-portal." >&2
    exit 1
  fi
  assert_cmd npm
  if [[ ! -d "$ADMIN_WEB_DIR/node_modules" ]]; then
    echo "Instalando dependencias de paw-portal (npm ci)..."
    (cd "$ADMIN_WEB_DIR" && npm ci)
  fi
}

run_tests() {
  if [[ "$INCLUDE_BACKEND" == true ]]; then
    log_step "Ejecutando validaciones y pruebas del backend"

    if [[ -n "$DESIGN_DIR" && -f "$DESIGN_DIR/prisma/schema.prisma" ]]; then
      if command -v npx >/dev/null 2>&1; then
        echo "Validando esquema Prisma..."
        (cd "$DESIGN_DIR" && npx prisma validate)
        log_ok "Esquema Prisma válido."
      else
        log_warn "npx no disponible; se omite prisma validate."
      fi
    fi

    assert_cmd cargo
    echo "Ejecutando pruebas unitarias de Rust (cargo test --lib)..."
    (cd "$API_DIR" && SQLX_OFFLINE=true cargo test --lib)
    log_ok "Pruebas unitarias de api-rest aprobadas."
  fi

  if [[ "$INCLUDE_WEB" == true ]]; then
    log_step "Ejecutando pruebas del portal web (paw-portal)"
    ensure_web_deps
    (cd "$ADMIN_WEB_DIR" && npx ng test --watch=false)
    log_ok "Pruebas unitarias de paw-portal aprobadas."
  fi
}

run_build() {
  if [[ "$INCLUDE_BACKEND" == true ]]; then
    log_step "Compilando backend (Target: $TARGET)"

    case "$TARGET" in
      docker)
        assert_cmd docker
        (cd "$BACKEND_DIR" && docker compose build api_rest)
        log_ok "Imagen Docker de api_rest compilada."
        ;;
      podman)
        assert_cmd podman
        (cd "$API_DIR" && podman build -t localhost/api-rest:latest .)
        log_ok "Imagen Podman localhost/api-rest:latest compilada."
        ;;
      gcp)
        assert_cmd gcloud
        if [[ -z "$GCP_PROJECT" ]]; then
          echo "Error: debes especificar --gcp-project o GCP_PROJECT_ID." >&2
          exit 1
        fi
        local image_uri="${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT}/${GCP_REPO}/api-rest:latest"
        (cd "$API_DIR" && gcloud builds submit --project "$GCP_PROJECT" --tag "$image_uri" .)
        log_ok "Imagen publicada en Artifact Registry: $image_uri"
        ;;
      *)
        echo "Target inválido: $TARGET" >&2
        exit 1
        ;;
    esac
  fi

  if [[ "$INCLUDE_WEB" == true ]]; then
    log_step "Compilando portal web paw-portal (Target: $TARGET)"
    case "$TARGET" in
      docker)
        assert_cmd docker
        (cd "$BACKEND_DIR" && docker compose build admin_web)
        log_ok "Imagen Docker de admin_web compilada."
        ;;
      podman)
        assert_cmd podman
        [[ -n "$ADMIN_WEB_DIR" ]] || { echo "Error: no se encontró paw-portal." >&2; exit 1; }
        (cd "$ADMIN_WEB_DIR" && podman build -t localhost/paw-portal:latest .)
        log_ok "Imagen Podman localhost/paw-portal:latest compilada."
        ;;
      gcp)
        ensure_web_deps
        (cd "$ADMIN_WEB_DIR" && npm run build)
        log_ok "Bundle de producción de paw-portal generado."
        ;;
    esac
  fi
}

wait_for_health() {
  local url="$1"
  local label="${2:-Servicio}"
  local max_attempts="${3:-15}"
  log_step "Verificando salud de $label en $url"

  if ! command -v curl >/dev/null 2>&1; then
    log_warn "curl no está instalado; omitiendo health check automático."
    return
  fi

  for ((i=1; i<=max_attempts; i++)); do
    if curl -fsS --max-time 3 "$url" >/dev/null 2>&1; then
      log_ok "$label operativo (HTTP 200)."
      return
    fi
    echo "Esperando a que $label responda ($i/$max_attempts)..."
    sleep 2
  done
  log_warn "$label no respondió HTTP 200 en $url tras $max_attempts intentos."
}

run_deploy() {
  log_step "Desplegando (Scope: $SCOPE | Target: $TARGET)"

  case "$TARGET" in
    docker)
      assert_cmd docker
      local services=()
      [[ "$INCLUDE_BACKEND" == true ]] && services+=(postgres mongodb rabbitmq api_rest nginx)
      [[ "$INCLUDE_WEB" == true ]] && services+=(admin_web)
      (cd "$BACKEND_DIR" && docker compose up -d --build "${services[@]}" && docker compose ps)
      log_ok "Contenedores levantados con Docker Compose."
      [[ "$INCLUDE_BACKEND" == true ]] && wait_for_health "http://localhost/health" "API REST"
      [[ "$INCLUDE_WEB" == true ]] && wait_for_health "http://localhost:4200" "Portal Web (paw-portal)" 10
      echo ""
      echo "URLs locales:"
      if [[ "$INCLUDE_BACKEND" == true ]]; then
        echo "  - API REST (Nginx):  http://localhost/api/v1"
        echo "  - Swagger UI:        http://localhost/swagger-ui"
        echo "  - RabbitMQ UI:       http://localhost:15672"
      fi
      if [[ "$INCLUDE_WEB" == true ]]; then
        echo "  - Portal Web Admin:  http://localhost:4200"
      fi
      ;;
    podman)
      assert_cmd podman
      if [[ "$INCLUDE_BACKEND" == true ]]; then
        assert_cmd envsubst
        (
          cd "$K8S_DIR"
          envsubst < postgres.yaml | podman kube play -
          podman kube play mongodb.yaml
          podman kube play rabbitmq.yaml
          echo "Esperando 10s a que inicien PostgreSQL, MongoDB y RabbitMQ..."
          sleep 10
          envsubst < api-rest.yaml | podman kube play -
          podman kube play nginx.yaml
        )
        log_ok "Despliegue backend con Podman Kube completado."
      fi
      if [[ "$INCLUDE_WEB" == true ]]; then
        podman rm -f paw-portal >/dev/null 2>&1 || true
        podman run -d --name paw-portal -p 4200:80 localhost/paw-portal:latest
        log_ok "Contenedor paw-portal iniciado en http://localhost:4200."
      fi
      podman ps
      [[ "$INCLUDE_BACKEND" == true ]] && wait_for_health "http://localhost:8080/health" "API REST (Podman)"
      [[ "$INCLUDE_WEB" == true ]] && wait_for_health "http://localhost:4200" "Portal Web (paw-portal)" 10
      ;;
    gcp)
      if [[ "$INCLUDE_BACKEND" == true ]]; then
        assert_cmd gcloud
        if [[ -z "$GCP_PROJECT" ]]; then
          echo "Error: debes especificar --gcp-project o GCP_PROJECT_ID." >&2
          exit 1
        fi
        local image_uri="${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT}/${GCP_REPO}/api-rest:latest"
        local extra_args=()
        if [[ -n "$GCP_CLOUDSQL_INSTANCE" ]]; then
          extra_args+=("--add-cloudsql-instances=$GCP_CLOUDSQL_INSTANCE")
        fi

        gcloud run deploy "$GCP_SERVICE" \
          --project "$GCP_PROJECT" \
          --image "$image_uri" \
          --region "$GCP_REGION" \
          --allow-unauthenticated \
          --min-instances=1 \
          --max-instances=5 \
          --no-cpu-throttling \
          --memory=512Mi \
          --set-env-vars="MONGODB_DB_NAME=paw_read_db,RUST_LOG=info,ENABLE_SWAGGER=false,GEMINI_MODEL=gemini-3.8-flash" \
          --set-secrets="DATABASE_URL=DATABASE_URL:latest,RABBITMQ_URI=RABBITMQ_URI:latest,MONGODB_URI=MONGODB_URI:latest,JWT_SECRET=JWT_SECRET:latest,CLOUDINARY_CLOUD_NAME=CLOUDINARY_CLOUD_NAME:latest,CLOUDINARY_API_KEY=CLOUDINARY_API_KEY:latest,CLOUDINARY_API_SECRET=CLOUDINARY_API_SECRET:latest,GEMINI_API_KEY=GEMINI_API_KEY:latest" \
          "${extra_args[@]}"
        log_ok "Despliegue backend en Google Cloud Run completado."
      fi

      if [[ "$INCLUDE_WEB" == true ]]; then
        ensure_web_deps
        (cd "$ADMIN_WEB_DIR" && npx firebase-tools deploy --only hosting)
        log_ok "Despliegue de paw-portal en Firebase Hosting completado."
      fi
      ;;
    *)
      echo "Target inválido: $TARGET" >&2
      exit 1
      ;;
  esac
}

run_down() {
  log_step "Deteniendo servicios locales (Scope: $SCOPE | Target: $TARGET)"
  case "$TARGET" in
    docker)
      assert_cmd docker
      if [[ "$SCOPE" == "all" ]]; then
        (cd "$BACKEND_DIR" && docker compose down)
      elif [[ "$SCOPE" == "web" ]]; then
        (cd "$BACKEND_DIR" && docker compose stop admin_web && docker compose rm -f admin_web)
      else
        (cd "$BACKEND_DIR" && docker compose stop postgres mongodb rabbitmq api_rest nginx)
      fi
      log_ok "Servicios Docker detenidos."
      ;;
    podman)
      assert_cmd podman
      if [[ "$INCLUDE_WEB" == true ]]; then
        podman rm -f paw-portal >/dev/null 2>&1 || true
      fi
      if [[ "$INCLUDE_BACKEND" == true ]]; then
        (
          cd "$K8S_DIR"
          for f in nginx.yaml api-rest.yaml rabbitmq.yaml mongodb.yaml postgres.yaml; do
            [[ -f "$f" ]] && podman kube down "$f" || true
          done
        )
      fi
      log_ok "Servicios Podman detenidos."
      ;;
    gcp)
      log_warn "La acción 'down' solo aplica a entornos locales (docker/podman)."
      ;;
  esac
}

# --- Ejecución principal ---
[[ "$INCLUDE_BACKEND" == true ]] && load_backend_env

case "$ACTION" in
  test)   run_tests ;;
  build)  run_build ;;
  deploy)
    [[ "$SKIP_TESTS" == false ]] && run_tests
    run_deploy
    ;;
  down)   run_down ;;
  all)
    [[ "$SKIP_TESTS" == false ]] && run_tests
    run_build
    run_deploy
    ;;
  *)
    echo "Acción inválida: $ACTION" >&2
    usage
    exit 1
    ;;
esac

echo -e "\nPipeline '$ACTION' (Scope: $SCOPE | Target: $TARGET) finalizado correctamente."
