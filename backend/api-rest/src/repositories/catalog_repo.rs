use sqlx::{Postgres, Executor};
use crate::models::entities::{Species, Breed, PetGender, PetStatus, MediaTypeEntity};

pub struct CatalogRepository;

impl CatalogRepository {
    // --- Species ---

    pub async fn get_all_species<'a, E>(executor: E) -> Result<Vec<Species>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, Species>(r#"SELECT id, name FROM "Species" ORDER BY name ASC"#)
            .fetch_all(executor)
            .await
    }

    pub async fn get_species_by_id<'a, E>(executor: E, id: &str) -> Result<Option<Species>, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, Species>(r#"SELECT id, name FROM "Species" WHERE id = $1"#)
            .bind(id)
            .fetch_optional(executor)
            .await
    }

    pub async fn create_species<'a, E>(executor: E, species: &Species) -> Result<(), sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query(r#"INSERT INTO "Species" (id, name) VALUES ($1, $2)"#)
            .bind(&species.id)
            .bind(&species.name)
            .execute(executor)
            .await?;
        Ok(())
    }

    pub async fn update_species<'a, E>(executor: E, id: &str, species: &Species) -> Result<u64, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(r#"UPDATE "Species" SET name = $1 WHERE id = $2"#)
            .bind(&species.name)
            .bind(id)
            .execute(executor)
            .await?;
        Ok(res.rows_affected())
    }

    pub async fn delete_species<'a, E>(executor: E, id: &str) -> Result<u64, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(r#"DELETE FROM "Species" WHERE id = $1"#)
            .bind(id)
            .execute(executor)
            .await?;
        Ok(res.rows_affected())
    }

    // --- Breeds ---

    pub async fn get_all_breeds<'a, E>(executor: E, species_id: Option<String>) -> Result<Vec<Breed>, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, Breed>(r#"SELECT id, name, "speciesId" as species_id FROM "Breed" WHERE ($1::text IS NULL OR "speciesId" = $1) ORDER BY name ASC"#)
            .bind(species_id)
            .fetch_all(executor)
            .await
    }

    pub async fn get_breeds_by_species<'a, E>(executor: E, species_id: &str) -> Result<Vec<Breed>, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, Breed>(r#"SELECT id, name, "speciesId" as species_id FROM "Breed" WHERE "speciesId" = $1 ORDER BY name ASC"#)
            .bind(species_id)
            .fetch_all(executor)
            .await
    }

    pub async fn get_breed_by_id<'a, E>(executor: E, id: &str) -> Result<Option<Breed>, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, Breed>(r#"SELECT id, name, "speciesId" as species_id FROM "Breed" WHERE id = $1"#)
            .bind(id)
            .fetch_optional(executor)
            .await
    }

    pub async fn create_breed<'a, E>(executor: E, breed: &Breed) -> Result<(), sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query(r#"INSERT INTO "Breed" (id, name, "speciesId") VALUES ($1, $2, $3)"#)
            .bind(&breed.id)
            .bind(&breed.name)
            .bind(&breed.species_id)
            .execute(executor)
            .await?;
        Ok(())
    }

    pub async fn update_breed<'a, E>(executor: E, id: &str, breed: &Breed) -> Result<u64, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(r#"UPDATE "Breed" SET name = $1, "speciesId" = $2 WHERE id = $3"#)
            .bind(&breed.name)
            .bind(&breed.species_id)
            .bind(id)
            .execute(executor)
            .await?;
        Ok(res.rows_affected())
    }

    pub async fn delete_breed<'a, E>(executor: E, id: &str) -> Result<u64, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(r#"DELETE FROM "Breed" WHERE id = $1"#)
            .bind(id)
            .execute(executor)
            .await?;
        Ok(res.rows_affected())
    }

    // --- Upserts para el Seeder (ON CONFLICT DO NOTHING) ---

    /// Inserta una especie solo si no existe ya (idempotente).
    pub async fn upsert_species<'a, E>(executor: E, species: &Species) -> Result<bool, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(
            r#"INSERT INTO "Species" (id, name) VALUES ($1, $2) ON CONFLICT (id) DO NOTHING"#
        )
        .bind(&species.id)
        .bind(&species.name)
        .execute(executor)
        .await?;
        Ok(res.rows_affected() > 0)
    }

    /// Inserta una raza solo si no existe ya (idempotente).
    pub async fn upsert_breed<'a, E>(executor: E, breed: &Breed) -> Result<bool, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(
            r#"INSERT INTO "Breed" (id, name, "speciesId") VALUES ($1, $2, $3) ON CONFLICT (id) DO NOTHING"#
        )
        .bind(&breed.id)
        .bind(&breed.name)
        .bind(&breed.species_id)
        .execute(executor)
        .await?;
        Ok(res.rows_affected() > 0)
    }

    // --- Gender & Status ---

    pub async fn get_all_genders<'a, E>(executor: E) -> Result<Vec<PetGender>, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, PetGender>(r#"SELECT id, name FROM "PetGender" ORDER BY name ASC"#)
            .fetch_all(executor)
            .await
    }

    pub async fn get_all_statuses<'a, E>(executor: E) -> Result<Vec<PetStatus>, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, PetStatus>(r#"SELECT id, name FROM "PetStatus" ORDER BY name ASC"#)
            .fetch_all(executor)
            .await
    }

    pub async fn upsert_gender<'a, E>(executor: E, gender: &PetGender) -> Result<bool, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(
            r#"INSERT INTO "PetGender" (id, name) VALUES ($1, $2) ON CONFLICT (id) DO NOTHING"#
        )
        .bind(&gender.id)
        .bind(&gender.name)
        .execute(executor)
        .await?;
        Ok(res.rows_affected() > 0)
    }

    pub async fn upsert_status<'a, E>(executor: E, status: &PetStatus) -> Result<bool, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(
            r#"INSERT INTO "PetStatus" (id, name) VALUES ($1, $2) ON CONFLICT (id) DO NOTHING"#
        )
        .bind(&status.id)
        .bind(&status.name)
        .execute(executor)
        .await?;
        Ok(res.rows_affected() > 0)
    }

    pub async fn upsert_media_type<'a, E>(executor: E, media_type: &MediaTypeEntity) -> Result<bool, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query(
            r#"INSERT INTO "MediaType" (id, name) VALUES ($1, $2) ON CONFLICT (id) DO NOTHING"#
        )
        .bind(&media_type.id)
        .bind(&media_type.name)
        .execute(executor)
        .await?;
        Ok(res.rows_affected() > 0)
    }

    
}
