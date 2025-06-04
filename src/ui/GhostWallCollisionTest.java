package ui;

import api.Actor;
import api.CellType;
import api.Location;
import api.Mode;
import api.PacmanGame;

/** Test to verify that ghosts never move through walls. */
public final class GhostWallCollisionTest {

  /** Common maze layout line pattern. */
  private static final String MAZE_LAYOUT_LINE = "#.####.#####.##.#####.####.#";

  /** Expected number of ghosts. */
  private static final int EXPECTED_GHOST_COUNT = 4;

  /** Status report interval. */
  private static final int STATUS_REPORT_INTERVAL = 500;

  /** Zero comparison value. */
  private static final int ZERO_COMPARISON = 0;

  /** Mode change frame 1. */
  private static final int MODE_CHANGE_FRAME_1 = 1000;

  /** Mode change frame 2. */
  private static final int MODE_CHANGE_FRAME_2 = 2000;

  /** Mode change frame 3. */
  private static final int MODE_CHANGE_FRAME_3 = 3000;

  /** Single movement distance. */
  private static final int SINGLE_MOVEMENT = 1;

  /** Tunnel boundary offset. */
  private static final int TUNNEL_BOUNDARY_OFFSET = 2;

  /** Single offset. */
  private static final int SINGLE_OFFSET = 1;

  /** Private constructor to prevent instantiation. */
  private GhostWallCollisionTest() {
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

  /** How many frames to run the test for */
  private static final int TOTAL_FRAMES = 5000;

  /**
   * Main method to run the test.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    if (Logger.isInfoEnabled()) {
      Logger.info("Starting Ghost Wall Collision Test...");
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
        Logger.info(ghost.getClass().getSimpleName() + " at " + ghost.getCurrentLocation());
      }
    }

    // Variables to track collision violations
    boolean[] hasViolatedWallConstraint = new boolean[ghosts.length];
    Location[] lastKnownLocation = new Location[ghosts.length];

    if (Logger.isInfoEnabled()) {
      Logger.info(
          "\nRunning simulation for " + TOTAL_FRAMES + " frames checking wall collisions...");
    }

    // Run the game for many frames and check for wall violations
    for (int frame = 0; frame < TOTAL_FRAMES; frame++) {
      // Before update, store current locations
      for (int g = 0; g < ghosts.length; g++) {
        lastKnownLocation[g] = ghosts[g].getCurrentLocation();
      }

      // Update game state
      game.updateAll();

      // After update, check if any ghost crossed a wall
      for (int g = 0; g < ghosts.length; g++) {
        Actor ghost = ghosts[g];
        Location currentLoc = ghost.getCurrentLocation();

        // If location changed, check if we crossed a wall
        if (lastKnownLocation[g] != null && !currentLoc.equals(lastKnownLocation[g]) && isWallBetween(game, lastKnownLocation[g], currentLoc)) {
          hasViolatedWallConstraint[g] = true;
          if (Logger.isInfoEnabled()) {
            Logger.info(
                "VIOLATION: "
                    + ghost.getClass().getSimpleName()
                    + " moved through a wall from "
                    + lastKnownLocation[g]
                    + " to "
                    + currentLoc
                    + " at frame "
                    + frame);
            Logger.info(
                "  Exact position: (" + ghost.getRowExact() + ", " + ghost.getColExact() + ")");
          }
        }
      }

      // Print progress report every 500 frames
      if (frame % STATUS_REPORT_INTERVAL == ZERO_COMPARISON && frame > ZERO_COMPARISON && Logger.isInfoEnabled()) {
        Logger.info("\nFrame " + frame + " wall collision status:");
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
                  + " direction: "
                  + ghost.getCurrentDirection()
                  + " mode: "
                  + ghost.getMode()
                  + " wall violations: "
                  + hasViolatedWallConstraint[g]);
        }
      }

      // Occasionally change modes to test different ghost behaviors
      if (frame == MODE_CHANGE_FRAME_1) {
        if (Logger.isInfoEnabled()) {
          Logger.info("\nSwitching to CHASE mode at frame 1000");
        }
        for (Actor ghost : ghosts) {
          ghost.setMode(Mode.CHASE, null);
        }
      } else if (frame == MODE_CHANGE_FRAME_2) {
        if (Logger.isInfoEnabled()) {
          Logger.info("\nSwitching to FRIGHTENED mode at frame 2000");
        }
        for (Actor ghost : ghosts) {
          ghost.setMode(Mode.FRIGHTENED, null);
        }
      } else if (frame == MODE_CHANGE_FRAME_3) {
        if (Logger.isInfoEnabled()) {
          Logger.info("\nSwitching back to SCATTER mode at frame 3000");
        }
        for (Actor ghost : ghosts) {
          ghost.setMode(Mode.SCATTER, null);
        }
      }
    }

    // Final report
    if (Logger.isInfoEnabled()) {
      Logger.info("\nFinal wall collision report after " + TOTAL_FRAMES + " frames:");
      for (int g = 0; g < ghosts.length; g++) {
        Actor ghost = ghosts[g];
        Logger.info(
            ghost.getClass().getSimpleName() + " wall violations: " + hasViolatedWallConstraint[g]);
      }
    }

    // Test result
    boolean allGhostsRespectWalls = true;
    for (boolean violated : hasViolatedWallConstraint) {
      if (violated) {
        allGhostsRespectWalls = false;
        break;
      }
    }

    if (allGhostsRespectWalls) {
      if (Logger.isInfoEnabled()) {
        Logger.info("\nTEST PASSED: All ghosts respected wall boundaries");
      }
    } else {
      if (Logger.isInfoEnabled()) {
        Logger.info("\nTEST FAILED: Some ghosts moved through walls");
      }
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("\nTest complete.");
    }
  }

  /**
   * Checks if a particular cell is a wall.
   *
   * @param game The Pacman game
   * @param row The row to check
   * @param col The column to check
   * @return True if the cell is a wall, false otherwise
   */
  private static boolean isWall(PacmanGame game, int row, int col) {
    try {
      return game.getCell(row, col).getType() == CellType.WALL;
    } catch (IndexOutOfBoundsException e) {
      // Out of bounds is considered a wall
      return true;
    }
  }

  /**
   * Determines if there is a wall between two adjacent locations.
   *
   * @param game The Pacman game
   * @param loc1 The first location
   * @param loc2 The second location
   * @return True if there is a wall between the locations
   */
  private static boolean isWallBetween(PacmanGame game, Location loc1, Location loc2) {
    // Check if locations are adjacent (orthogonally or diagonally)
    int rowDiff = Math.abs(loc1.row() - loc2.row());
    int colDiff = Math.abs(loc1.col() - loc2.col());

    // Special handling for tunnel wraparound
    if (rowDiff > SINGLE_MOVEMENT || colDiff > SINGLE_MOVEMENT) {
      // For tunnel wraparound (horizontal), don't consider it a wall violation
      if (loc1.row() == loc2.row()
          && ((loc1.col() == ZERO_COMPARISON && loc2.col() >= game.getNumColumns() - TUNNEL_BOUNDARY_OFFSET)
              || (loc2.col() == ZERO_COMPARISON && loc1.col() >= game.getNumColumns() - TUNNEL_BOUNDARY_OFFSET)
              || (loc1.col() == game.getNumColumns() - SINGLE_OFFSET && loc2.col() <= SINGLE_OFFSET)
              || (loc2.col() == game.getNumColumns() - SINGLE_OFFSET && loc1.col() <= SINGLE_OFFSET))) {
        // This is a valid tunnel transition
        return false;
      }

      // Starting positions (already in the tunnel)
      // Initial positions - don't count as violations because ghosts start in a special area
      // Any other large movement is a violation
      return !(isGhostHousePosition(loc1) || isGhostHousePosition(loc2));
    }

    // For orthogonal movement (not diagonal)
    if (rowDiff + colDiff == SINGLE_MOVEMENT && isWall(game, loc2.row(), loc2.col())) {
      // Check if the destination is a wall
      return true;
    }
    // For diagonal movement (some games allow this, but most Pac-Man implementations don't)
    else if (rowDiff == SINGLE_MOVEMENT && colDiff == SINGLE_MOVEMENT && (isWall(game, loc1.row(), loc2.col()) || isWall(game, loc2.row(), loc1.col()))) {
      // Diagonal movement requires checking both orthogonal paths
      return true;
    }

    return false;
  }

  /**
   * Checks if a location is in the ghost house. This is used to avoid counting initial ghost
   * movements out of their starting area as violations.
   *
   * @param loc The location to check
   * @return True if the location is in the ghost house
   */
  private static boolean isGhostHousePosition(Location loc) {
    // The ghost house is approximately this region:
    final int GHOST_HOUSE_MIN_ROW = 11;
    final int GHOST_HOUSE_MAX_ROW = 15;
    final int GHOST_HOUSE_MIN_COL = 10;
    final int GHOST_HOUSE_MAX_COL = 17;

    return loc.row() >= GHOST_HOUSE_MIN_ROW
        && loc.row() <= GHOST_HOUSE_MAX_ROW
        && loc.col() >= GHOST_HOUSE_MIN_COL
        && loc.col() <= GHOST_HOUSE_MAX_COL;
  }
}
