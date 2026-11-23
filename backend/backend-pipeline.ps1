<#
.SYNOPSIS
    Pipeline de compilación, pruebas y despliegue para el backend y portal web administrativo (paw-portal) de PAW.

.DESCRIPTION
    Automatiza pruebas, compilación y despliegue para:
      - Scope 'backend' : Prisma + API REST (Rust) + PostgreSQL, MongoDB, RabbitMQ y Nginx
      - Scope 'web'     : Portal Administrativo Web (src/frontend/admin-web/paw-portal - Angular 21)
      - Scope 'all'     : Backend + Portal Web (por defecto)

    Targets soportados:
      - docker : Docker Compose local (API en :80, Admin Web en :4200)
      - podman : Manifiestos Kubernetes locales en src/backend/k8s + contenedor paw-portal en :4200
      - gcp    : Artifact Registry + Cloud Run (backend) y Firebase Hosting (admin-web)

.EXAMPLE
    .\backend-pipeline.ps1 -Action all -Target docker
    .\backend-pipeline.ps1 -Action all -Target docker -Scope web
    .\backend-pipeline.ps1 -Action test -Scope all
    .\backend-pipeline.ps1 -Action deploy -Target podman -SkipTests
    .\backend-pipeline.ps1 -Action deploy -Target gcp -GcpProject "mi-proyecto-gcp"
    .\backend-pipeline.ps1 -Action down -Target docker
#>

[CmdletBinding()]
param(
    [ValidateSet("all", "build", "test", "deploy", "down")]
    [string]$Action = "all",

    [ValidateSet("docker", "podman", "gcp")]
    [string]$Target = "docker",

    [ValidateSet("all", "backend", "web")]
    [string]$Scope = "all",

    [switch]$SkipTests,

    [string]$GcpProject = $env:GCP_PROJECT_ID,
    [string]$GcpRegion = $(if ($env:GCP_REGION) { $env:GCP_REGION } else { "us-central1" }),
    [string]$GcpRepo = "paw-repo",
    [string]$GcpService = "paw-api",
    [string]$GcpCloudSqlInstance = $env:GCP_CLOUDSQL_INSTANCE
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$BackendDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ApiDir = Join-Path $BackendDir "api-rest"
$K8sDir = Join-Path $BackendDir "k8s"
$DesignDir = Resolve-Path (Join-Path $BackendDir "..\..\design") -ErrorAction SilentlyContinue
$AdminWebDir = Resolve-Path (Join-Path $BackendDir "..\frontend\admin-web\paw-portal") -ErrorAction SilentlyContinue

$IncludeBackend = ($Scope -eq "all" -or $Scope -eq "backend")
$IncludeWeb = ($Scope -eq "all" -or $Scope -eq "web")

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Write-Ok([string]$Message) {
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Warn([string]$Message) {
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Assert-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "La herramienta requerida '$Name' no está instalada o no se encuentra en PATH."
    }
}

function Import-EnvFile([string]$Path) {
    if (-not (Test-Path $Path)) { return $false }
    Write-Host "Cargando variables de entorno desde: $Path" -ForegroundColor DarkGray
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $key = $parts[0].Trim()
            $val = $parts[1].Trim().Trim('"').Trim("'")
            # Expandir referencias ${VAR} usando variables ya cargadas
            $val = [regex]::Replace($val, '\$\{([A-Za-z0-9_]+)\}', {
                param($m)
                $refVal = [System.Environment]::GetEnvironmentVariable($m.Groups[1].Value, "Process")
                if ($null -ne $refVal) { return $refVal }
                return $m.Value
            })
            [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
        }
    }
    return $true
}

function Load-BackendEnv {
    $loaded = $false
    if (Import-EnvFile (Join-Path $BackendDir ".env")) { $loaded = $true }
    if (Import-EnvFile (Join-Path $ApiDir ".env")) { $loaded = $true }
    if (-not $loaded) {
        Write-Warn "No se encontró archivo .env en src/backend ni en src/backend/api-rest. Se usarán las variables de entorno actuales."
    }
}

function Ensure-WebDeps {
    if (-not $AdminWebDir) {
        throw "No se encontró el directorio del portal web en src/frontend/admin-web/paw-portal."
    }
    Assert-Command "npm"
    if (-not (Test-Path (Join-Path $AdminWebDir "node_modules"))) {
        Write-Host "Instalando dependencias de paw-portal (npm ci)..."
        Push-Location $AdminWebDir
        try {
            npm ci
            if ($LASTEXITCODE -ne 0) { throw "Falló npm ci en paw-portal." }
        } finally {
            Pop-Location
        }
    }
}

function Invoke-Tests {
    if ($IncludeBackend) {
        Write-Step "Ejecutando validaciones y pruebas del backend"

        if ($DesignDir -and (Test-Path (Join-Path $DesignDir "prisma\schema.prisma"))) {
            if (Get-Command "npx" -ErrorAction SilentlyContinue) {
                Write-Host "Validando esquema Prisma..."
                Push-Location $DesignDir
                try {
                    npx prisma validate
                    if ($LASTEXITCODE -ne 0) { throw "Falló prisma validate." }
                    Write-Ok "Esquema Prisma válido."
                } finally {
                    Pop-Location
                }
            } else {
                Write-Warn "npx no disponible; se omite prisma validate."
            }
        }

        Assert-Command "cargo"
        Push-Location $ApiDir
        try {
            $env:SQLX_OFFLINE = "true"
            Write-Host "Ejecutando pruebas unitarias de Rust (cargo test --lib)..."
            cargo test --lib
            if ($LASTEXITCODE -ne 0) { throw "Fallaron las pruebas unitarias de api-rest." }
            Write-Ok "Pruebas unitarias de api-rest aprobadas."
        } finally {
            Pop-Location
        }
    }

    if ($IncludeWeb) {
        Write-Step "Ejecutando pruebas del portal web (paw-portal)"
        Ensure-WebDeps
        Push-Location $AdminWebDir
        try {
            npx ng test --watch=false
            if ($LASTEXITCODE -ne 0) { throw "Fallaron las pruebas de paw-portal." }
            Write-Ok "Pruebas unitarias de paw-portal aprobadas."
        } finally {
            Pop-Location
        }
    }
}

function Invoke-Build {
    if ($IncludeBackend) {
        Write-Step "Compilando backend (Target: $Target)"

        switch ($Target) {
            "docker" {
                Assert-Command "docker"
                Push-Location $BackendDir
                try {
                    docker compose build api_rest
                    if ($LASTEXITCODE -ne 0) { throw "Falló docker compose build api_rest." }
                    Write-Ok "Imagen Docker de api_rest compilada."
                } finally {
                    Pop-Location
                }
            }
            "podman" {
                Assert-Command "podman"
                Push-Location $ApiDir
                try {
                    podman build -t localhost/api-rest:latest .
                    if ($LASTEXITCODE -ne 0) { throw "Falló podman build api-rest." }
                    Write-Ok "Imagen Podman localhost/api-rest:latest compilada."
                } finally {
                    Pop-Location
                }
            }
            "gcp" {
                Assert-Command "gcloud"
                if (-not $GcpProject) {
                    throw "Debes especificar -GcpProject (o definir GCP_PROJECT_ID) para compilar en GCP."
                }
                $imageUri = "$GcpRegion-docker.pkg.dev/$GcpProject/$GcpRepo/api-rest:latest"
                Push-Location $ApiDir
                try {
                    gcloud builds submit --project $GcpProject --tag $imageUri .
                    if ($LASTEXITCODE -ne 0) { throw "Falló gcloud builds submit." }
                    Write-Ok "Imagen publicada en Artifact Registry: $imageUri"
                } finally {
                    Pop-Location
                }
            }
        }
    }

    if ($IncludeWeb) {
        Write-Step "Compilando portal web paw-portal (Target: $Target)"
        switch ($Target) {
            "docker" {
                Assert-Command "docker"
                Push-Location $BackendDir
                try {
                    docker compose build admin_web
                    if ($LASTEXITCODE -ne 0) { throw "Falló docker compose build admin_web." }
                    Write-Ok "Imagen Docker de admin_web compilada."
                } finally {
                    Pop-Location
                }
            }
            "podman" {
                Assert-Command "podman"
                if (-not $AdminWebDir) { throw "No se encontró paw-portal." }
                Push-Location $AdminWebDir
                try {
                    podman build -t localhost/paw-portal:latest .
                    if ($LASTEXITCODE -ne 0) { throw "Falló podman build paw-portal." }
                    Write-Ok "Imagen Podman localhost/paw-portal:latest compilada."
                } finally {
                    Pop-Location
                }
            }
            "gcp" {
                Ensure-WebDeps
                Push-Location $AdminWebDir
                try {
                    npm run build
                    if ($LASTEXITCODE -ne 0) { throw "Falló npm run build en paw-portal." }
                    Write-Ok "Bundle de producción de paw-portal generado."
                } finally {
                    Pop-Location
                }
            }
        }
    }
}

function Wait-ForHealthCheck([string]$Url, [string]$Label = "Servicio", [int]$MaxAttempts = 15) {
    Write-Step "Verificando salud de $Label en $Url"
    for ($i = 1; $i -le $MaxAttempts; $i++) {
        try {
            $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($resp.StatusCode -eq 200) {
                Write-Ok "$Label operativo (HTTP 200)."
                return
            }
        } catch {
            Write-Host "Esperando a que $Label responda ($i/$MaxAttempts)..." -ForegroundColor DarkGray
            Start-Sleep -Seconds 2
        }
    }
    Write-Warn "$Label no respondió HTTP 200 en $Url tras $MaxAttempts intentos. Revisa los logs del contenedor."
}

function Expand-K8sManifest([string]$FilePath) {
    $content = Get-Content -Raw $FilePath
    return [regex]::Replace($content, '\$\{([A-Za-z0-9_]+)\}', {
        param($match)
        $varName = $match.Groups[1].Value
        $val = [System.Environment]::GetEnvironmentVariable($varName)
        if ($null -eq $val) { return "" }
        return $val
    })
}

function Invoke-Deploy {
    Write-Step "Desplegando (Scope: $Scope | Target: $Target)"

    switch ($Target) {
        "docker" {
            Assert-Command "docker"
            Push-Location $BackendDir
            try {
                $services = @()
                if ($IncludeBackend) { $services += @("postgres", "mongodb", "rabbitmq", "api_rest", "nginx") }
                if ($IncludeWeb) { $services += "admin_web" }

                docker compose up -d --build @services
                if ($LASTEXITCODE -ne 0) { throw "Falló docker compose up." }
                Write-Ok "Contenedores levantados con Docker Compose."
                docker compose ps
            } finally {
                Pop-Location
            }
            if ($IncludeBackend) { Wait-ForHealthCheck "http://localhost/health" "API REST" }
            if ($IncludeWeb) { Wait-ForHealthCheck "http://localhost:4200" "Portal Web (paw-portal)" 10 }
            Write-Host "`nURLs locales:" -ForegroundColor White
            if ($IncludeBackend) {
                Write-Host "  - API REST (Nginx):  http://localhost/api/v1"
                Write-Host "  - Swagger UI:        http://localhost/swagger-ui"
                Write-Host "  - RabbitMQ UI:       http://localhost:15672"
            }
            if ($IncludeWeb) {
                Write-Host "  - Portal Web Admin:  http://localhost:4200"
            }
        }
        "podman" {
            Assert-Command "podman"
            if ($IncludeBackend) {
                Push-Location $K8sDir
                try {
                    Expand-K8sManifest (Join-Path $K8sDir "postgres.yaml") | podman kube play -
                    podman kube play mongodb.yaml
                    podman kube play rabbitmq.yaml
                    Write-Host "Esperando 10s a que inicien PostgreSQL, MongoDB y RabbitMQ..."
                    Start-Sleep -Seconds 10
                    Expand-K8sManifest (Join-Path $K8sDir "api-rest.yaml") | podman kube play -
                    podman kube play nginx.yaml
                    Write-Ok "Despliegue backend con Podman Kube completado."
                } finally {
                    Pop-Location
                }
            }
            if ($IncludeWeb) {
                podman rm -f paw-portal 2>$null | Out-Null
                podman run -d --name paw-portal -p 4200:80 localhost/paw-portal:latest
                if ($LASTEXITCODE -ne 0) { throw "Falló el arranque del contenedor paw-portal en Podman." }
                Write-Ok "Contenedor paw-portal iniciado en http://localhost:4200."
            }
            podman ps
            if ($IncludeBackend) { Wait-ForHealthCheck "http://localhost:8080/health" "API REST (Podman)" }
            if ($IncludeWeb) { Wait-ForHealthCheck "http://localhost:4200" "Portal Web (paw-portal)" 10 }
        }
        "gcp" {
            if ($IncludeBackend) {
                Assert-Command "gcloud"
                if (-not $GcpProject) {
                    throw "Debes especificar -GcpProject (o definir GCP_PROJECT_ID)."
                }
                $imageUri = "$GcpRegion-docker.pkg.dev/$GcpProject/$GcpRepo/api-rest:latest"
                $argsList = @(
                    "run", "deploy", $GcpService,
                    "--project", $GcpProject,
                    "--image", $imageUri,
                    "--region", $GcpRegion,
                    "--allow-unauthenticated",
                    "--min-instances=1",
                    "--max-instances=5",
                    "--no-cpu-throttling",
                    "--memory=512Mi",
                    "--set-env-vars=MONGODB_DB_NAME=paw_read_db,RUST_LOG=info,ENABLE_SWAGGER=false,GEMINI_MODEL=gemini-3.8-flash",
                    "--set-secrets=DATABASE_URL=DATABASE_URL:latest,RABBITMQ_URI=RABBITMQ_URI:latest,MONGODB_URI=MONGODB_URI:latest,JWT_SECRET=JWT_SECRET:latest,CLOUDINARY_CLOUD_NAME=CLOUDINARY_CLOUD_NAME:latest,CLOUDINARY_API_KEY=CLOUDINARY_API_KEY:latest,CLOUDINARY_API_SECRET=CLOUDINARY_API_SECRET:latest,GEMINI_API_KEY=GEMINI_API_KEY:latest"
                )
                if ($GcpCloudSqlInstance) {
                    $argsList += "--add-cloudsql-instances=$GcpCloudSqlInstance"
                }

                & gcloud @argsList
                if ($LASTEXITCODE -ne 0) { throw "Falló gcloud run deploy." }
                Write-Ok "Despliegue backend en Google Cloud Run completado."
            }

            if ($IncludeWeb) {
                Ensure-WebDeps
                Push-Location $AdminWebDir
                try {
                    npx firebase-tools deploy --only hosting
                    if ($LASTEXITCODE -ne 0) { throw "Falló el despliegue de paw-portal en Firebase Hosting." }
                    Write-Ok "Despliegue de paw-portal en Firebase Hosting completado."
                } finally {
                    Pop-Location
                }
            }
        }
    }
}

function Invoke-Down {
    Write-Step "Deteniendo servicios locales (Scope: $Scope | Target: $Target)"
    switch ($Target) {
        "docker" {
            Assert-Command "docker"
            Push-Location $BackendDir
            try {
                if ($Scope -eq "all") {
                    docker compose down
                } elseif ($Scope -eq "web") {
                    docker compose stop admin_web
                    docker compose rm -f admin_web
                } else {
                    docker compose stop postgres mongodb rabbitmq api_rest nginx
                }
                Write-Ok "Servicios Docker detenidos."
            } finally {
                Pop-Location
            }
        }
        "podman" {
            Assert-Command "podman"
            if ($IncludeWeb) {
                podman rm -f paw-portal 2>$null | Out-Null
            }
            if ($IncludeBackend) {
                Push-Location $K8sDir
                try {
                    foreach ($f in @("nginx.yaml", "api-rest.yaml", "rabbitmq.yaml", "mongodb.yaml", "postgres.yaml")) {
                        if (Test-Path $f) { podman kube down $f 2>$null }
                    }
                } finally {
                    Pop-Location
                }
            }
            Write-Ok "Servicios Podman detenidos."
        }
        "gcp" {
            Write-Warn "La acción 'down' solo aplica a entornos locales (docker/podman)."
        }
    }
}

# --- Ejecución principal ---
if ($IncludeBackend) { Load-BackendEnv }

switch ($Action) {
    "test"   { Invoke-Tests }
    "build"  { Invoke-Build }
    "deploy" {
        if (-not $SkipTests) { Invoke-Tests }
        Invoke-Deploy
    }
    "down"   { Invoke-Down }
    "all"    {
        if (-not $SkipTests) { Invoke-Tests }
        Invoke-Build
        Invoke-Deploy
    }
}

Write-Host "`nPipeline '$Action' (Scope: $Scope | Target: $Target) finalizado correctamente." -ForegroundColor Green
