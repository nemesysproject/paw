use utoipa::ToSchema;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, sqlx::Type, ToSchema, Default)]
#[sqlx(type_name = "PetStatus", rename_all = "SCREAMING_SNAKE_CASE")]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum PetStatus {
    #[default]
    Lost,
    Found,
    Adopted,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, sqlx::Type, ToSchema, Default)]
#[sqlx(type_name = "PetGender", rename_all = "SCREAMING_SNAKE_CASE")]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum PetGender {
    #[default]
    Unknown,
    Male,
    Female,
}

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

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, sqlx::Type, ToSchema, Default)]
#[sqlx(type_name = "MediaType", rename_all = "SCREAMING_SNAKE_CASE")]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum MediaType {
    #[default]
    ReferencePhoto,
    SightingImage,
    SightingVideo,
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
        
        let deserialized: PetStatus = serde_json::from_str("\"FOUND\"").unwrap();
        assert_eq!(deserialized, PetStatus::Found);
    }
}
