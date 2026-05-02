use sqlx::{Postgres, Executor};
use crate::models::entities::Pet;

pub struct PetRepository;

impl PetRepository {
    pub async fn find_by_id<'a, E>(executor: E, id: &str) -> Result<Option<Pet>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as!(
            Pet,
            r#"SELECT id, name, gender as "gender: _", status as "status: _", description, "speciesId" as species_id, "breedId" as breed_id, "reporterId" as reporter_id, "shelterId" as shelter_id, "createdAt" as created_at, "updatedAt" as updated_at, "lastLatitude" as last_latitude, "lastLongitude" as last_longitude, "lastGeohash" as last_geohash FROM "Pet" WHERE id = $1"#,
            id
        )
        .fetch_optional(executor)
        .await
    }

    pub async fn find_all<'a, E>(executor: E) -> Result<Vec<Pet>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as!(
            Pet,
            r#"SELECT id, name, gender as "gender: _", status as "status: _", description, "speciesId" as species_id, "breedId" as breed_id, "reporterId" as reporter_id, "shelterId" as shelter_id, "createdAt" as created_at, "updatedAt" as updated_at, "lastLatitude" as last_latitude, "lastLongitude" as last_longitude, "lastGeohash" as last_geohash FROM "Pet" ORDER BY "createdAt" DESC LIMIT 100"#
        )
        .fetch_all(executor)
        .await
    }

    pub async fn find_by_geohash_prefixes<'a, E>(executor: E, prefixes: &[String]) -> Result<Vec<Pet>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as!(
            Pet,
            r#"SELECT id, name, gender as "gender: _", status as "status: _", description, "speciesId" as species_id, "breedId" as breed_id, "reporterId" as reporter_id, "shelterId" as shelter_id, "createdAt" as created_at, "updatedAt" as updated_at, "lastLatitude" as last_latitude, "lastLongitude" as last_longitude, "lastGeohash" as last_geohash FROM "Pet" WHERE "lastGeohash" LIKE ANY($1)"#,
            prefixes
        )
        .fetch_all(executor)
        .await
    }

    pub async fn create<'a, E>(executor: E, pet: &Pet) -> Result<(), sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query!(
            r#"INSERT INTO "Pet" (id, name, gender, status, description, "speciesId", "breedId", "reporterId", "shelterId", "lastLatitude", "lastLongitude", "lastGeohash", "createdAt", "updatedAt")
               VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)"#,
            pet.id, pet.name, pet.gender as _, pet.status as _, pet.description,
            pet.species_id, pet.breed_id, pet.reporter_id, pet.shelter_id,
            pet.last_latitude, pet.last_longitude, pet.last_geohash,
            pet.created_at, pet.updated_at
        )
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn update<'a, E>(executor: E, pet_id: &str, pet: &Pet) -> Result<u64, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query!(
            r#"UPDATE "Pet" SET 
               name = COALESCE($1, name), 
               gender = COALESCE($2, gender), 
               status = COALESCE($3, status),
               description = COALESCE($4, description),
               "speciesId" = COALESCE($5, "speciesId"),
               "breedId" = COALESCE($6, "breedId"),
               "lastLatitude" = COALESCE($7, "lastLatitude"),
               "lastLongitude" = COALESCE($8, "lastLongitude"),
               "lastGeohash" = COALESCE($9, "lastGeohash"),
               "updatedAt" = $10
               WHERE id = $11"#,
            pet.name, pet.gender as _, pet.status as _, pet.description,
            pet.species_id, pet.breed_id, pet.last_latitude, pet.last_longitude, pet.last_geohash,
            pet.updated_at, pet_id
        )
        .execute(executor)
        .await?;
        Ok(res.rows_affected())
    }

    pub async fn delete<'a, E>(executor: E, id: &str) -> Result<u64, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query!(r#"DELETE FROM "Pet" WHERE id = $1"#, id)
            .execute(executor)
            .await?;
        Ok(res.rows_affected())
    }
}
