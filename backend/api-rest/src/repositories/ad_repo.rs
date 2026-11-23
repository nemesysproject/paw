use sqlx::{Executor, PgPool, Postgres};

use crate::models::ads::AdFeedItemResponse;
use crate::models::entities::{
    AdCampaign, AdCategoryEntity, AdEventTypeEntity, AdMetricEvent, AdPlacementEntity, Advertiser,
};

pub struct AdRepository;

impl AdRepository {
    // --- Catalogs ---

    pub async fn get_categories<'a, E>(executor: E) -> Result<Vec<AdCategoryEntity>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, AdCategoryEntity>(r#"SELECT id, name FROM "AdCategory" ORDER BY name ASC"#)
            .fetch_all(executor)
            .await
    }

    pub async fn get_placements<'a, E>(executor: E) -> Result<Vec<AdPlacementEntity>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, AdPlacementEntity>(r#"SELECT id, name FROM "AdPlacement" ORDER BY name ASC"#)
            .fetch_all(executor)
            .await
    }

    pub async fn get_event_types<'a, E>(executor: E) -> Result<Vec<AdEventTypeEntity>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, AdEventTypeEntity>(r#"SELECT id, name FROM "AdEventType" ORDER BY name ASC"#)
            .fetch_all(executor)
            .await
    }

    // --- Advertisers ---

    pub async fn list_advertisers<'a, E>(executor: E) -> Result<Vec<Advertiser>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, Advertiser>(
            r#"
            SELECT
                id, name, category,
                "contactName" AS contact_name,
                email, phone, whatsapp, website,
                "logoUrl" AS logo_url,
                "logoPublicId" AS logo_public_id,
                "isGovernment" AS is_government,
                "officialSourceUrl" AS official_source_url,
                "veterinaryId" AS veterinary_id,
                "isActive" AS is_active,
                "createdAt" AS created_at,
                "updatedAt" AS updated_at
            FROM "Advertiser"
            ORDER BY "createdAt" DESC
            "#,
        )
        .fetch_all(executor)
        .await
    }

    pub async fn get_advertiser_by_id<'a, E>(
        executor: E,
        id: &str,
    ) -> Result<Option<Advertiser>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, Advertiser>(
            r#"
            SELECT
                id, name, category,
                "contactName" AS contact_name,
                email, phone, whatsapp, website,
                "logoUrl" AS logo_url,
                "logoPublicId" AS logo_public_id,
                "isGovernment" AS is_government,
                "officialSourceUrl" AS official_source_url,
                "veterinaryId" AS veterinary_id,
                "isActive" AS is_active,
                "createdAt" AS created_at,
                "updatedAt" AS updated_at
            FROM "Advertiser"
            WHERE id = $1
            "#,
        )
        .bind(id)
        .fetch_optional(executor)
        .await
    }

    pub async fn create_advertiser<'a, E>(
        executor: E,
        advertiser: &Advertiser,
    ) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"
            INSERT INTO "Advertiser" (
                id, name, category, "contactName", email, phone, whatsapp, website,
                "logoUrl", "logoPublicId", "isGovernment", "officialSourceUrl",
                "veterinaryId", "isActive", "createdAt", "updatedAt"
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16)
            "#,
        )
        .bind(&advertiser.id)
        .bind(&advertiser.name)
        .bind(&advertiser.category)
        .bind(&advertiser.contact_name)
        .bind(&advertiser.email)
        .bind(&advertiser.phone)
        .bind(&advertiser.whatsapp)
        .bind(&advertiser.website)
        .bind(&advertiser.logo_url)
        .bind(&advertiser.logo_public_id)
        .bind(advertiser.is_government)
        .bind(&advertiser.official_source_url)
        .bind(&advertiser.veterinary_id)
        .bind(advertiser.is_active)
        .bind(advertiser.created_at)
        .bind(advertiser.updated_at)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn update_advertiser<'a, E>(
        executor: E,
        id: &str,
        advertiser: &Advertiser,
    ) -> Result<u64, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        let res = sqlx::query(
            r#"
            UPDATE "Advertiser"
            SET
                name = $1,
                category = $2,
                "contactName" = $3,
                email = $4,
                phone = $5,
                whatsapp = $6,
                website = $7,
                "logoUrl" = $8,
                "logoPublicId" = $9,
                "isGovernment" = $10,
                "officialSourceUrl" = $11,
                "veterinaryId" = $12,
                "isActive" = $13,
                "updatedAt" = $14
            WHERE id = $15
            "#,
        )
        .bind(&advertiser.name)
        .bind(&advertiser.category)
        .bind(&advertiser.contact_name)
        .bind(&advertiser.email)
        .bind(&advertiser.phone)
        .bind(&advertiser.whatsapp)
        .bind(&advertiser.website)
        .bind(&advertiser.logo_url)
        .bind(&advertiser.logo_public_id)
        .bind(advertiser.is_government)
        .bind(&advertiser.official_source_url)
        .bind(&advertiser.veterinary_id)
        .bind(advertiser.is_active)
        .bind(advertiser.updated_at)
        .bind(id)
        .execute(executor)
        .await?;
        Ok(res.rows_affected())
    }

    pub async fn delete_advertiser<'a, E>(executor: E, id: &str) -> Result<u64, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        let res = sqlx::query(r#"DELETE FROM "Advertiser" WHERE id = $1"#)
            .bind(id)
            .execute(executor)
            .await?;
        Ok(res.rows_affected())
    }

    // --- Campaigns ---

    /// Devuelve anuncios activos y vigentes para la app móvil, filtrando por ubicación (Geohash)
    /// e incluyendo campañas globales (`geohash IS NULL`).
    pub async fn fetch_active_feed<'a, E>(
        executor: E,
        placement: Option<&str>,
        category: Option<&str>,
        geohash_patterns: Option<&[String]>,
        limit: i64,
    ) -> Result<Vec<AdFeedItemResponse>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        let patterns: Vec<String> = geohash_patterns
            .map(|p| p.to_vec())
            .unwrap_or_default();
        let has_geo = !patterns.is_empty();

        sqlx::query_as::<_, AdFeedItemResponse>(
            r#"
            SELECT
                c.id,
                c."advertiserId" AS advertiser_id,
                a.name AS advertiser_name,
                a."logoUrl" AS advertiser_logo_url,
                a."isGovernment" AS is_government,
                a."officialSourceUrl" AS official_source_url,
                c.title,
                c.description,
                c."ctaLabel" AS cta_label,
                c."targetUrl" AS target_url,
                c."imageUrl" AS image_url,
                c.category,
                c.placement,
                c."badgeLabel" AS badge_label,
                c.latitude,
                c.longitude,
                c."radiusKm" AS radius_km,
                c.priority,
                c."startsAt" AS starts_at,
                c."endsAt" AS ends_at
            FROM "AdCampaign" c
            INNER JOIN "Advertiser" a ON a.id = c."advertiserId"
            WHERE c."isActive" = TRUE
              AND a."isActive" = TRUE
              AND NOW() BETWEEN c."startsAt" AND c."endsAt"
              AND ($1::text IS NULL OR c.placement = $1)
              AND ($2::text IS NULL OR c.category = $2)
              AND (
                    NOT $3::boolean
                    OR c.geohash IS NULL
                    OR c.geohash LIKE ANY($4::text[])
                  )
            ORDER BY
                CASE WHEN c.geohash IS NOT NULL THEN 0 ELSE 1 END ASC,
                c.priority DESC,
                c."createdAt" DESC
            LIMIT $5
            "#,
        )
        .bind(placement)
        .bind(category)
        .bind(has_geo)
        .bind(&patterns)
        .bind(limit)
        .fetch_all(executor)
        .await
    }

    pub async fn list_campaigns<'a, E>(executor: E) -> Result<Vec<AdCampaign>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, AdCampaign>(
            r#"
            SELECT
                id,
                "advertiserId" AS advertiser_id,
                title, description,
                "ctaLabel" AS cta_label,
                "targetUrl" AS target_url,
                "imageUrl" AS image_url,
                "imagePublicId" AS image_public_id,
                category, placement,
                "badgeLabel" AS badge_label,
                latitude, longitude, geohash,
                "radiusKm" AS radius_km,
                priority,
                "startsAt" AS starts_at,
                "endsAt" AS ends_at,
                "isActive" AS is_active,
                "impressionsCount" AS impressions_count,
                "clicksCount" AS clicks_count,
                "createdAt" AS created_at,
                "updatedAt" AS updated_at
            FROM "AdCampaign"
            ORDER BY priority DESC, "createdAt" DESC
            "#,
        )
        .fetch_all(executor)
        .await
    }

    pub async fn get_campaign_by_id<'a, E>(
        executor: E,
        id: &str,
    ) -> Result<Option<AdCampaign>, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query_as::<_, AdCampaign>(
            r#"
            SELECT
                id,
                "advertiserId" AS advertiser_id,
                title, description,
                "ctaLabel" AS cta_label,
                "targetUrl" AS target_url,
                "imageUrl" AS image_url,
                "imagePublicId" AS image_public_id,
                category, placement,
                "badgeLabel" AS badge_label,
                latitude, longitude, geohash,
                "radiusKm" AS radius_km,
                priority,
                "startsAt" AS starts_at,
                "endsAt" AS ends_at,
                "isActive" AS is_active,
                "impressionsCount" AS impressions_count,
                "clicksCount" AS clicks_count,
                "createdAt" AS created_at,
                "updatedAt" AS updated_at
            FROM "AdCampaign"
            WHERE id = $1
            "#,
        )
        .bind(id)
        .fetch_optional(executor)
        .await
    }

    pub async fn create_campaign<'a, E>(
        executor: E,
        campaign: &AdCampaign,
    ) -> Result<(), sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        sqlx::query(
            r#"
            INSERT INTO "AdCampaign" (
                id, "advertiserId", title, description, "ctaLabel", "targetUrl",
                "imageUrl", "imagePublicId", category, placement, "badgeLabel",
                latitude, longitude, geohash, "radiusKm", priority,
                "startsAt", "endsAt", "isActive", "impressionsCount", "clicksCount",
                "createdAt", "updatedAt"
            )
            VALUES (
                $1, $2, $3, $4, $5, $6,
                $7, $8, $9, $10, $11,
                $12, $13, $14, $15, $16,
                $17, $18, $19, $20, $21,
                $22, $23
            )
            "#,
        )
        .bind(&campaign.id)
        .bind(&campaign.advertiser_id)
        .bind(&campaign.title)
        .bind(&campaign.description)
        .bind(&campaign.cta_label)
        .bind(&campaign.target_url)
        .bind(&campaign.image_url)
        .bind(&campaign.image_public_id)
        .bind(&campaign.category)
        .bind(&campaign.placement)
        .bind(&campaign.badge_label)
        .bind(campaign.latitude)
        .bind(campaign.longitude)
        .bind(&campaign.geohash)
        .bind(campaign.radius_km)
        .bind(campaign.priority)
        .bind(campaign.starts_at)
        .bind(campaign.ends_at)
        .bind(campaign.is_active)
        .bind(campaign.impressions_count)
        .bind(campaign.clicks_count)
        .bind(campaign.created_at)
        .bind(campaign.updated_at)
        .execute(executor)
        .await?;
        Ok(())
    }

    pub async fn update_campaign<'a, E>(
        executor: E,
        id: &str,
        campaign: &AdCampaign,
    ) -> Result<u64, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        let res = sqlx::query(
            r#"
            UPDATE "AdCampaign"
            SET
                title = $1,
                description = $2,
                "ctaLabel" = $3,
                "targetUrl" = $4,
                "imageUrl" = $5,
                "imagePublicId" = $6,
                category = $7,
                placement = $8,
                "badgeLabel" = $9,
                latitude = $10,
                longitude = $11,
                geohash = $12,
                "radiusKm" = $13,
                priority = $14,
                "startsAt" = $15,
                "endsAt" = $16,
                "isActive" = $17,
                "updatedAt" = $18
            WHERE id = $19
            "#,
        )
        .bind(&campaign.title)
        .bind(&campaign.description)
        .bind(&campaign.cta_label)
        .bind(&campaign.target_url)
        .bind(&campaign.image_url)
        .bind(&campaign.image_public_id)
        .bind(&campaign.category)
        .bind(&campaign.placement)
        .bind(&campaign.badge_label)
        .bind(campaign.latitude)
        .bind(campaign.longitude)
        .bind(&campaign.geohash)
        .bind(campaign.radius_km)
        .bind(campaign.priority)
        .bind(campaign.starts_at)
        .bind(campaign.ends_at)
        .bind(campaign.is_active)
        .bind(campaign.updated_at)
        .bind(id)
        .execute(executor)
        .await?;
        Ok(res.rows_affected())
    }

    pub async fn delete_campaign<'a, E>(executor: E, id: &str) -> Result<u64, sqlx::Error>
    where
        E: Executor<'a, Database = Postgres>,
    {
        let res = sqlx::query(r#"DELETE FROM "AdCampaign" WHERE id = $1"#)
            .bind(id)
            .execute(executor)
            .await?;
        Ok(res.rows_affected())
    }

    // --- Metrics ---

    /// Registra un evento (IMPRESSION / CLICK) y actualiza el contador en una transacción atómica.
    pub async fn record_metric_event(
        pool: &PgPool,
        event: &AdMetricEvent,
    ) -> Result<bool, sqlx::Error> {
        let mut tx = pool.begin().await?;

        let updated = match event.event_type.as_str() {
            "IMPRESSION" => {
                sqlx::query(
                    r#"UPDATE "AdCampaign" SET "impressionsCount" = "impressionsCount" + 1 WHERE id = $1"#,
                )
                .bind(&event.campaign_id)
                .execute(&mut *tx)
                .await?
                .rows_affected()
            }
            "CLICK" => {
                sqlx::query(
                    r#"UPDATE "AdCampaign" SET "clicksCount" = "clicksCount" + 1 WHERE id = $1"#,
                )
                .bind(&event.campaign_id)
                .execute(&mut *tx)
                .await?
                .rows_affected()
            }
            _ => 0,
        };

        if updated == 0 {
            tx.rollback().await?;
            return Ok(false);
        }

        sqlx::query(
            r#"
            INSERT INTO "AdMetricEvent" (id, "campaignId", "eventType", "userId", geohash, "createdAt")
            VALUES ($1, $2, $3, $4, $5, $6)
            "#,
        )
        .bind(&event.id)
        .bind(&event.campaign_id)
        .bind(&event.event_type)
        .bind(&event.user_id)
        .bind(&event.geohash)
        .bind(event.created_at)
        .execute(&mut *tx)
        .await?;

        tx.commit().await?;
        Ok(true)
    }
}
