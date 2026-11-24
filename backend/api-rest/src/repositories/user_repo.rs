use sqlx::{Postgres, Executor};
use crate::models::entities::{User, UserIdentity};

pub struct UserRepository;

impl UserRepository {
    pub async fn find_by_email<'a, E>(executor: E, email: &str) -> Result<Option<User>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, User>(
            r#"SELECT id, email, password, "emailVerified" as email_verified, name, phone, "avatarUrl" as avatar_url, role, "createdAt" as created_at, "updatedAt" as updated_at FROM "User" WHERE LOWER(email) = LOWER($1)"#,
        )
        .bind(email)
        .fetch_optional(executor)
        .await
    }

    pub async fn find_by_id<'a, E>(executor: E, id: &str) -> Result<Option<User>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, User>(
            r#"SELECT id, email, password, "emailVerified" as email_verified, name, phone, "avatarUrl" as avatar_url, role, "createdAt" as created_at, "updatedAt" as updated_at FROM "User" WHERE id = $1"#,
        )
        .bind(id)
        .fetch_optional(executor)
        .await
    }

    pub async fn create<'a, E>(executor: E, user: &User) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"INSERT INTO "User" (id, email, password, "emailVerified", name, "avatarUrl", role, "createdAt", "updatedAt") VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)"#,
        )
        .bind(&user.id)
        .bind(&user.email)
        .bind(&user.password)
        .bind(user.email_verified)
        .bind(&user.name)
        .bind(&user.avatar_url)
        .bind(&user.role)
        .bind(user.created_at)
        .bind(user.updated_at)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn find_identity<'a, E>(
        executor: E,
        provider: &str,
        provider_user_id: &str,
    ) -> Result<Option<UserIdentity>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, UserIdentity>(
            r#"SELECT id, "userId" as user_id, provider, "providerUserId" as provider_user_id, email, "createdAt" as created_at, "updatedAt" as updated_at FROM "UserIdentity" WHERE provider = $1 AND "providerUserId" = $2"#,
        )
        .bind(provider)
        .bind(provider_user_id)
        .fetch_optional(executor)
        .await
    }

    pub async fn link_identity<'a, E>(executor: E, identity: &UserIdentity) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"INSERT INTO "UserIdentity" (id, "userId", provider, "providerUserId", email, "createdAt", "updatedAt")
               VALUES ($1, $2, $3, $4, $5, $6, $7)
               ON CONFLICT (provider, "providerUserId") DO NOTHING"#,
        )
        .bind(&identity.id)
        .bind(&identity.user_id)
        .bind(&identity.provider)
        .bind(&identity.provider_user_id)
        .bind(&identity.email)
        .bind(identity.created_at)
        .bind(identity.updated_at)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn mark_verified_and_update_avatar<'a, E>(
        executor: E,
        user_id: &str,
        avatar_url: Option<&str>,
    ) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"UPDATE "User"
               SET "emailVerified" = TRUE,
                   "avatarUrl" = COALESCE("avatarUrl", $2),
                   "updatedAt" = CURRENT_TIMESTAMP
               WHERE id = $1"#,
        )
        .bind(user_id)
        .bind(avatar_url)
        .execute(executor)
        .await?;
        Ok(())
    }
}
