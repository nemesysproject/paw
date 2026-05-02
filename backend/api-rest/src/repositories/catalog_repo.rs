use sqlx::{Postgres, Executor};
use crate::models::entities::{Species, Breed};

pub struct CatalogRepository;

impl CatalogRepository {
    // --- Species ---

    pub async fn get_all_species<'a, E>(executor: E) -> Result<Vec<Species>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as!(
            Species,
            r#"SELECT id, name FROM "Species" ORDER BY name ASC"#
        )
        .fetch_all(executor)
        .await
    }

    pub async fn get_species_by_id<'a, E>(executor: E, id: &str) -> Result<Option<Species>, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as!(
            Species,
            r#"SELECT id, name FROM "Species" WHERE id = $1"#,
            id
        )
        .fetch_optional(executor)
        .await
    }

    pub async fn create_species<'a, E>(executor: E, species: &Species) -> Result<(), sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query!(
            r#"INSERT INTO "Species" (id, name) VALUES ($1, $2)"#,
            species.id,
            species.name
        )
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn update_species<'a, E>(executor: E, id: &str, species: &Species) -> Result<u64, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query!(
            r#"UPDATE "Species" SET name = $1 WHERE id = $2"#,
            species.name,
            id
        )
        .execute(executor)
        .await?;
        Ok(res.rows_affected())
    }

    pub async fn delete_species<'a, E>(executor: E, id: &str) -> Result<u64, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query!(
            r#"DELETE FROM "Species" WHERE id = $1"#,
            id
        )
        .execute(executor)
        .await?;
        Ok(res.rows_affected())
    }

    // --- Breeds ---

    pub async fn get_all_breeds<'a, E>(executor: E, species_id: Option<String>) -> Result<Vec<Breed>, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as!(
            Breed,
            r#"SELECT id, name, "speciesId" as species_id FROM "Breed" WHERE ($1::text IS NULL OR "speciesId" = $1) ORDER BY name ASC"#,
            species_id
        )
        .fetch_all(executor)
        .await
    }

    pub async fn get_breeds_by_species<'a, E>(executor: E, species_id: &str) -> Result<Vec<Breed>, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as!(
            Breed,
            r#"SELECT id, name, "speciesId" as species_id FROM "Breed" WHERE "speciesId" = $1 ORDER BY name ASC"#,
            species_id
        )
        .fetch_all(executor)
        .await
    }

    pub async fn get_breed_by_id<'a, E>(executor: E, id: &str) -> Result<Option<Breed>, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as!(
            Breed,
            r#"SELECT id, name, "speciesId" as species_id FROM "Breed" WHERE id = $1"#,
            id
        )
        .fetch_optional(executor)
        .await
    }

    pub async fn create_breed<'a, E>(executor: E, breed: &Breed) -> Result<(), sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query!(
            r#"INSERT INTO "Breed" (id, name, "speciesId") VALUES ($1, $2, $3)"#,
            breed.id,
            breed.name,
            breed.species_id
        )
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn update_breed<'a, E>(executor: E, id: &str, breed: &Breed) -> Result<u64, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query!(
            r#"UPDATE "Breed" SET name = $1, "speciesId" = $2 WHERE id = $3"#,
            breed.name,
            breed.species_id,
            id
        )
        .execute(executor)
        .await?;
        Ok(res.rows_affected())
    }

    pub async fn delete_breed<'a, E>(executor: E, id: &str) -> Result<u64, sqlx::Error>
    where E: Executor<'a, Database = Postgres>
    {
        let res = sqlx::query!(
            r#"DELETE FROM "Breed" WHERE id = $1"#,
            id
        )
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
}
