// Módulo para inicializar SQLx
// pub async fn init_db() -> Result<sqlx::PgPool, sqlx::Error> {
//     let database_url = std::env::var("DATABASE_URL").expect("DATABASE_URL must be set");
//     sqlx::postgres::PgPoolOptions::new()
//         .max_connections(5)
//         .connect(&database_url)
//         .await
// }
