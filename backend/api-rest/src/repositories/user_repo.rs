use sqlx::{Postgres, Executor};
use crate::models::entities::User;

pub struct UserRepository;

impl UserRepository {
    pub async fn find_by_email<'a, E>(executor: E, email: &str) -> Result<Option<User>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, User>(
            r#"SELECT id, email, password, name, role, "createdAt" as created_at, "updatedAt" as updated_at FROM "User" WHERE email = $1"#
        )
        .bind(email)
        .fetch_optional(executor)
        .await
    }

    pub async fn find_by_id<'a, E>(executor: E, id: &str) -> Result<Option<User>, sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query_as::<_, User>(
            r#"SELECT id, email, password, name, role, "createdAt" as created_at, "updatedAt" as updated_at FROM "User" WHERE id = $1"#
        )
        .bind(id)
        .fetch_optional(executor)
        .await
    }

    pub async fn create<'a, E>(executor: E, user: &User) -> Result<(), sqlx::Error> 
    where E: Executor<'a, Database = Postgres>
    {
        sqlx::query(
            r#"INSERT INTO "User" (id, email, password, name, role, "createdAt", "updatedAt") VALUES ($1, $2, $3, $4, $5, $6, $7)"#
        )
        .bind(&user.id)
        .bind(&user.email)
        .bind(&user.password)
        .bind(&user.name)
        .bind(&user.role)
        .bind(user.created_at)
        .bind(user.updated_at)
        .execute(executor)
        .await?;
        Ok(())
    }
}
