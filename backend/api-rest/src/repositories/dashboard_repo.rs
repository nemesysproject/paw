use sqlx::{PgPool, Row};
use crate::models::dashboard::{DashboardStats, PetLocation};

pub struct DashboardRepository;

impl DashboardRepository {
    pub async fn get_stats(pool: &PgPool) -> Result<DashboardStats, sqlx::Error> {
        let row = sqlx::query(
            r#"
            SELECT 
                COUNT(*) FILTER (WHERE status = 'LOST') as total_lost,
                COUNT(*) FILTER (WHERE status = 'FOUND') as total_found,
                COUNT(*) FILTER (WHERE status = 'ADOPTED') as total_adopted,
                COUNT(*) as total_pets
            FROM "Pet"
            "#
        )
        .fetch_one(pool)
        .await?;

        Ok(DashboardStats {
            total_lost: row.get("total_lost"),
            total_found: row.get("total_found"),
            total_adopted: row.get("total_adopted"),
            total_pets: row.get("total_pets"),
        })
    }

    pub async fn get_pet_locations(pool: &PgPool) -> Result<Vec<PetLocation>, sqlx::Error> {
        let locations = sqlx::query_as::<_, PetLocation>(
            r#"
            SELECT 
                p.id, p.name, p.status, p."lastLatitude" as last_latitude, p."lastLongitude" as last_longitude,
                (SELECT url FROM "Media" WHERE pet_id = p.id LIMIT 1) as image_url
            FROM "Pet" p
            WHERE p."lastLatitude" IS NOT NULL AND p."lastLongitude" IS NOT NULL
            "#
        )
        .fetch_all(pool)
        .await?;

        Ok(locations)
    }
}
