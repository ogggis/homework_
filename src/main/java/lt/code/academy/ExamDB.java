package lt.code.academy;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class ExamDB {
    private static final String DB_NAME = "exam_db";

    private static MongoClient mongoClient = null;

    private ExamDB() {

    }

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            mongoClient = new MongoClient();
        }
        return mongoClient.getDatabase(DB_NAME);
    }
}
