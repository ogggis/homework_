package lt.code.academy;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public class ExamApp {

    private static final String EXAMS_COLLECTION = "exams";
    private static final String STATS_COLLECTION = "stats";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MongoDatabase database = ExamDB.getDatabase();
        MongoCollection<Document> examsCollection = database.getCollection(EXAMS_COLLECTION);
        MongoCollection<Document> statsCollection = database.getCollection(STATS_COLLECTION);

        while (true) {
            System.out.println("\nEnter a command:");
            System.out.println("1. Take an exam");
            System.out.println("2. Create an exam");
            System.out.println("3. Edit an exam");
            System.out.println("4. Delete an exam");
            System.out.println("5. View statistics");
            System.out.println("6. Quit");

            int option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1:
                    takeExam(statsCollection, examsCollection, scanner);
                    break;
                case 2:
                    createExam(examsCollection, scanner);
                    break;
                case 3:
                    editExam(examsCollection, scanner);
                    break;
                case 4:
                    deleteExam(examsCollection, statsCollection, scanner);
                    break;
                case 5:
                    viewStatistics(statsCollection, examsCollection, scanner);
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid option");
            }
        }
    }

    private static void takeExam(MongoCollection<Document> statsCollection, MongoCollection<Document> examsCollection, Scanner scanner) {
        System.out.println("Enter your name:");
        String name = scanner.nextLine();

        System.out.println("Select an exam:");
        List<String> examNames = new ArrayList<>();
        for (Document exam : examsCollection.find()) {
            examNames.add(exam.getString("name"));
            System.out.println((examNames.size()) + ". " + exam.getString("name"));
        }

        int examIndex = scanner.nextInt() - 1;
        scanner.nextLine();
        Document selectedExam = examsCollection.find(new Document("name", examNames.get(examIndex))).first();

        List<Document> questions = selectedExam.getList("questions", Document.class);
        List<Integer> answers = new ArrayList<>();

        for (int i = 0; i < questions.size() ; i++) {
            Document question = questions.get(i);
            System.out.println((i + 1) + ". " + question.getString("question"));
            List<String> choices = question.getList("choices", String.class);
            for (int j = 0; j < choices.size(); j++) {
                System.out.println((j + 1) + ". " + choices.get(j));
            }
            System.out.println("Enter your answer (1-3):");
            int userAnswer = scanner.nextInt();
            scanner.nextLine();
            answers.add(userAnswer);
        }

        int score = calculateScore(selectedExam, answers);

        Document statsDoc = new Document()
                .append("exam_name", selectedExam.getString("name"))
                .append("user_name", name)
                .append("score", score)
                .append("answers", answers);

        statsCollection.insertOne(statsDoc);

        System.out.println("Your score: " + score);
    }

    private static void editExam(MongoCollection<Document> examsCollection, Scanner scanner) {
        System.out.println("Enter the name of the exam to edit:");
        String examName = scanner.nextLine();
        Document exam = examsCollection.find(eq("name", examName)).first();
        if (exam == null) {
            System.out.println("Exam not found");
            return;
        }
        System.out.println("Enter the number of the question to edit:");
        int questionNumber = scanner.nextInt() -1 ;
        scanner.nextLine(); // consume newline character

        List<Document> questions = exam.getList("questions", Document.class);
        if (questionNumber < 0 || questionNumber >= questions.size()) {
            System.out.println("Invalid question number");
            return;
        }

        Document question = questions.get(questionNumber);

        System.out.println("Enter the new question:");
        String newQuestion = scanner.nextLine();
        question.put("question", newQuestion);

        List<String> choices = question.getList("choices", String.class);
        for (int i = 0; i < choices.size(); i++) {
            System.out.println("Enter the new text for choice " + (i + 1) + ":");
            String newChoice = scanner.nextLine();
            choices.set(i, newChoice);
        }
        question.put("choices", choices);

        System.out.println("Enter the index of the correct answer (1-3):");
        int answerIndex = scanner.nextInt() - 1;
        scanner.nextLine();
        question.put("answer", answerIndex);

        examsCollection.replaceOne(eq("name", examName), exam);

        System.out.println("Question updated successfully");
    }

    private static void deleteExam(MongoCollection<Document> examsCollection, MongoCollection<Document> statsCollection, Scanner scanner) {
        System.out.println("Enter the name of the exam to delete:");
        String examName = scanner.nextLine();

        Document exam = examsCollection.findOneAndDelete(eq("name", examName));
        if (exam == null) {
            System.out.println("Exam not found");
            return;
        }

        DeleteResult result = statsCollection.deleteMany(eq("exam_name", examName));
        long deletedCount = result.getDeletedCount();
        System.out.println(deletedCount + " statistics deleted");

        System.out.println("Exam deleted successfully");
    }


    private static void viewStatistics(MongoCollection<Document> statsCollection, MongoCollection<Document> examsCollection, Scanner scanner) {
        System.out.println("Enter the name of an exam to view its statistics, or press Enter to view overall statistics:");
        String examName = scanner.nextLine();

        if (!examName.isEmpty()) {
            Document exam = examsCollection.find(eq("name", examName)).first();
            if (exam == null) {
                System.out.println("Exam not found");
                return;
            }
            List<Document> stats = statsCollection.find(eq("exam_name", examName)).into(new ArrayList<>());
            if (stats.isEmpty()) {
                System.out.println("No statistics available for this exam");
                return;
            }
            int totalAttempts = stats.size();
            int totalCorrect = 0;
            Map<Integer, Integer> answerCounts = new HashMap<>();
            for (int i = 0; i < stats.size(); i++) {
                Document stat = stats.get(i);
                totalCorrect += stat.getInteger("score");
                List<Integer> answers = stat.getList("answers", Integer.class);
                for (int j = 0; j < answers.size(); j++) {
                    int answer = answers.get(j);
                    answerCounts.put(answer, answerCounts.getOrDefault(answer, 0) + 1);
                }
            }
            int totalQuestions = exam.getList("questions", Document.class).size();
            double averageScore = (double) (totalCorrect / totalAttempts)  ;
        double percentCorrect = (double) totalCorrect / (totalAttempts * totalQuestions) * 100;

            System.out.println("Statistics for exam: " + examName);
            System.out.println("Total attempts: " + totalAttempts);
            System.out.println("Average score: " + averageScore);
            System.out.println("Total correct: " + totalCorrect);
            System.out.println("Answer choice distribution:");
            for (int i = 1; i <= 3; i++) {
                int count = answerCounts.getOrDefault(i, 0);
                double percent = (double) count / (totalAttempts * totalQuestions) * 100;
                System.out.println(i + ": " + count + " (" + String.format("%.2f", percent) + "%)");
            }
        } else {
            List<Document> stats = statsCollection.find().into(new ArrayList<>());
            if (stats.isEmpty()) {
                System.out.println("No statistics available");
                return;
            }
            int totalAttempts = stats.size();
            int totalCorrect = 0;
            Map<String, Integer> examCounts = new HashMap<>();
            Map<Integer, Integer> answerCounts = new HashMap<>();
            for (int i = 0; i < stats.size(); i++) {
                Document stat = stats.get(i);
                String exam = stat.getString("exam_name");
                examCounts.put(exam, examCounts.getOrDefault(exam, 0) + 1);
                totalCorrect += stat.getInteger("score");
                List<Integer> answers = stat.getList("answers", Integer.class);
                for (int j = 0; j < answers.size(); j++) {
                    int answer = answers.get(j);
                    answerCounts.put(answer, answerCounts.getOrDefault(answer, 0) + 1);
                }
            }
            double averageScore = (double) (totalCorrect / totalAttempts) ;
            double percentCorrect = (double) totalCorrect / (totalAttempts * 5) * 100;
            System.out.println("Overall statistics:");
            System.out.println("Total attempts: " + totalAttempts);
            System.out.println("Average score: " + averageScore);
          System.out.println("Percent correct: " + String.format("%.2f", percentCorrect) + "%");
            System.out.println("Total correct:" + totalCorrect);
            System.out.println("Exam distribution:");
            for (Map.Entry<String, Integer> entry : examCounts.entrySet()) {
                String exam = entry.getKey();
                int count = entry.getValue();
                double percent = (double) count / totalAttempts * 100;
                System.out.println(exam + ": " + count + " (" + String.format("%.2f", percent) + "%)");
            }
            System.out.println("Answer choice distribution:");
            for (int i = 1; i <= 3; i++) {
                int count = answerCounts.getOrDefault(i, 0);
                double percent = (double) count / (totalAttempts * 5) * 100;
                System.out.println(i + ": " + count + " (" + String.format("%.2f", percent) + "%)");
            }
        }
    }

    private static int calculateScore(Document exam, List<Integer> answers) {
        List<Document> questions = exam.getList("questions", Document.class);
        int totalCorrect = 0;
        for (int i = 0; i <= questions.size() -1 ; i++) {
            Document question = questions.get(i);
            int correctAnswer = question.getInteger("answer");
            int userAnswer = answers.get(i);
            if (userAnswer == (correctAnswer)) {
                totalCorrect++;
            }
        }
        return totalCorrect * 2 ;
    }

    private static void createExam(MongoCollection<Document> examsCollection, Scanner scanner) {
        System.out.println("Enter the name of the new exam:");
        String examName = scanner.nextLine();

        List<Document> questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            System.out.println("Enter question " + i + ":");
            String questionText = scanner.nextLine();

            List<String> choices = new ArrayList<>();
            for (int j = 1; j <= 3; j++) {
                System.out.println("Enter choice " + j + " for question " + i + ":");
                String choiceText = scanner.nextLine();
                choices.add(choiceText);
            }

            System.out.println("Enter the number of the correct answer (1-3) for question " + i + ":");
            int correctIndex = scanner.nextInt() ;
            scanner.nextLine();

            Document question = new Document()
                    .append("question", questionText)
                    .append("choices", choices)
                    .append("answer", correctIndex);
            questions.add(question);
        }

        Document exam = new Document()
                .append("name", examName)
                .append("questions", questions);

        examsCollection.insertOne(exam);
        System.out.println("Exam created successfully");
    }

}



