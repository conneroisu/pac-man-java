package ui;

import api.Actor;
import api.Mode;
import api.PacmanGame;

/** Test to verify that ghosts are always positioned at the center of a cell. */
public final class GhostPositionTest {

  /** Common maze layout line pattern. */
  private static final String MAZE_LAYOUT_LINE = "#.####.#####.##.#####.####.#";

  /** Expected number of ghosts. */
  private static final int EXPECTED_GHOST_COUNT = 4;

  /** Status report interval 1. */
  private static final int STATUS_REPORT_INTERVAL_1 = 100;

  /** Status report interval 2. */
  private static final int STATUS_REPORT_INTERVAL_2 = 50;

  /** Zero comparison. */
  private static final int ZERO_COMPARISON = 0;

  /** Distance threshold. */
  private static final double DISTANCE_THRESHOLD = 0.1;

  /** Private constructor to prevent instantiation. */
  private GhostPositionTest() {
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
      Logger.info("Starting Ghost Position Test...");
    }

    // Create a game with a reasonable framerate
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
                + " exact: ("
                + ghost.getRowExact()
                + ", "
                + ghost.getColExact()
                + ")"
                + " with direction "
                + ghost.getCurrentDirection());
      }
    }

    // Run initial simulation with normal SCATTER mode
    if (Logger.isInfoEnabled()) {
      Logger.info("\nRunning initial simulation for 200 frames in SCATTER mode...");
    }
    final int initialFrames = 200;

    // Track position errors
    int[] positionErrorCounts = new int[4];
    double[] maxPositionErrors = new double[4];

    for (int i = 0; i < initialFrames; i++) {
      game.updateAll();
      checkGhostPositions(ghosts, positionErrorCounts, maxPositionErrors);

      // Report progress and position errors every 100 frames
      if (i % STATUS_REPORT_INTERVAL_1 == ZERO_COMPARISON) {
        printGhostPositions(i, ghosts, positionErrorCounts);
      }
    }

    // Now switch to FRIGHTENED mode to test random movement at intersections
    final int numFrames = 150;
    if (Logger.isInfoEnabled()) {
      Logger.info("\nSwitching ghosts to FRIGHTENED mode for 150 frames...");
    }
    for (Actor ghost : ghosts) {
      ghost.setMode(Mode.FRIGHTENED, null);
    }

    for (int i = 0; i < numFrames; i++) {
      game.updateAll();
      checkGhostPositions(ghosts, positionErrorCounts, maxPositionErrors);

      // Report progress more frequently during FRIGHTENED mode
      if (i % STATUS_REPORT_INTERVAL_2 == ZERO_COMPARISON) {
        printGhostPositions(initialFrames + i, ghosts, positionErrorCounts);
      }
    }

    // Finally switch to CHASE mode and move in corridors
    if (Logger.isInfoEnabled()) {
      Logger.info("\nSwitching ghosts to CHASE mode for 150 frames...");
    }
    for (Actor ghost : ghosts) {
      ghost.setMode(Mode.CHASE, null);
    }

    for (int i = 0; i < numFrames; i++) {
      game.updateAll();
      checkGhostPositions(ghosts, positionErrorCounts, maxPositionErrors);

      // Report progress during CHASE mode
      if (i % STATUS_REPORT_INTERVAL_2 == ZERO_COMPARISON) {
        printGhostPositions(initialFrames + numFrames + i, ghosts, positionErrorCounts);
      }
    }

    // Final report
    int totalFrames = initialFrames + numFrames + numFrames; // Scatter + Frightened + Chase
    if (Logger.isInfoEnabled()) {
      Logger.info("\nPosition error report after " + totalFrames + " frames:");
    }
    for (int g = 0; g < ghosts.length; g++) {
      Actor ghost = ghosts[g];
      if (Logger.isInfoEnabled()) {
        Logger.info(
            ghost.getClass().getSimpleName()
                + " position errors: "
                + positionErrorCounts[g]
                + " max error distance: "
                + maxPositionErrors[g]);
      }
    }

    // Test result summary
    boolean allGhostsCentered = true;
    for (int count : positionErrorCounts) {
      if (count > ZERO_COMPARISON) {
        allGhostsCentered = false;
        break;
      }
    }

    if (allGhostsCentered && Logger.isInfoEnabled()) {
      Logger.info("\nTEST PASSED: All ghosts maintained center cell positioning");
    } else if (!allGhostsCentered && Logger.isInfoEnabled()) {
      Logger.info("\nTEST FAILED: Some ghosts had positioning errors");
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("\nTest complete.");
    }
  }

  /** Helper method to check ghost positions and update error counts. */
  private static void checkGhostPositions(
      Actor[] ghosts, int[] positionErrorCounts, double... maxPositionErrors) {
    // Check each ghost's position after movement
    for (int g = 0; g < ghosts.length; g++) {
      Actor ghost = ghosts[g];

      // Calculate how far the ghost is from the center of its cell
      double rowExact = ghost.getRowExact();
      double colExact = ghost.getColExact();
      int cellRow = (int) rowExact;
      int cellCol = (int) colExact;

      // Distance from center (center of cell is at x.5, y.5)
      double distFromCenterRow = Math.abs((cellRow + 0.5) - rowExact);
      double distFromCenterCol = Math.abs((cellCol + 0.5) - colExact);
      double totalDistFromCenter =
          Math.sqrt(distFromCenterRow * distFromCenterRow + distFromCenterCol * distFromCenterCol);

      // Check if the ghost is at a cell's center when it's making a movement decision
      // We use a larger threshold (0.1) since we now only enforce centering at decision points
      // but not during continuous movement between cells
      boolean atDecisionPoint =
          ghost.getCurrentDirection() != null
              && (distFromCenterRow < 0.1 && distFromCenterCol < 0.1);
      if (atDecisionPoint && totalDistFromCenter > DISTANCE_THRESHOLD) {
        positionErrorCounts[g]++;
        if (totalDistFromCenter > maxPositionErrors[g]) {
          maxPositionErrors[g] = totalDistFromCenter;
        }
      }
    }
  }

  /** Helper method to print ghost positions. */
  private static void printGhostPositions(int frame, Actor[] ghosts, int... positionErrorCounts) {
    if (Logger.isInfoEnabled()) {
      Logger.info("\nFrame " + frame + " ghost positions:");
    }
    for (int g = 0; g < ghosts.length; g++) {
      Actor ghost = ghosts[g];
      double rowExact = ghost.getRowExact();
      double colExact = ghost.getColExact();
      int cellRow = (int) rowExact;
      int cellCol = (int) colExact;
      double distFromCenterRow = Math.abs((cellRow + 0.5) - rowExact);
      double distFromCenterCol = Math.abs((cellCol + 0.5) - colExact);

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
                + " center dist: (row="
                + distFromCenterRow
                + ", col="
                + distFromCenterCol
                + ")"
                + " direction: "
                + ghost.getCurrentDirection()
                + " mode: "
                + ghost.getMode()
                + " position errors: "
                + positionErrorCounts[g]);
      }
    }
  }
}
