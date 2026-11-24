-- Migración: Autenticación por redes sociales (Google Sign-In y proveedores federados)
-- Permite usuarios sin contraseña local y vinculación multi-proveedor mediante UserIdentity.

ALTER TABLE "User"
    ALTER COLUMN password DROP NOT NULL;

ALTER TABLE "User"
    ADD COLUMN IF NOT EXISTS "emailVerified" BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS "UserIdentity" (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL REFERENCES "User"(id) ON DELETE CASCADE,
    provider TEXT NOT NULL,
    "providerUserId" TEXT NOT NULL,
    email TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "UserIdentity_provider_providerUserId_key" UNIQUE (provider, "providerUserId")
);

CREATE INDEX IF NOT EXISTS "UserIdentity_userId_idx" ON "UserIdentity"("userId");
