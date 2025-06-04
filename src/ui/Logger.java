package ui;

/**
 * Simple logging utility for the Pac-Man application. This replaces direct System.out.println calls
 * with a more structured approach.
 */
public final class Logger {

  /** Private constructor to prevent instantiation. */
  private Logger() {
    // Utility class
  }

  /** Enable info logging. */
  private static final boolean INFO_ENABLED = true;
  /** Enable debug logging. */
  private static final boolean DEBUG_ENABLED = true;
  /** Enable warning logging. */
  private static final boolean WARN_ENABLED = true;
  /** Enable error logging. */
  private static final boolean ERROR_ENABLED = true;
  /** PMD SystemPrintln suppression annotation string. */
  private static final String PMD_SYSTEM_PRINTLN = "PMD.SystemPrintln";

  /**
   * Check if info logging is enabled.
   *
   * @return true if info logging is enabled
   */
  public static boolean isInfoEnabled() {
    return INFO_ENABLED;
  }

  /**
   * Check if debug logging is enabled.
   *
   * @return true if debug logging is enabled
   */
  public static boolean isDebugEnabled() {
    return DEBUG_ENABLED;
  }

  /**
   * Check if warning logging is enabled.
   *
   * @return true if warning logging is enabled
   */
  public static boolean isWarnEnabled() {
    return WARN_ENABLED;
  }

  /**
   * Check if error logging is enabled.
   *
   * @return true if error logging is enabled
   */
  public static boolean isErrorEnabled() {
    return ERROR_ENABLED;
  }

  /**
   * Logs an informational message.
   *
   * @param message the message to log
   */
  @SuppressWarnings(PMD_SYSTEM_PRINTLN)
  public static void info(String message) {
    System.out.println(message);
  }

  /**
   * Logs an error message.
   *
   * @param message the error message to log
   */
  @SuppressWarnings(PMD_SYSTEM_PRINTLN)
  public static void error(String message) {
    System.err.println(message);
  }

  /**
   * Logs a debug message.
   *
   * @param message the debug message to log
   */
  @SuppressWarnings(PMD_SYSTEM_PRINTLN)
  public static void debug(String message) {
    System.out.println("[DEBUG] " + message);
  }

  /**
   * Logs a warning message.
   *
   * @param message the warning message to log
   */
  @SuppressWarnings(PMD_SYSTEM_PRINTLN)
  public static void warn(String message) {
    System.out.println("[WARN] " + message);
  }

  /**
   * Prints without a newline (for progress indicators).
   *
   * @param message the message to print
   */
  @SuppressWarnings(PMD_SYSTEM_PRINTLN)
  public static void print(String message) {
    System.out.print(message);
  }
}
