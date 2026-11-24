-- =============================================================================
-- Migración: Interactividad Social (Avistamientos, Chat, Adopciones, Push, UGC)
-- Conecta usuarios, rescatistas, refugios y veterinarias.
-- =============================================================================

-- 1. Tablas de Catálogo (convención del proyecto para seedabilidad)
CREATE TABLE "ConversationType" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE "ConversationStatus" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE "MessageType" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE "AdoptionStatus" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE "ReportTargetType" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE "ReportStatus" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

-- Seed idempotente de catálogos
INSERT INTO "ConversationType" (id, name) VALUES
    ('PET_SIGHTING', 'Avistamiento de Mascota Perdida'),
    ('ADOPTION_INQUIRY', 'Consulta de Adopción'),
    ('SHELTER_SUPPORT', 'Contacto con Refugio'),
    ('VET_CONSULT', 'Consulta con Veterinaria')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "ConversationStatus" (id, name) VALUES
    ('OPEN', 'Abierta'),
    ('CLOSED', 'Cerrada'),
    ('ARCHIVED', 'Archivada')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "MessageType" (id, name) VALUES
    ('TEXT', 'Texto'),
    ('IMAGE', 'Imagen'),
    ('LOCATION', 'Ubicación GPS'),
    ('SYSTEM', 'Mensaje del Sistema')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "AdoptionStatus" (id, name) VALUES
    ('PENDING', 'Pendiente'),
    ('IN_REVIEW', 'En Revisión'),
    ('APPROVED', 'Aprobada'),
    ('REJECTED', 'Rechazada'),
    ('CANCELLED', 'Cancelada')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "ReportTargetType" (id, name) VALUES
    ('USER', 'Usuario'),
    ('MESSAGE', 'Mensaje'),
    ('SIGHTING', 'Avistamiento'),
    ('PET', 'Mascota')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "ReportStatus" (id, name) VALUES
    ('OPEN', 'Abierto'),
    ('REVIEWED', 'Revisado'),
    ('RESOLVED', 'Resuelto'),
    ('DISMISSED', 'Descartado')
ON CONFLICT (id) DO NOTHING;

-- Asegurar roles VET y SHELTER_OWNER en UserRole
INSERT INTO "UserRole" (id, name) VALUES
    ('VET', 'Veterinaria / Médico Veterinario'),
    ('SHELTER_OWNER', 'Responsable de Refugio')
ON CONFLICT (id) DO NOTHING;

-- 2. Mejoras a tablas existentes (User, Shelter, Veterinary)
ALTER TABLE "User"
    ADD COLUMN IF NOT EXISTS phone TEXT,
    ADD COLUMN IF NOT EXISTS "avatarUrl" TEXT;

ALTER TABLE "Shelter"
    ADD COLUMN IF NOT EXISTS "ownerId" TEXT REFERENCES "User"(id) ON DELETE SET NULL;

ALTER TABLE "Veterinary"
    ADD COLUMN IF NOT EXISTS "ownerId" TEXT REFERENCES "User"(id) ON DELETE SET NULL;

-- 3. Tokens FCM para notificaciones push
CREATE TABLE "DeviceToken" (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    "fcmToken" TEXT UNIQUE NOT NULL,
    platform TEXT NOT NULL DEFAULT 'ANDROID',
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX "DeviceToken_userId_idx" ON "DeviceToken"("userId");

-- 4. Avistamientos colaborativos ("¡La vi aquí!")
CREATE TABLE "PetSighting" (
    id TEXT PRIMARY KEY,
    "petId" TEXT NOT NULL REFERENCES "Pet"(id) ON DELETE CASCADE,
    "reporterId" TEXT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    notes TEXT,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    geohash VARCHAR(12),
    "imageUrl" TEXT,
    "imagePublicId" TEXT,
    "sightedAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "isConfirmedByOwner" BOOLEAN NOT NULL DEFAULT FALSE,
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX "PetSighting_petId_createdAt_idx" ON "PetSighting"("petId", "createdAt");
CREATE INDEX "PetSighting_geohash_idx" ON "PetSighting"(geohash);

-- 5. Conversaciones contextuales
CREATE TABLE "Conversation" (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL REFERENCES "ConversationType"(id),
    status TEXT NOT NULL REFERENCES "ConversationStatus"(id) DEFAULT 'OPEN',
    "petId" TEXT REFERENCES "Pet"(id) ON DELETE SET NULL,
    "shelterId" TEXT REFERENCES "Shelter"(id) ON DELETE SET NULL,
    "veterinaryId" TEXT REFERENCES "Veterinary"(id) ON DELETE SET NULL,
    "lastMessageAt" TIMESTAMP,
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX "Conversation_petId_idx" ON "Conversation"("petId");
CREATE INDEX "Conversation_shelterId_idx" ON "Conversation"("shelterId");
CREATE INDEX "Conversation_veterinaryId_idx" ON "Conversation"("veterinaryId");
CREATE INDEX "Conversation_lastMessageAt_idx" ON "Conversation"("lastMessageAt");

-- 6. Participantes de conversación
CREATE TABLE "ConversationParticipant" (
    id TEXT PRIMARY KEY,
    "conversationId" TEXT NOT NULL REFERENCES "Conversation"(id) ON DELETE CASCADE,
    "userId" TEXT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    "lastReadAt" TIMESTAMP,
    "isMuted" BOOLEAN NOT NULL DEFAULT FALSE,
    "joinedAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT "ConversationParticipant_conv_user_uq" UNIQUE ("conversationId", "userId")
);

CREATE INDEX "ConversationParticipant_userId_idx" ON "ConversationParticipant"("userId");

-- 7. Mensajes (texto, imagen, ubicación GPS)
CREATE TABLE "Message" (
    id TEXT PRIMARY KEY,
    "conversationId" TEXT NOT NULL REFERENCES "Conversation"(id) ON DELETE CASCADE,
    "senderId" TEXT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    type TEXT NOT NULL REFERENCES "MessageType"(id) DEFAULT 'TEXT',
    content TEXT,
    "mediaUrl" TEXT,
    "mediaPublicId" TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    geohash VARCHAR(12),
    "locationLabel" TEXT,
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "deletedAt" TIMESTAMP
);

CREATE INDEX "Message_conversationId_createdAt_idx" ON "Message"("conversationId", "createdAt");

-- 8. Solicitudes formales de adopción
CREATE TABLE "AdoptionRequest" (
    id TEXT PRIMARY KEY,
    "petId" TEXT NOT NULL REFERENCES "Pet"(id) ON DELETE CASCADE,
    "applicantId" TEXT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    "ownerId" TEXT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    "shelterId" TEXT REFERENCES "Shelter"(id) ON DELETE SET NULL,
    "conversationId" TEXT UNIQUE REFERENCES "Conversation"(id) ON DELETE SET NULL,
    status TEXT NOT NULL REFERENCES "AdoptionStatus"(id) DEFAULT 'PENDING',
    notes TEXT,
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX "AdoptionRequest_petId_status_idx" ON "AdoptionRequest"("petId", status);
CREATE INDEX "AdoptionRequest_applicantId_idx" ON "AdoptionRequest"("applicantId");
CREATE INDEX "AdoptionRequest_ownerId_idx" ON "AdoptionRequest"("ownerId");

-- 9. Moderación y Bloqueo UGC (requerido por Google Play)
CREATE TABLE "UserBlock" (
    id TEXT PRIMARY KEY,
    "blockerId" TEXT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    "blockedUserId" TEXT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    reason TEXT,
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT "UserBlock_blocker_blocked_uq" UNIQUE ("blockerId", "blockedUserId")
);

CREATE TABLE "ContentReport" (
    id TEXT PRIMARY KEY,
    "reporterId" TEXT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    "targetType" TEXT NOT NULL REFERENCES "ReportTargetType"(id),
    "targetId" TEXT NOT NULL,
    reason TEXT NOT NULL,
    status TEXT NOT NULL REFERENCES "ReportStatus"(id) DEFAULT 'OPEN',
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX "ContentReport_target_idx" ON "ContentReport"("targetType", "targetId");
CREATE INDEX "ContentReport_status_idx" ON "ContentReport"(status);
