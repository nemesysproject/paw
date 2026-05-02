use sqlx::PgPool;
use crate::models::entities::User;

pub struct UserRepository<'a> {
    pool: &'a PgPool,
}

impl<'a> UserRepository<'a> {
    pub fn new(pool: &'a PgPool) -> Self {
        Self { pool }
    }

    pub async fn find_by_id(&self, id: String) -> Result<Option<User>, sqlx::Error> {
        sqlx::query_as!(
            User,
            r#"SELECT id, email, password, name, role as "role: _", "createdAt" as created_at, "updatedAt" as updated_at FROM "User" WHERE id = $1"#,
            id
        )
        .fetch_optional(self.pool)
        .await
    }

    // El ID se asume generado por Prisma (uuid) pero en insert lo podemos dejar delegar o pasar explícito
    pub async fn create(&self, user: &User) -> Result<(), sqlx::Error> {
        sqlx::query!(
            r#"
            INSERT INTO "User" (id, email, password, name, role, "createdAt", "updatedAt")
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            "#,
            user.id,
            user.email,
            user.password,
            user.name,
            user.role as _,
            user.created_at,
            user.updated_at
        )
        .execute(self.pool)
        .await?;
        Ok(())
    }
}
