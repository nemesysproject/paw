use axum::{extract::State, http::StatusCode, response::IntoResponse, Json};
use crate::AppState;
use crate::repositories::dashboard_repo::DashboardRepository;
use crate::models::dashboard::DashboardResponse;

#[utoipa::path(
    get,
    path = "/api/v1/dashboard",
    responses(
        (status = 200, description = "Dashboard data", body = DashboardResponse)
    ),
    tag = "Dashboard"
)]
pub async fn get_dashboard_data(State(state): State<AppState>) -> impl IntoResponse {
    let stats = match DashboardRepository::get_stats(&state.pool).await {
        Ok(s) => s,
        Err(e) => {
            tracing::error!("Error fetching dashboard stats: {:?}", e);
            return (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": "Error al obtener estadísticas"}))).into_response();
        }
    };

    let locations = match DashboardRepository::get_pet_locations(&state.pool).await {
        Ok(l) => l,
        Err(e) => {
            tracing::error!("Error fetching pet locations: {:?}", e);
            return (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": "Error al obtener ubicaciones"}))).into_response();
        }
    };

    (StatusCode::OK, Json(DashboardResponse { stats, locations })).into_response()
}
