-- Tablas de Catálogo (Sustituyen Enums para evitar errores de tipo y ser seedables)
CREATE TABLE "UserRole" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE "MediaType" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

-- Tablas de Catálogo (Sustituyen Enums anteriores para ser seedables)
CREATE TABLE "PetStatus" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE "PetGender" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

-- Table User
CREATE TABLE "User" (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    name TEXT NOT NULL,
    role TEXT NOT NULL REFERENCES "UserRole"(id) DEFAULT 'USER',
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table Species
CREATE TABLE "Species" (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

-- Table Breed
CREATE TABLE "Breed" (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    "speciesId" TEXT NOT NULL REFERENCES "Species"(id)
);

-- Table Shelter
CREATE TABLE "Shelter" (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table Veterinary
CREATE TABLE "Veterinary" (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table Pet
CREATE TABLE "Pet" (
    id TEXT PRIMARY KEY,
    name TEXT,
    gender TEXT NOT NULL REFERENCES "PetGender"(id),
    status TEXT NOT NULL REFERENCES "PetStatus"(id),
    description TEXT,
    "speciesId" TEXT NOT NULL REFERENCES "Species"(id),
    "breedId" TEXT REFERENCES "Breed"(id),
    "reporterId" TEXT NOT NULL,
    "shelterId" TEXT REFERENCES "Shelter"(id),
    "lastLatitude" DOUBLE PRECISION,
    "lastLongitude" DOUBLE PRECISION,
    "lastGeohash" VARCHAR(12),
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table Media
CREATE TABLE "Media" (
    id TEXT PRIMARY KEY,
    url TEXT NOT NULL,
    "publicId" TEXT NOT NULL,
    type TEXT NOT NULL REFERENCES "MediaType"(id),
    "petId" TEXT NOT NULL REFERENCES "Pet"(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    geohash VARCHAR(12),
    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX "Media_geohash_idx" ON "Media"(geohash);
