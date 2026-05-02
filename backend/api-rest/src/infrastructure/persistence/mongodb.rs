use mongodb::{Client, Database, Collection, options::ClientOptions};

pub struct MongoDBService {
    db: Database,
}

impl MongoDBService {
    pub async fn new(uri: &str, db_name: &str) -> Result<Self, mongodb::error::Error> {
        let client_options = ClientOptions::parse(uri).await?;
        let client = Client::with_options(client_options)?;
        let db = client.database(db_name);
        
        Ok(Self { db })
    }

    pub fn pets_collection(&self) -> Collection<mongodb::bson::Document> {
        self.db.collection("pets")
    }
}
