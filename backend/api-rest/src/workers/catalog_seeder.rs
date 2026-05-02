use sqlx::PgPool;
use tracing::{info, warn};
use uuid::Uuid;

use crate::models::entities::{Breed, Species};
use crate::repositories::catalog_repo::CatalogRepository;

/// Datos estáticos del catálogo: (nombre_especie, [razas])
static CATALOG: &[(&str, &[&str])] = &[
    (
        "Perro",
        &[
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

/// Ejecuta el seeder de catálogos al inicio del servicio.
///
/// Es **idempotente**: usa `ON CONFLICT (id) DO NOTHING`, por lo que
/// puede llamarse en cada arranque sin duplicar registros.
pub async fn run_catalog_seed(pool: &PgPool) -> Result<(), sqlx::Error> {
    info!("🌱 Iniciando seeder de catálogos (Species & Breeds)...");

    let mut species_inserted = 0u32;
    let mut breeds_inserted = 0u32;

    for (species_name, breed_names) in CATALOG {
        // Generamos un UUID determinístico basado en el nombre para que sea
        // siempre el mismo ID aunque el seeder se ejecute varias veces.
        // Usamos UUIDv5 con namespace DNS para reproducibilidad.
        let species_id = deterministic_uuid(species_name);

        let species = Species {
            id: species_id.clone(),
            name: species_name.to_string(),
        };

        match CatalogRepository::upsert_species(pool, &species).await {
            Ok(true) => {
                info!("  ✅ Especie insertada: {}", species_name);
                species_inserted += 1;
            }
            Ok(false) => {
                info!("  ⏭️  Especie ya existente: {}", species_name);
            }
            Err(e) => {
                warn!("  ⚠️  Error insertando especie '{}': {}", species_name, e);
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
                    warn!("  ⚠️  Error insertando raza '{}': {}", breed_name, e);
                }
            }
        }
    }

    info!(
        "🌱 Seeder completado — Especies nuevas: {}, Razas nuevas: {}",
        species_inserted, breeds_inserted
    );
    Ok(())
}

/// Genera un UUID v5 (SHA-1, namespace OID) determinístico a partir de un string.
/// Mismo input → mismo UUID en cada ejecución.
fn deterministic_uuid(input: &str) -> String {
    Uuid::new_v5(&Uuid::NAMESPACE_OID, input.as_bytes()).to_string()
}
