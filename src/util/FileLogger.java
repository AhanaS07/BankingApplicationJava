package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logging utility for the Banking Application.
 *
 * Backed by Log4j2 (configured in resources/log4j2.xml). Every message is
 * written to BOTH the terminal (Console appender) and the log file
 * logs/banking-app.log (RollingFile appender).
 *
 * The public API (logInfo / logError) is unchanged, so existing callers
 * such as BankingService keep working without any modification.
 */
public class FileLogger {

    private static final Logger LOGGER = LogManager.getLogger("BankingApp");

    private FileLogger() {
        // utility class — no instances
    }

    // Logs an informational message to terminal + file.
    public static void logInfo(String message) {
        LOGGER.info(message);
    }

    // Logs an error message to terminal + file.
    public static void logError(String message) {
        LOGGER.error(message);
    }
}
