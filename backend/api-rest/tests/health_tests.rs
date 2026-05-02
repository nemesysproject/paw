use axum_test::TestServer;
use api_rest::{connect_postgres, create_app};
use axum::http::StatusCode;

#[tokio::test]
async fn test_health_check() {
    // Para que este test funcione en un entorno local, las DBs deben estar levantadas
    // o debemos usar mocks. Por ahora, asumimos que las DBs de dev están accesibles.
    let pool = connect_postgres().await;
    let app = create_app(pool).await;
    let server = TestServer::new(app).unwrap();

    let response = server.get("/health").await;
    response.assert_status(StatusCode::OK);
    response.assert_text("OK - API Rest (Rust) is running");
}
