-- =============================================================================
-- Migración: Modelo de Publicidad y Campañas Geolocalizadas
-- Soporta: Alimento para mascotas, veterinarias, campañas gubernamentales y
-- negocios afines, alineado con políticas de transparencia de Google Play.
-- =============================================================================

-- 1. Tablas de Catálogo (siguiendo la convención de 20260503000000_initial_schema.sql)
CREATE TABLE "AdCategory" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE "AdPlacement" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE "AdEventType" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

-- Seed inicial idempotente de catálogos de publicidad
INSERT INTO "AdCategory" (id, name) VALUES
    ('PET_FOOD', 'Alimento para Mascotas'),
    ('VETERINARY', 'Veterinarias y Clínicas'),
    ('GOVERNMENT', 'Campañas Gubernamentales'),
    ('PET_SHOP', 'Tiendas y Negocios Afines'),
    ('SHELTER_EVENT', 'Eventos de Refugios y Adopción')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "AdPlacement" (id, name) VALUES
    ('HOME_FEED', 'Feed Principal (Tarjeta Nativa)'),
    ('BANNER_TOP', 'Banner / Carrusel Superior'),
    ('POST_REPORT', 'Confirmación Post-Reporte'),
    ('DIRECTORY_FEATURED', 'Directorio Destacado')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "AdEventType" (id, name) VALUES
    ('IMPRESSION', 'Impresión'),
    ('CLICK', 'Clic')
ON CONFLICT (id) DO NOTHING;

-- 2. Mejoras a la tabla Veterinary para directorio destacado y búsqueda por Geohash
ALTER TABLE "Veterinary"
    ADD COLUMN IF NOT EXISTS whatsapp TEXT,
    ADD COLUMN IF NOT EXISTS website TEXT,
    ADD COLUMN IF NOT EXISTS "isVerified" BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS "isFeatured" BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS geohash VARCHAR(12);

CREATE INDEX IF NOT EXISTS "Veterinary_geohash_idx" ON "Veterinary"(geohash);

-- 3. Tabla Advertiser (Anunciantes / Patrocinadores)
CREATE TABLE "Advertiser" (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT NOT NULL REFERENCES "AdCategory"(id),
    "contactName" TEXT,
    email TEXT,
    phone TEXT,
    whatsapp TEXT,
    website TEXT,
    "logoUrl" TEXT,
    "logoPublicId" TEXT,
    "isGovernment" BOOLEAN NOT NULL DEFAULT FALSE,
    "officialSourceUrl" TEXT,
    "veterinaryId" TEXT REFERENCES "Veterinary"(id) ON DELETE SET NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT TRUE,
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 4. Tabla AdCampaign (Campañas y anuncios geolocalizados)
CREATE TABLE "AdCampaign" (
    id TEXT PRIMARY KEY,
    "advertiserId" TEXT NOT NULL REFERENCES "Advertiser"(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    "ctaLabel" TEXT NOT NULL DEFAULT 'Ver más',
    "targetUrl" TEXT,
    "imageUrl" TEXT NOT NULL,
    "imagePublicId" TEXT NOT NULL,
    category TEXT NOT NULL REFERENCES "AdCategory"(id),
    placement TEXT NOT NULL REFERENCES "AdPlacement"(id),
    "badgeLabel" TEXT NOT NULL DEFAULT 'Patrocinado',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    geohash VARCHAR(12),
    "radiusKm" DOUBLE PRECISION,
    priority INTEGER NOT NULL DEFAULT 0,
    "startsAt" TIMESTAMP NOT NULL,
    "endsAt" TIMESTAMP NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT TRUE,
    "impressionsCount" BIGINT NOT NULL DEFAULT 0,
    "clicksCount" BIGINT NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT "AdCampaign_valid_dates_chk" CHECK ("endsAt" > "startsAt")
);

CREATE INDEX "AdCampaign_active_placement_dates_idx"
    ON "AdCampaign"("isActive", placement, "startsAt", "endsAt");
CREATE INDEX "AdCampaign_geohash_idx" ON "AdCampaign"(geohash);
CREATE INDEX "AdCampaign_advertiserId_idx" ON "AdCampaign"("advertiserId");

-- 5. Tabla AdMetricEvent (Registro granular de impresiones y clics para reportes CTR)
CREATE TABLE "AdMetricEvent" (
    id TEXT PRIMARY KEY,
    "campaignId" TEXT NOT NULL REFERENCES "AdCampaign"(id) ON DELETE CASCADE,
    "eventType" TEXT NOT NULL REFERENCES "AdEventType"(id),
    "userId" TEXT REFERENCES "User"(id) ON DELETE SET NULL,
    geohash VARCHAR(12),
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX "AdMetricEvent_campaign_type_date_idx"
    ON "AdMetricEvent"("campaignId", "eventType", "createdAt");
