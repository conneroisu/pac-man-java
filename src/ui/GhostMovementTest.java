package ui;

import api.Actor;
import api.Location;
import api.Mode;
import api.PacmanGame;

/** Simple test to check ghost movement and log generation. */
public final class GhostMovementTest {

  /** Common maze layout line pattern. */
  private static final String MAZE_LAYOUT_LINE = "#.####.#####.##.#####.####.#";

  /** Expected number of ghosts. */
  private static final int EXPECTED_GHOST_COUNT = 4;

  /** Status report interval. */
  private static final int STATUS_REPORT_INTERVAL = 50;

  /** Minimum frames before stuck check. */
  private static final int MINIMUM_FRAMES_BEFORE_CHECK = 2;

  /** Private constructor to prevent instantiation. */
  private GhostMovementTest() {
    // Utility class
  }

  /** Test maze with the same structure as the main game. */
  public static final String[] TEST_MAZE = {
    "############################",
    "#............##............#",
    MAZE_LAYOUT_LINE,
    "#*####.#####.##.#####.####*#",
    MAZE_LAYOUT_LINE,
    "#..........................#",
    "#.####.##.########.##.####.#",
    "#.####.##.########.##.####.#",
    "#......##....##....##......#",
    "######.##### ## #####.######",
    "     #.##### ## #####.#     ",
    "     #.##          ##.#     ",
    "     #.## ##BPIC## ##.#     ",
    "######.## ######## ##.######",
    "      .   ##    ##   .      ",
    "######.## ######## ##.######",
    "     #.## ######## ##.#     ",
    "     #.##          ##.#     ",
    "     #.## ######## ##.#     ",
    "######.## ######## ##.######",
    "#............##............#",
    MAZE_LAYOUT_LINE,
    MAZE_LAYOUT_LINE,
    "#*..##................##..*#",
    "###.##.##.########.##.##.###",
    "###.##.##.########.##.##.###",
    "#......##...S##....##......#",
    "#.##########.##.##########.#",
    "#.##########.##.##########.#",
    "#..........................#",
    "############################",
  };

  /**
   * Main method to run the test.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    if (Logger.isInfoEnabled()) {
      Logger.info("Starting Ghost Movement Test with Logging...");
    }

    // Create a game with a reasonable frame rate
    PacmanGame game = new PacmanGame(TEST_MAZE, 20);
    Actor[] ghosts = game.getEnemies();

    // Check if we have the expected number of ghosts
    if (ghosts.length != EXPECTED_GHOST_COUNT) {
      if (Logger.isErrorEnabled()) {
        Logger.error("ERROR: Expected 4 ghosts, got " + ghosts.length);
      }
      return;
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("Found " + ghosts.length + " ghosts");
    }

    // Set all ghosts to SCATTER mode to ensure they're active
    if (Logger.isInfoEnabled()) {
      Logger.info("Setting all ghosts to SCATTER mode");
    }
    for (Actor ghost : ghosts) {
      ghost.setMode(Mode.SCATTER, null);
      if (Logger.isInfoEnabled()) {
        Logger.info(
            ghost.getClass().getSimpleName()
                + " at "
                + ghost.getCurrentLocation()
                + " with direction "
                + ghost.getCurrentDirection());
      }
    }

    // Run for a number of frames to let ghosts move
    if (Logger.isInfoEnabled()) {
      Logger.info("\nRunning simulation for 300 frames...");
    }
    final int framesToRun = 300;

    // Track movement for oscillation detection
    Location[] previousLocations = new Location[4];
    int[] oscillationCounts = new int[4];

    for (int i = 0; i < framesToRun; i++) {
      game.updateAll();

      // Check for oscillation (returning to same position)
      for (int g = 0; g < ghosts.length; g++) {
        Actor ghost = ghosts[g];
        Location currentLoc = ghost.getCurrentLocation();

        if (previousLocations[g] != null && i > MINIMUM_FRAMES_BEFORE_CHECK && currentLoc.equals(previousLocations[g])) {
          // If we've returned to a previous position after moving away,
          // that might indicate oscillation
          oscillationCounts[g]++;
        }

        previousLocations[g] = currentLoc;
      }

      // Report ghost positions every 50 frames
      if (i % STATUS_REPORT_INTERVAL == 0 && Logger.isInfoEnabled()) {
        Logger.info("\nFrame " + i + " ghost positions:");
        for (int g = 0; g < ghosts.length; g++) {
          Actor ghost = ghosts[g];
          Logger.info(
              ghost.getClass().getSimpleName()
                  + " at "
                  + ghost.getCurrentLocation()
                  + " exact: ("
                  + ghost.getRowExact()
                  + ", "
                  + ghost.getColExact()
                  + ")"
                  + " with direction "
                  + ghost.getCurrentDirection()
                  + " mode: "
                  + ghost.getMode()
                  + " oscillation count: "
                  + oscillationCounts[g]);
        }
      }
    }

    // Final report
    if (Logger.isInfoEnabled()) {
      Logger.info("\nFinal ghost positions after " + framesToRun + " frames:");
    }
    for (int g = 0; g < ghosts.length; g++) {
      Actor ghost = ghosts[g];
      if (Logger.isInfoEnabled()) {
        Logger.info(
            ghost.getClass().getSimpleName()
                + " at "
                + ghost.getCurrentLocation()
                + " exact: ("
                + ghost.getRowExact()
                + ", "
                + ghost.getColExact()
                + ")"
                + " with direction "
                + ghost.getCurrentDirection()
                + " mode: "
                + ghost.getMode()
                + " oscillation count: "
                + oscillationCounts[g]);
      }
    }

    // Check for log file
    java.io.File logFile = new java.io.File("ghost_movement.log");
    if (logFile.exists()) {
      if (Logger.isInfoEnabled()) {
        Logger.info("\nLog file created at: " + logFile.getAbsolutePath());
      }}
  }
}
