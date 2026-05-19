package util;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger {

    private static final String LOG_FILE = "transaction_errors.log";

    // Logs an error message to the log file.
    // Demonstrates: IOException (checked), FileNotFoundException (checked)
    public static void logError(String message) {
        PrintWriter writer = null;
        try {
            // FileWriter(path, true) opens the file in append mode
            writer = new PrintWriter(new FileWriter(LOG_FILE, true));
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.println("[ERROR] [" + timestamp + "] " + message);
            writer.flush();
        } catch (FileNotFoundException e) {
            System.err.println("  Log file not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("  IO error while writing to log: " + e.getMessage());
        } finally {
            // finally block always runs — ensures writer is closed
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static void logInfo(String message) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(LOG_FILE, true));
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.println("[INFO]  [" + timestamp + "] " + message);
            writer.flush();
        } catch (IOException e) {
            System.err.println("  IO error while writing log: " + e.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
