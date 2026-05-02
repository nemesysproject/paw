use sqlx::{Postgres, Executor};
use crate::models::entities::Media;

pub struct MediaRepository;

impl MediaRepository {
    pub async fn find_by_pet_id<'a, E>(executor: E, pet_id: &str) -> Result<Vec<Media>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as!(
            Media,
            r#"SELECT id, url, "publicId" as public_id, type as "type: _", "petId" as pet_id, latitude, longitude, geohash, "createdAt" as created_at FROM "Media" WHERE "petId" = $1 ORDER BY "createdAt" DESC"#,
            pet_id
        )
        .fetch_all(executor)
        .await
    }

    pub async fn create<'a, E>(executor: E, media: &Media) -> Result<(), sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query!(
            r#"INSERT INTO "Media" (id, url, "publicId", type, "petId", latitude, longitude, geohash, "createdAt")
               VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)"#,
            media.id, media.url, media.public_id, media.r#type as _, media.pet_id,
            media.latitude, media.longitude, media.geohash, media.created_at
        )
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn get_public_ids_by_pet_id<'a, E>(executor: E, pet_id: &str) -> Result<Vec<String>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        let rows = sqlx::query!(
            r#"SELECT "publicId" FROM "Media" WHERE "petId" = $1"#,
            pet_id
        )
        .fetch_all(executor)
        .await?;
        
        Ok(rows.into_iter().map(|r| r.publicId).collect())
    }
}
