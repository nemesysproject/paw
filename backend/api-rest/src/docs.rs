use utoipa::OpenApi;
use crate::models::entities::*;
use crate::models::pet::commands::*;
use crate::models::catalog::commands::*;
use crate::models::catalog::queries::*;
use crate::handlers::pet::commands as pet_handlers;
use crate::handlers::pet::queries as pet_queries;
use crate::handlers::catalog::commands as catalog_handlers;
use crate::handlers::catalog::queries as catalog_queries;
use crate::handlers::auth as auth_handlers;
use crate::handlers::ads as ad_handlers;
use crate::handlers::ai as ai_handlers;
use crate::models::auth::*;
use crate::models::ads::*;
use crate::models::ai::*;

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
        catalog_queries::get_all_genders,
        catalog_queries::get_all_statuses,
        auth_handlers::register,
        auth_handlers::login,
        auth_handlers::refresh,
        ad_handlers::get_active_ads,
        ad_handlers::record_ad_event,
        ad_handlers::get_ad_catalogs,
        ad_handlers::list_advertisers,
        ad_handlers::create_advertiser,
        ad_handlers::update_advertiser,
        ad_handlers::delete_advertiser,
        ad_handlers::list_campaigns,
        ad_handlers::get_campaign_by_id,
        ad_handlers::create_campaign,
        ad_handlers::upload_ad_image,
        ad_handlers::update_campaign,
        ad_handlers::delete_campaign,
        ai_handlers::chat_assistant,
    ),
    components(
        schemas(
            PetStatus, PetGender, UserRole, MediaType,
            User, Species, Breed, Pet, Media, Shelter, Veterinary,
            AdCategoryEntity, AdPlacementEntity, AdEventTypeEntity,
            Advertiser, AdCampaign, AdMetricEvent,
            CreatePetCommand, UpdatePetCommand, crate::models::pet::queries::PetDetailResponse,
            CreateSpeciesCommand, UpdateSpeciesCommand, CreateBreedCommand, UpdateBreedCommand,
            BreedQuery,
            RegisterRequest, LoginRequest, TokenResponse, RefreshRequest,
            CreateAdvertiserRequest, UpdateAdvertiserRequest,
            CreateAdCampaignRequest, UpdateAdCampaignRequest,
            RecordAdEventRequest, AdFeedItemResponse, AdCampaignSummaryResponse,
            AdCatalogsResponse, UploadAdImageResponse,
            AiChatMessage, AiChatRequest, AiNavigationAction, AiChatResponse
        )
    ),
    tags(
        (name = "Health", description = "Endpoints de estado del sistema"),
        (name = "Pets", description = "Gestión de mascotas y avistamientos"),
        (name = "Catalogs", description = "Gestión de especies y razas"),
        (name = "Auth", description = "Autenticación y gestión de usuarios"),
        (name = "Ads", description = "Publicidad geolocalizada, patrocinadores y campañas gubernamentales"),
        (name = "AI", description = "Copiloto IA para consultas y navegación asistida en el portal")
    ),
    info(
        title = "Save Puppy API",
        version = "1.0.0",
        description = "Backend REST para el proyecto Save Puppy (PAW)"
    )
)]
pub struct ApiDoc;
