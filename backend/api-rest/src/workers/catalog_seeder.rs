use sqlx::PgPool;
use tracing::{info, error};
use uuid::Uuid;

use crate::models::entities::{Breed, Species, PetGender, PetStatus, MediaTypeEntity, UserRole};
use crate::repositories::catalog_repo::CatalogRepository;

static ROLES: &[(&str, &str)] = &[
    ("USER", "Usuario Estándar"),
    ("ADMIN", "Administrador de Sistema"),
];

static MEDIA_TYPES: &[(&str, &str)] = &[
    ("ReferencePhoto", "Foto de Referencia"),
    ("SightingImage", "Imagen de Avistamiento"),
    ("SightingVideo", "Video de Avistamiento"),
];


/// Datos estáticos del catálogo: (nombre_especie, [razas])
static CATALOG: &[(&str, &[&str])] = &[
    (
        "Perro",
        &[
            "Mestizo",
            "Labrador Retriever",
            "Golden Retriever",
            "Pastor Alemán",
            "Bulldog Francés",
            "Beagle",
            "Poodle",
            "Rottweiler",
            "Yorkshire Terrier",
            "Chihuahua",
            "Dóberman",
            "Boxer",
            "Shih Tzu",
            "Husky Siberiano",
            "Maltés",
            "Pomerania",
            "Dálmata",
            "Cocker Spaniel",
            "Schnauzer",
            "Shar Pei",
            "Border Collie",
        ],
    ),
    (
        "Gato",
        &[
            "Mestizo",
            "Siamés",
            "Persa",
            "Maine Coon",
            "Bengalí",
            "Ragdoll",
            "Abisinio",
            "British Shorthair",
            "Sphynx",
            "Scottish Fold",
            "Burmés",
            "Angora Turco",
            "Ruso Azul",
            "Noruego del Bosque",
            "Himalayo",
            "Savannah",
            "Devon Rex",
            "Tonkinés",
            "Somalí",
            "Manx",
            "Bombay",
        ],
    ),
];

static GENDERS: &[(&str, &str)] = &[
    ("MALE", "Macho"),
    ("FEMALE", "Hembra"),
    ("UNKNOWN", "Desconocido"),
];

static STATUSES: &[(&str, &str)] = &[
    ("LOST", "Perdido"),
    ("FOUND", "Encontrado"),
    ("ADOPTED", "Adoptado"),
];

/// Ejecuta el seeder de catálogos al inicio del servicio.
///
/// Es **idempotente**: usa `ON CONFLICT (id) DO NOTHING`, por lo que
/// puede llamarse en cada arranque sin duplicar registros.
pub async fn run_catalog_seed(pool: &PgPool) -> Result<(), sqlx::Error> {
    info!("🌱 Iniciando seeder de catálogos (Species & Breeds)...");

    let mut species_inserted = 0u32;
    let mut breeds_inserted = 0u32;
    let mut genders_inserted = 0u32;
    let mut statuses_inserted = 0u32;
    let mut roles_inserted = 0u32;

    // 0. Roles
    for (id, name) in ROLES {
        let role = UserRole { id: id.to_string(), name: name.to_string() };
        if CatalogRepository::upsert_user_role(pool, &role).await? {
            info!("  ✅ Rol insertado: {}", name);
            roles_inserted += 1;
        }
    }

    // 1. Genders
    for (id, name) in GENDERS {
        let gender = PetGender { id: id.to_string(), name: name.to_string() };
        if CatalogRepository::upsert_gender(pool, &gender).await? {
            info!("  ✅ Género insertado: {}", name);
            genders_inserted += 1;
        }
    }

    // 2. Statuses
    for (id, name) in STATUSES {
        let status = PetStatus { id: id.to_string(), name: name.to_string() };
        if CatalogRepository::upsert_status(pool, &status).await? {
            info!("  ✅ Estado insertado: {}", name);
            statuses_inserted += 1;
        }
    }

    // 3. Species & Breeds
    for (species_name, breed_names) in CATALOG {
        // Generamos un UUID determinístico basado en el nombre para que sea
        // siempre el mismo ID aunque el seeder se ejecute varias veces.
        let species_id = deterministic_uuid(species_name);

        let species = Species {
            id: species_id.clone(),
            name: species_name.to_string(),
        };

        match CatalogRepository::upsert_species(pool, &species).await {
            Ok(true) => {
                info!("  ✅ Especie insertada: {} (ID: {})", species_name, species_id);
                species_inserted += 1;
            }
            Ok(false) => {
                info!("  ⏭️  Especie ya existente: {}", species_name);
            }
            Err(e) => {
                error!("  ❌ Error CRÍTICO insertando especie '{}': {:?}", species_name, e);
                return Err(e);
            }
        }

        for breed_name in *breed_names {
            let breed_id = deterministic_uuid(&format!("{}::{}", species_name, breed_name));

            let breed = Breed {
                id: breed_id,
                name: breed_name.to_string(),
                species_id: species_id.clone(),
            };

            match CatalogRepository::upsert_breed(pool, &breed).await {
                Ok(true) => {
                    breeds_inserted += 1;
                }
                Ok(false) => {} // Ya existía, silencio
                Err(e) => {
                    error!("  ❌ Error CRÍTICO insertando raza '{}': {:?}", breed_name, e);
                    return Err(e);
                }
            }
        }
    }

    let mut media_types_inserted = 0u32;
    // ... (tus otras variables de conteo)

    // 1. Media Types
    for (id, name) in MEDIA_TYPES {
        // Asumiendo que tienes una entidad MediaType definida similar a PetGender
        let m_type = MediaTypeEntity { 
            id: id.to_string(), 
            name: name.to_string() 
        };
        
        if CatalogRepository::upsert_media_type(pool, &m_type).await? {
            info!("  ✅ Tipo de medio insertado: {}", name);
            media_types_inserted += 1;
        }
    }

    info!(
        "🌱 Seeder completado — Especies: {}, Razas: {}, Géneros: {}, Estados: {}, Roles: {}, Tipos Medio: {}",
        species_inserted, breeds_inserted, genders_inserted, statuses_inserted, roles_inserted, media_types_inserted
    );
    Ok(())
}

/// Genera un UUID v5 (SHA-1, namespace OID) determinístico a partir de un string.
/// Mismo input → mismo UUID en cada ejecución.
fn deterministic_uuid(input: &str) -> String {
    Uuid::new_v5(&Uuid::NAMESPACE_OID, input.as_bytes()).to_string()
}
