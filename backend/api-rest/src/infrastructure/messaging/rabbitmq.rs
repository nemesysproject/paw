use lapin::{
    options::*, types::FieldTable, Connection, ConnectionProperties,
    ExchangeKind, BasicProperties, Channel
};
use serde::Serialize;
use tracing::info;

pub struct RabbitMQService {
    channel: Channel,
}

impl RabbitMQService {
    pub async fn new(uri: &str) -> Result<Self, lapin::Error> {
        let connection = Connection::connect(uri, ConnectionProperties::default()).await?;
        let channel = connection.create_channel().await?;

        channel
            .exchange_declare(
                "pet_events".into(),
                ExchangeKind::Topic,
                ExchangeDeclareOptions {
                    durable: true,
                    ..Default::default()
                },
                FieldTable::default(),
            )
            .await?;

        info!("Conectado a RabbitMQ");
        Ok(Self { channel })
    }

    pub async fn publish_event<T: Serialize>(&self, routing_key: &str, event: &T) -> Result<(), lapin::Error> {
        let payload = serde_json::to_vec(event).unwrap();
        
        self.channel
            .basic_publish(
                "pet_events".into(),
                routing_key.into(),
                BasicPublishOptions::default(),
                &payload,
                BasicProperties::default().with_delivery_mode(2),
            )
            .await?;

        info!("Evento publicado en RabbitMQ: {}", routing_key);
        Ok(())
    }

    pub async fn get_consumer(&self, queue_name: &str, routing_key: &str) -> Result<lapin::Consumer, lapin::Error> {
        let queue = self.channel
            .queue_declare(
                queue_name.into(),
                QueueDeclareOptions {
                    durable: true,
                    ..Default::default()
                },
                FieldTable::default(),
            )
            .await?;

        self.channel
            .queue_bind(
                queue.name().as_str().into(),
                "pet_events".into(),
                routing_key.into(),
                QueueBindOptions::default(),
                FieldTable::default(),
            )
            .await?;

        self.channel
            .basic_consume(
                queue.name().as_str().into(),
                "sync_worker".into(),
                BasicConsumeOptions::default(),
                FieldTable::default(),
            )
            .await
    }
}
