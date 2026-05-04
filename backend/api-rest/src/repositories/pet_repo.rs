use sqlx::{Postgres, Executor};
use crate::models::entities::Pet;

pub struct PetRepository;

impl PetRepository {
    pub async fn find_by_id<'a, E>(executor: E, id: &str) -> Result<Option<Pet>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, Pet>(
            r#"SELECT id, name, gender, status, description, "speciesId" as species_id, "breedId" as breed_id, "reporterId" as reporter_id, "shelterId" as shelter_id, "createdAt" as created_at, "updatedAt" as updated_at, "lastLatitude" as last_latitude, "lastLongitude" as last_longitude, "lastGeohash" as last_geohash FROM "Pet" WHERE id = $1"#
        )
        .bind(id)
        .fetch_optional(executor)
        .await
    }

    pub async fn find_all<'a, E>(executor: E) -> Result<Vec<Pet>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, Pet>(
            r#"SELECT id, name, gender, status, description, "speciesId" as species_id, "breedId" as breed_id, "reporterId" as reporter_id, "shelterId" as shelter_id, "createdAt" as created_at, "updatedAt" as updated_at, "lastLatitude" as last_latitude, "lastLongitude" as last_longitude, "lastGeohash" as last_geohash FROM "Pet" ORDER BY "createdAt" DESC LIMIT 100"#
        )
        .fetch_all(executor)
        .await
    }

    pub async fn find_by_geohash_prefixes<'a, E>(executor: E, prefixes: &[String]) -> Result<Vec<Pet>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, Pet>(
            r#"SELECT id, name, gender, status, description, "speciesId" as species_id, "breedId" as breed_id, "reporterId" as reporter_id, "shelterId" as shelter_id, "createdAt" as created_at, "updatedAt" as updated_at, "lastLatitude" as last_latitude, "lastLongitude" as last_longitude, "lastGeohash" as last_geohash FROM "Pet" WHERE "lastGeohash" LIKE ANY($1)"#
        )
        .bind(prefixes)
        .fetch_all(executor)
        .await
    }

    pub async fn create<'a, E>(executor: E, pet: &Pet) -> Result<(), sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query(
            r#"INSERT INTO "Pet" (id, name, gender, status, description, "speciesId", "breedId", "reporterId", "shelterId", "lastLatitude", "lastLongitude", "lastGeohash", "createdAt", "updatedAt")
               VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)"#
        )
        .bind(&pet.id)
        .bind(&pet.name)
        .bind(pet.gender.clone())
        .bind(pet.status.clone())
        .bind(&pet.description)
        .bind(&pet.species_id)
        .bind(&pet.breed_id)
        .bind(&pet.reporter_id)
        .bind(&pet.shelter_id)
        .bind(pet.last_latitude)
        .bind(pet.last_longitude)
        .bind(&pet.last_geohash)
        .bind(pet.created_at)
        .bind(pet.updated_at)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn update<'a, E>(executor: E, pet_id: &str, pet: &Pet) -> Result<u64, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(
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
               WHERE id = $11"#
        )
        .bind(&pet.name)
        .bind(pet.gender.clone())
        .bind(pet.status.clone())
        .bind(&pet.description)
        .bind(&pet.species_id)
        .bind(&pet.breed_id)
        .bind(pet.last_latitude)
        .bind(pet.last_longitude)
        .bind(&pet.last_geohash)
        .bind(pet.updated_at)
        .bind(pet_id)
        .execute(executor)
        .await?;
        Ok(res.rows_affected())
    }

    pub async fn delete<'a, E>(executor: E, id: &str) -> Result<u64, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(r#"DELETE FROM "Pet" WHERE id = $1"#)
            .bind(id)
            .execute(executor)
            .await?;
        Ok(res.rows_affected())
    }
}
