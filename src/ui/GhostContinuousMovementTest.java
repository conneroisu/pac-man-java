package ui;

import api.Actor;
import api.Location;
import api.Mode;
import api.PacmanGame;

/** Test to verify that ghosts move continuously and never stop once they leave the ghost house. */
public final class GhostContinuousMovementTest {

  /** Common maze layout line pattern. */
  private static final String MAZE_LAYOUT_LINE = "#.####.#####.##.#####.####.#";

  /** Expected number of ghosts. */
  private static final int EXPECTED_GHOST_COUNT = 4;

  /** Status report interval. */
  private static final int STATUS_REPORT_INTERVAL = 100;

  /** Private constructor to prevent instantiation. */
  private GhostContinuousMovementTest() {
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

  /** How many frames between movement checks */
  private static final int CHECK_INTERVAL = 10;

  /** How many frames to run the test for */
  private static final int TOTAL_FRAMES = 100000;

  /** Maximum allowed frames without movement before considering a ghost stuck */
  private static final int MAX_STATIONARY_FRAMES = 15;

  /**
   * Main method to run the test.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    if (Logger.isInfoEnabled()) {
      Logger.info("Starting Ghost Continuous Movement Test...");
    }

    // Create a game with a reasonable framerate
    PacmanGame game = new PacmanGame(TEST_MAZE, 20);
    Actor[] ghosts = game.getEnemies();

    // Check if we have the expected number of ghosts
    if (ghosts.length != EXPECTED_GHOST_COUNT) {
      if (Logger.isErrorEnabled()) {
        Logger.error("Expected 4 ghosts, got " + ghosts.length);
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

    // Initialize movement tracking
    Location[] previousLocations = new Location[ghosts.length];
    int[] stationaryFrames = new int[ghosts.length];
    boolean[] leftGhostHouse = new boolean[ghosts.length];
    boolean[] everStuck = new boolean[ghosts.length];

    // The ghost house area is approximately in this region
    final int GHOST_HOUSE_MIN_ROW = 11;
    final int GHOST_HOUSE_MAX_ROW = 14;
    final int GHOST_HOUSE_MIN_COL = 11;
    final int GHOST_HOUSE_MAX_COL = 16;

    // Store initial locations
    for (int g = 0; g < ghosts.length; g++) {
      previousLocations[g] = ghosts[g].getCurrentLocation();
      stationaryFrames[g] = 0;
      leftGhostHouse[g] = false;
      everStuck[g] = false;
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("\nRunning simulation for " + TOTAL_FRAMES + " frames...");
    }

    // Run the game for many frames and check for continuous movement
    for (int frame = 0; frame < TOTAL_FRAMES; frame++) {
      // Update game state
      game.updateAll();

      // Check each ghost's movement periodically
      if (frame % CHECK_INTERVAL == 0) {
        for (int g = 0; g < ghosts.length; g++) {
          Actor ghost = ghosts[g];
          Location currentLocation = ghost.getCurrentLocation();
          double rowExact = ghost.getRowExact();
          double colExact = ghost.getColExact();
          int row = (int) rowExact;
          int col = (int) colExact;

          // Check if ghost has left the ghost house
          boolean inGhostHouse =
              row >= GHOST_HOUSE_MIN_ROW
                  && row <= GHOST_HOUSE_MAX_ROW
                  && col >= GHOST_HOUSE_MIN_COL
                  && col <= GHOST_HOUSE_MAX_COL;

          if (!inGhostHouse) {
            leftGhostHouse[g] = true;
          }

          // If ghost hasn't moved since last check and has left the ghost house
          // Only check ghosts that are in ACTIVE modes (not INACTIVE)
          if (leftGhostHouse[g]
              && previousLocations[g] != null
              && currentLocation.equals(previousLocations[g])
              && ghost.getMode() != Mode.INACTIVE) {
            stationaryFrames[g] += CHECK_INTERVAL;

            // If ghost hasn't moved for too long, it's stuck
            if (stationaryFrames[g] >= MAX_STATIONARY_FRAMES) {
              everStuck[g] = true;
              if (Logger.isWarnEnabled()) {
                Logger.warn(
                    ghost.getClass().getSimpleName()
                        + " is stuck at "
                        + currentLocation
                        + " exact: ("
                        + rowExact
                        + ", "
                        + colExact
                        + ")"
                        + " with direction "
                        + ghost.getCurrentDirection()
                        + " mode: "
                        + ghost.getMode()
                        + " for "
                        + stationaryFrames[g]
                        + " frames");
              }
            }
          } else {
            // Ghost moved, reset stationary counter
            stationaryFrames[g] = 0;
          }

          // Update previous location for next check
          previousLocations[g] = currentLocation;
        }

        // Print progress report every 100 frames
        if (frame % STATUS_REPORT_INTERVAL == 0) {
          if (Logger.isInfoEnabled()) {
            Logger.info("\nFrame " + frame + " ghost positions:");
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
                      + " direction: "
                      + ghost.getCurrentDirection()
                      + " mode: "
                      + ghost.getMode()
                      + " left ghost house: "
                      + leftGhostHouse[g]
                      + " stationary frames: "
                      + stationaryFrames[g]);
            }
          }
        }
      }
    }

    // Final report
    if (Logger.isInfoEnabled()) {
      Logger.info("\nFinal ghost positions after " + TOTAL_FRAMES + " frames:");
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
                + " direction: "
                + ghost.getCurrentDirection()
                + " mode: "
                + ghost.getMode()
                + " left ghost house: "
                + leftGhostHouse[g]
                + " ever stuck: "
                + everStuck[g]);
      }
    }

    // Test result
    boolean allGhostsMoveContinuously = true;
    for (int g = 0; g < ghosts.length; g++) {
      if (leftGhostHouse[g] && everStuck[g]) {
        allGhostsMoveContinuously = false;
        break;
      }
    }

    if (allGhostsMoveContinuously) {
      if (Logger.isInfoEnabled()) {
        Logger.info("\nTEST PASSED: All ghosts moved continuously after leaving the ghost house");
      }
    } else {
      if (Logger.isErrorEnabled()) {
        Logger.error("\nTEST FAILED: Some ghosts got stuck after leaving the ghost house");
      }
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("\nTest complete.");
    }
  }
}
