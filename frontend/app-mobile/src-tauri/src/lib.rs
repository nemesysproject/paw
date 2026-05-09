// Learn more about Tauri commands at https://tauri.app/develop/calling-rust/
#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_biometric::init())
        .plugin(tauri_plugin_sql::Builder::default().build())
        .plugin(tauri_plugin_stronghold::Builder::new(|password| {
            use argon2::{
                password_hash::{PasswordHasher, SaltString},
                Argon2,
            };
            let salt = SaltString::from_b64("static_salt_for_now").unwrap();
            let argon2 = Argon2::default();
            let hash = argon2.hash_password(password.as_bytes(), &salt).unwrap();
            hash.serialize().as_bytes().to_vec()
        }).build())
        .invoke_handler(tauri::generate_handler![greet])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
