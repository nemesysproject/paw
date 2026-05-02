use utoipa::OpenApi;
use crate::models::enums::*;
use crate::models::entities::*;
use crate::models::pet::commands::*;
use crate::models::catalog::commands::*;
use crate::models::catalog::queries::*;
use crate::handlers::pet::commands as pet_handlers;
use crate::handlers::pet::queries as pet_queries;
use crate::handlers::catalog::commands as catalog_handlers;
use crate::handlers::catalog::queries as catalog_queries;

#[derive(OpenApi)]
#[openapi(
    paths(
        crate::health_check,
        pet_handlers::create_pet,
        pet_handlers::update_pet,
        pet_handlers::delete_pet,
        pet_queries::list_pets,
        pet_queries::get_pet_by_id,
        pet_queries::search_pets_by_location,
        catalog_handlers::create_species,
        catalog_handlers::update_species,
        catalog_handlers::delete_species,
        catalog_handlers::create_breed,
        catalog_handlers::update_breed,
        catalog_handlers::delete_breed,
        catalog_queries::get_all_species,
        catalog_queries::get_all_breeds,
        catalog_queries::get_species_by_id,
        catalog_queries::get_breed_by_id,
        catalog_queries::get_breeds_by_species,
    ),
    components(
        schemas(
            PetStatus, PetGender, UserRole, MediaType,
            User, Species, Breed, Pet, Media, Shelter, Veterinary,
            CreatePetCommand, UpdatePetCommand, crate::models::pet::queries::PetDetailResponse,
            CreateSpeciesCommand, UpdateSpeciesCommand, CreateBreedCommand, UpdateBreedCommand,
            BreedQuery
        )
    ),
    tags(
        (name = "Health", description = "Endpoints de estado del sistema"),
        (name = "Pets", description = "Gestión de mascotas y avistamientos"),
        (name = "Catalogs", description = "Gestión de especies y razas")
    ),
    info(
        title = "Save Puppy API",
        version = "1.0.0",
        description = "Backend REST para el proyecto Save Puppy (PAW)"
    )
)]
pub struct ApiDoc;
