use sqlx::{Postgres, Executor};
use crate::models::entities::Media;

pub struct MediaRepository;

impl MediaRepository {
    pub async fn find_by_pet_id<'a, E>(executor: E, pet_id: &str) -> Result<Vec<Media>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, Media>(
            r#"SELECT id, url, "publicId" as public_id, type as "type", "petId" as pet_id, latitude, longitude, geohash, "createdAt" as created_at FROM "Media" WHERE "petId" = $1 ORDER BY "createdAt" DESC"#
        )
        .bind(pet_id)
        .fetch_all(executor)
        .await
    }

    pub async fn create<'a, E>(executor: E, media: &Media) -> Result<(), sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query(
            r#"INSERT INTO "Media" (id, url, "publicId", type, "petId", latitude, longitude, geohash, "createdAt")
               VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)"#
        )
        .bind(&media.id)
        .bind(&media.url)
        .bind(&media.public_id)
        .bind(&media.r#type)
        .bind(&media.pet_id)
        .bind(media.latitude)
        .bind(media.longitude)
        .bind(&media.geohash)
        .bind(media.created_at)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn get_public_ids_by_pet_id<'a, E>(executor: E, pet_id: &str) -> Result<Vec<String>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        let rows = sqlx::query(r#"SELECT "publicId" FROM "Media" WHERE "petId" = $1"#)
            .bind(pet_id)
            .fetch_all(executor)
            .await?;
        
        use sqlx::Row;
        Ok(rows.into_iter().map(|r| r.get("publicId")).collect())
    }

    pub async fn find_by_pet_ids<'a, E>(executor: E, pet_ids: &[String]) -> Result<Vec<Media>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, Media>(
            r#"SELECT id, url, "publicId" as public_id, type as "type", "petId" as pet_id, latitude, longitude, geohash, "createdAt" as created_at 
               FROM "Media" 
               WHERE "petId" = ANY($1) 
               ORDER BY "createdAt" DESC"#
        )
        .bind(pet_ids)
        .fetch_all(executor)
        .await
    }
}
