use axum::{
    routing::{get, post, put},
    Router,
};

use crate::handlers::ads;
use crate::AppState;

pub fn ad_routes() -> Router<AppState> {
    Router::new()
        // Consumo público (App Móvil)
        .route("/", get(ads::get_active_ads))
        .route("/catalogs", get(ads::get_ad_catalogs))
        .route("/:id/events", post(ads::record_ad_event))
        // Gestión de anunciantes (Admin JWT)
        .route(
            "/advertisers",
            get(ads::list_advertisers).post(ads::create_advertiser),
        )
        .route(
            "/advertisers/:id",
            put(ads::update_advertiser).delete(ads::delete_advertiser),
        )
        // Gestión de campañas y creatividades (Admin JWT)
        .route(
            "/campaigns",
            get(ads::list_campaigns).post(ads::create_campaign),
        )
        .route("/campaigns/upload-image", post(ads::upload_ad_image))
        .route(
            "/campaigns/:id",
            get(ads::get_campaign_by_id)
                .put(ads::update_campaign)
                .delete(ads::delete_campaign),
        )
}
