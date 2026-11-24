use chrono::NaiveDateTime;
use sqlx::{Executor, PgPool, Postgres, Row};

use crate::models::entities::{
    AdoptionRequest, ContentReport, Conversation, ConversationParticipant, DeviceToken, Message,
    PetSighting, UserBlock,
};
use crate::models::social::{
    AdoptionRequestResponse, ConversationSummaryResponse, MessageResponse, PetSightingResponse,
};

pub struct SocialRepository;

impl SocialRepository {
    // =========================================================================
    // Sightings
    // =========================================================================

    pub async fn list_sightings_by_pet<'a, E>(
        executor: E,
        pet_id: &str,
    ) -> Result<Vec<PetSightingResponse>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, PetSightingResponse>(
            r#"
            SELECT
                s.id,
                s."petId" AS pet_id,
                s."reporterId" AS reporter_id,
                u.name AS reporter_name,
                s.notes,
                s.latitude,
                s.longitude,
                s.geohash,
                s."imageUrl" AS image_url,
                s."sightedAt" AS sighted_at,
                s."isConfirmedByOwner" AS is_confirmed_by_owner,
                s."createdAt" AS created_at
            FROM "PetSighting" s
            LEFT JOIN "User" u ON u.id = s."reporterId"
            WHERE s."petId" = $1
            ORDER BY s."createdAt" DESC
            "#,
        )
        .bind(pet_id)
        .fetch_all(executor)
        .await
    }

    pub async fn create_sighting<'a, E>(
        executor: E,
        sighting: &PetSighting,
    ) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"
            INSERT INTO "PetSighting" (
                id, "petId", "reporterId", notes, latitude, longitude, geohash,
                "imageUrl", "imagePublicId", "sightedAt", "isConfirmedByOwner", "createdAt"
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
            "#,
        )
        .bind(&sighting.id)
        .bind(&sighting.pet_id)
        .bind(&sighting.reporter_id)
        .bind(&sighting.notes)
        .bind(sighting.latitude)
        .bind(sighting.longitude)
        .bind(&sighting.geohash)
        .bind(&sighting.image_url)
        .bind(&sighting.image_public_id)
        .bind(sighting.sighted_at)
        .bind(sighting.is_confirmed_by_owner)
        .bind(sighting.created_at)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn confirm_sighting<'a, E>(executor: E, id: &str) -> Result<u64, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        let res = sqlx::query(
            r#"UPDATE "PetSighting" SET "isConfirmedByOwner" = TRUE WHERE id = $1"#,
        )
        .bind(id)
        .execute(executor)
        .await?;
        Ok(res.rows_affected())
    }

    // =========================================================================
    // Conversations & Messages
    // =========================================================================

    pub async fn list_conversations_for_user<'a, E>(
        executor: E,
        user_id: &str,
    ) -> Result<Vec<ConversationSummaryResponse>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, ConversationSummaryResponse>(
            r#"
            SELECT
                c.id,
                c.type,
                c.status,
                c."petId" AS pet_id,
                p.name AS pet_name,
                c."shelterId" AS shelter_id,
                c."veterinaryId" AS veterinary_id,
                (
                    SELECT u.name
                    FROM "ConversationParticipant" cp2
                    INNER JOIN "User" u ON u.id = cp2."userId"
                    WHERE cp2."conversationId" = c.id AND cp2."userId" <> $1
                    LIMIT 1
                ) AS counterpart_name,
                (
                    SELECT COALESCE(m.content, m."locationLabel", '[' || m.type || ']')
                    FROM "Message" m
                    WHERE m."conversationId" = c.id AND m."deletedAt" IS NULL
                    ORDER BY m."createdAt" DESC
                    LIMIT 1
                ) AS last_message_preview,
                c."lastMessageAt" AS last_message_at,
                c."createdAt" AS created_at,
                c."updatedAt" AS updated_at
            FROM "Conversation" c
            INNER JOIN "ConversationParticipant" cp ON cp."conversationId" = c.id
            LEFT JOIN "Pet" p ON p.id = c."petId"
            WHERE cp."userId" = $1
            ORDER BY COALESCE(c."lastMessageAt", c."updatedAt") DESC
            "#,
        )
        .bind(user_id)
        .fetch_all(executor)
        .await
    }

    pub async fn create_conversation_with_participants(
        pool: &PgPool,
        conversation: &Conversation,
        participants: &[ConversationParticipant],
        initial_message: Option<&Message>,
    ) -> Result<(), sqlx::Error> {
        let mut tx = pool.begin().await?;

        sqlx::query(
            r#"
            INSERT INTO "Conversation" (
                id, type, status, "petId", "shelterId", "veterinaryId",
                "lastMessageAt", "createdAt", "updatedAt"
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
            "#,
        )
        .bind(&conversation.id)
        .bind(&conversation.r#type)
        .bind(&conversation.status)
        .bind(&conversation.pet_id)
        .bind(&conversation.shelter_id)
        .bind(&conversation.veterinary_id)
        .bind(conversation.last_message_at)
        .bind(conversation.created_at)
        .bind(conversation.updated_at)
        .execute(&mut *tx)
        .await?;

        for p in participants {
            sqlx::query(
                r#"
                INSERT INTO "ConversationParticipant" (
                    id, "conversationId", "userId", "lastReadAt", "isMuted", "joinedAt"
                )
                VALUES ($1, $2, $3, $4, $5, $6)
                ON CONFLICT ("conversationId", "userId") DO NOTHING
                "#,
            )
            .bind(&p.id)
            .bind(&p.conversation_id)
            .bind(&p.user_id)
            .bind(p.last_read_at)
            .bind(p.is_muted)
            .bind(p.joined_at)
            .execute(&mut *tx)
            .await?;
        }

        if let Some(msg) = initial_message {
            sqlx::query(
                r#"
                INSERT INTO "Message" (
                    id, "conversationId", "senderId", type, content,
                    "mediaUrl", "mediaPublicId", latitude, longitude, geohash,
                    "locationLabel", "createdAt", "deletedAt"
                )
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
                "#,
            )
            .bind(&msg.id)
            .bind(&msg.conversation_id)
            .bind(&msg.sender_id)
            .bind(&msg.r#type)
            .bind(&msg.content)
            .bind(&msg.media_url)
            .bind(&msg.media_public_id)
            .bind(msg.latitude)
            .bind(msg.longitude)
            .bind(&msg.geohash)
            .bind(&msg.location_label)
            .bind(msg.created_at)
            .bind(msg.deleted_at)
            .execute(&mut *tx)
            .await?;
        }

        tx.commit().await?;
        Ok(())
    }

    pub async fn is_participant<'a, E>(
        executor: E,
        conversation_id: &str,
        user_id: &str,
    ) -> Result<bool, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        let row = sqlx::query(
            r#"SELECT 1 FROM "ConversationParticipant" WHERE "conversationId" = $1 AND "userId" = $2"#,
        )
        .bind(conversation_id)
        .bind(user_id)
        .fetch_optional(executor)
        .await?;
        Ok(row.is_some())
    }

    pub async fn list_messages<'a, E>(
        executor: E,
        conversation_id: &str,
    ) -> Result<Vec<MessageResponse>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, MessageResponse>(
            r#"
            SELECT
                m.id,
                m."conversationId" AS conversation_id,
                m."senderId" AS sender_id,
                u.name AS sender_name,
                m.type,
                m.content,
                m."mediaUrl" AS media_url,
                m.latitude,
                m.longitude,
                m.geohash,
                m."locationLabel" AS location_label,
                m."createdAt" AS created_at
            FROM "Message" m
            LEFT JOIN "User" u ON u.id = m."senderId"
            WHERE m."conversationId" = $1 AND m."deletedAt" IS NULL
            ORDER BY m."createdAt" ASC
            "#,
        )
        .bind(conversation_id)
        .fetch_all(executor)
        .await
    }

    pub async fn mark_conversation_read<'a, E>(
        executor: E,
        conversation_id: &str,
        user_id: &str,
        read_at: NaiveDateTime,
    ) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"
            UPDATE "ConversationParticipant"
            SET "lastReadAt" = $1
            WHERE "conversationId" = $2 AND "userId" = $3
            "#,
        )
        .bind(read_at)
        .bind(conversation_id)
        .bind(user_id)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn send_message(pool: &PgPool, message: &Message) -> Result<(), sqlx::Error> {
        let mut tx = pool.begin().await?;

        sqlx::query(
            r#"
            INSERT INTO "Message" (
                id, "conversationId", "senderId", type, content,
                "mediaUrl", "mediaPublicId", latitude, longitude, geohash,
                "locationLabel", "createdAt", "deletedAt"
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
            "#,
        )
        .bind(&message.id)
        .bind(&message.conversation_id)
        .bind(&message.sender_id)
        .bind(&message.r#type)
        .bind(&message.content)
        .bind(&message.media_url)
        .bind(&message.media_public_id)
        .bind(message.latitude)
        .bind(message.longitude)
        .bind(&message.geohash)
        .bind(&message.location_label)
        .bind(message.created_at)
        .bind(message.deleted_at)
        .execute(&mut *tx)
        .await?;

        sqlx::query(
            r#"UPDATE "Conversation" SET "lastMessageAt" = $1, "updatedAt" = $1 WHERE id = $2"#,
        )
        .bind(message.created_at)
        .bind(&message.conversation_id)
        .execute(&mut *tx)
        .await?;

        tx.commit().await?;
        Ok(())
    }

    // =========================================================================
    // Adoptions
    // =========================================================================

    pub async fn find_pet_owner<'a, E>(
        executor: E,
        pet_id: &str,
    ) -> Result<Option<String>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        let row = sqlx::query(r#"SELECT "reporterId" FROM "Pet" WHERE id = $1"#)
            .bind(pet_id)
            .fetch_optional(executor)
            .await?;
        Ok(row.and_then(|r| r.try_get::<String, _>("reporterId").ok()))
    }

    pub async fn list_adoptions_for_user<'a, E>(
        executor: E,
        user_id: &str,
    ) -> Result<Vec<AdoptionRequestResponse>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, AdoptionRequestResponse>(
            r#"
            SELECT
                a.id,
                a."petId" AS pet_id,
                p.name AS pet_name,
                a."applicantId" AS applicant_id,
                u.name AS applicant_name,
                a."ownerId" AS owner_id,
                a."shelterId" AS shelter_id,
                a."conversationId" AS conversation_id,
                a.status,
                a.notes,
                a."createdAt" AS created_at,
                a."updatedAt" AS updated_at
            FROM "AdoptionRequest" a
            LEFT JOIN "Pet" p ON p.id = a."petId"
            LEFT JOIN "User" u ON u.id = a."applicantId"
            WHERE a."applicantId" = $1 OR a."ownerId" = $1
            ORDER BY a."updatedAt" DESC
            "#,
        )
        .bind(user_id)
        .fetch_all(executor)
        .await
    }

    pub async fn create_adoption_request<'a, E>(
        executor: E,
        req: &AdoptionRequest,
    ) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"
            INSERT INTO "AdoptionRequest" (
                id, "petId", "applicantId", "ownerId", "shelterId",
                "conversationId", status, notes, "createdAt", "updatedAt"
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
            "#,
        )
        .bind(&req.id)
        .bind(&req.pet_id)
        .bind(&req.applicant_id)
        .bind(&req.owner_id)
        .bind(&req.shelter_id)
        .bind(&req.conversation_id)
        .bind(&req.status)
        .bind(&req.notes)
        .bind(req.created_at)
        .bind(req.updated_at)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn update_adoption_status<'a, E>(
        executor: E,
        id: &str,
        status: &str,
        updated_at: NaiveDateTime,
    ) -> Result<u64, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        let res = sqlx::query(
            r#"UPDATE "AdoptionRequest" SET status = $1, "updatedAt" = $2 WHERE id = $3"#,
        )
        .bind(status)
        .bind(updated_at)
        .bind(id)
        .execute(executor)
        .await?;
        Ok(res.rows_affected())
    }

    // =========================================================================
    // Push Device Tokens & UGC Moderation
    // =========================================================================

    pub async fn upsert_device_token<'a, E>(
        executor: E,
        token: &DeviceToken,
    ) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"
            INSERT INTO "DeviceToken" (id, "userId", "fcmToken", platform, "createdAt", "updatedAt")
            VALUES ($1, $2, $3, $4, $5, $6)
            ON CONFLICT ("fcmToken")
            DO UPDATE SET "userId" = EXCLUDED."userId", platform = EXCLUDED.platform, "updatedAt" = EXCLUDED."updatedAt"
            "#,
        )
        .bind(&token.id)
        .bind(&token.user_id)
        .bind(&token.fcm_token)
        .bind(&token.platform)
        .bind(token.created_at)
        .bind(token.updated_at)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn create_user_block<'a, E>(executor: E, block: &UserBlock) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"
            INSERT INTO "UserBlock" (id, "blockerId", "blockedUserId", reason, "createdAt")
            VALUES ($1, $2, $3, $4, $5)
            ON CONFLICT ("blockerId", "blockedUserId") DO NOTHING
            "#,
        )
        .bind(&block.id)
        .bind(&block.blocker_id)
        .bind(&block.blocked_user_id)
        .bind(&block.reason)
        .bind(block.created_at)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn create_content_report<'a, E>(
        executor: E,
        report: &ContentReport,
    ) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"
            INSERT INTO "ContentReport" (
                id, "reporterId", "targetType", "targetId", reason, status, "createdAt", "updatedAt"
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            "#,
        )
        .bind(&report.id)
        .bind(&report.reporter_id)
        .bind(&report.target_type)
        .bind(&report.target_id)
        .bind(&report.reason)
        .bind(&report.status)
        .bind(report.created_at)
        .bind(report.updated_at)
        .execute(executor)
        .await?;
        Ok(())
    }
}
