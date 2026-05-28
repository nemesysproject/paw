

// Learn more about Tauri commands at https://tauri.app/develop/calling-rust/
#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    #[allow(unused_mut)]
    let mut builder = tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_sql::Builder::default().build());

    #[cfg(mobile)]
    {
        builder = builder.plugin(tauri_plugin_biometric::init());
    }

    builder
        .invoke_handler(tauri::generate_handler![greet, copy_android_content_to_local])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}



// Limpieza de la cabecera del archivo para evitar duplicados e imports innecesarios

#[tauri::command]
async fn copy_android_content_to_local(
    _app: tauri::AppHandle,
    source_uri: String,
    dest_path: String,
) -> Result<(), String> {
    #[cfg(target_os = "android")]
    {
        use jni::objects::{JObject, JValue};
        use jni::{JavaVM, objects::ReleaseMode};
        use std::fs::File;
        use std::io::Write;
        
        // Acceder a los punteros nativos usando ndk_context
        let ndk_ctx = ndk_context::android_context();
        let vm = unsafe { JavaVM::from_raw(ndk_ctx.vm().cast()) }
            .map_err(|e| format!("Error obteniendo JavaVM: {:?}", e))?;
        let mut env = vm.attach_current_thread()
            .map_err(|e| format!("Error adjuntando hilo: {:?}", e))?;
        let context = unsafe { JObject::from_raw(ndk_ctx.context().cast()) };

        // 1. Obtener el ContentResolver de Android (Pasando referencia &context)
        let resolver = env
            .call_method(
                &context,
                "getContentResolver",
                "()Landroid/content/ContentResolver;", // Signature correcta
                &[],
            )
            .map_err(|e| format!("Error obteniendo ContentResolver: {:?}", e))?
            .l()
            .unwrap();

        // 2. Parsear la cadena URI a un objeto android.net.Uri
        let uri_string = env.new_string(&source_uri).map_err(|e| e.to_string())?;
        let uri_obj = env
            .call_static_method(
                "android/net/Uri",
                "parse",
                "(Ljava/lang/String;)Landroid/net/Uri;",
                &[(&uri_string).into()],
            )
            .map_err(|e| format!("Error parseando URI: {:?}", e))?
            .l()
            .unwrap();

        // 3. Abrir el InputStream (CORRECCIÓN: Se usa &resolver para no perder la propiedad del objeto)
        let input_stream = env
            .call_method(
                &resolver,
                "openInputStream",
                "(Landroid/net/Uri;)Ljava/io/InputStream;",
                &[(&uri_obj).into()],
            )
            .map_err(|e| format!("Error abriendo InputStream (posible falta de permisos): {:?}", e))?
            .l()
            .unwrap();

        // 4. Leer el flujo de datos y guardarlo en el archivo destino
        let mut dest_file = File::create(&dest_path)
            .map_err(|e| format!("Error creando archivo destino: {:?}", e))?;

        // Inicializar búfer de transferencia (4KB)
        let java_buffer = env.new_byte_array(4096)
            .map_err(|e| format!("Error creando búfer: {:?}", e))?;

        loop {
            // Se pasa &input_stream por referencia para poder reusarlo en el bucle
            let read_bytes = env.call_method(
                &input_stream,
                "read",
                "([B)I",
                &[JValue::Object(&java_buffer)],
            )
            .map_err(|e| format!("Error leyendo del stream: {:?}", e))?
            .i()
            .unwrap();

            if read_bytes == -1 {
                break;
            }

            // Optimización: Usar get_array_elements para evitar copias Vec innecesarias
            let auto_array = unsafe { env.get_array_elements(&java_buffer, ReleaseMode::NoCopyBack) }
                .map_err(|e| e.to_string())?;
            
            let slice = unsafe { std::slice::from_raw_parts(auto_array.as_ptr() as *const u8, read_bytes as usize) };
            dest_file.write_all(slice).map_err(|e| e.to_string())?;
        }

        // Cerrar el flujo formalmente en Java
        let _ = env.call_method(&input_stream, "close", "()V", &[]);

        return Ok(());
    }

    #[cfg(not(target_os = "android"))]
    {
        let _ = source_uri;
        let _ = dest_path;
        Err("Plataforma no soportada".to_string())
    }
}
