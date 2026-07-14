use utoipa::ToSchema;
use serde::{Deserialize, Serialize};

// Enum para el estado de la mascota (alineado con schema.prisma)
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, sqlx::Type, ToSchema, Default)]
#[sqlx(type_name = "PetStatus", rename_all = "SCREAMING_SNAKE_CASE")]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum PetStatus {
    #[default]
    Lost,       // Perdida
    Adoption,   // En adopción
    Street,     // Situación de calle
    AtRisk,     // En riesgo
    Safe,       // A salvo / Rescatada
}

// Enum para el género de la mascota (alineado con schema.prisma)
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, sqlx::Type, ToSchema, Default)]
#[sqlx(type_name = "PetGender", rename_all = "SCREAMING_SNAKE_CASE")]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum PetGender {
    Male,
    Female,
    #[default]
    Unknown,
}

// Enum para roles de usuario (alineado con schema.prisma)
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, sqlx::Type, ToSchema, Default)]
#[sqlx(type_name = "UserRole", rename_all = "SCREAMING_SNAKE_CASE")]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum UserRole {
    #[default]
    User,
    Admin,
    Vet,
    ShelterOwner,
}

// Enum para el tipo de medio (alineado con schema.prisma)
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, sqlx::Type, ToSchema, Default)]
#[sqlx(type_name = "MediaType", rename_all = "SCREAMING_SNAKE_CASE")]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum MediaType {
    #[default]
    ReferencePhoto,  // Foto subida por el dueño para buscar a la mascota
    SightingImage,   // Foto de un avistamiento en la calle
    SightingVideo,   // Video de un avistamiento
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_pet_status_default() {
        assert_eq!(PetStatus::default(), PetStatus::Lost);
    }

    #[test]
    fn test_pet_status_serialization() {
        let status = PetStatus::Lost;
        let serialized = serde_json::to_string(&status).unwrap();
        assert_eq!(serialized, "\"LOST\"");

        let deserialized: PetStatus = serde_json::from_str("\"AT_RISK\"").unwrap();
        assert_eq!(deserialized, PetStatus::AtRisk);
    }

    #[test]
    fn test_pet_gender_default() {
        assert_eq!(PetGender::default(), PetGender::Unknown);
    }

    #[test]
    fn test_user_role_default() {
        assert_eq!(UserRole::default(), UserRole::User);
    }
}
