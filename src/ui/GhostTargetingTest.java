package ui;

import api.Actor;
import api.Direction;
import api.Location;
import api.Mode;
import api.PacmanGame;

/** Test to verify that each ghost's targeting logic is working correctly. */
public final class GhostTargetingTest {

  /** Common maze layout line pattern. */
  private static final String MAZE_LAYOUT_LINE = "#.####.#####.##.#####.####.#";

  /** Expected number of ghosts. */
  private static final int EXPECTED_GHOST_COUNT = 4;

  /** Distance improvement threshold. */
  private static final double DISTANCE_IMPROVEMENT_THRESHOLD = 0.1;

  /** Private constructor to prevent instantiation. */
  private GhostTargetingTest() {
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
      Logger.info("Starting Ghost Targeting Test...");
    }

    // Create a game with a reasonable framerate
    PacmanGame game = new PacmanGame(TEST_MAZE, 20);
    Actor[] ghosts = game.getEnemies();
    Actor pacman = game.getPlayer();

    // Check if we have the expected number of ghosts
    if (ghosts.length != EXPECTED_GHOST_COUNT) {
      if (Logger.isErrorEnabled()) {
        Logger.error("ERROR: Expected " + EXPECTED_GHOST_COUNT + " ghosts, got " + ghosts.length);
      }
      return;
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("Found " + ghosts.length + " ghosts");
    }

    // Identify each ghost by class name
    Actor blinky = null;
    Actor pinky = null;
    Actor inky = null;
    Actor clyde = null;

    for (Actor ghost : ghosts) {
      String className = ghost.getClass().getSimpleName();
      if (Logger.isInfoEnabled()) {
        Logger.info("Found ghost: " + className);
      }

      if ("Blinky".equals(className)) {
        blinky = ghost;
      } else if ("Pinky".equals(className)) {
        pinky = ghost;
      } else if ("Inky".equals(className)) {
        inky = ghost;
      } else if ("Clyde".equals(className)) {
        clyde = ghost;
      }
    }

    // Verify we found all ghost types
    if (blinky == null || pinky == null || inky == null || clyde == null) {
      Logger.error("ERROR: Could not identify all ghost types");
      return;
    }

    // Test Blinky's targeting (directly targets Pacman)
    if (Logger.isInfoEnabled()) {
      Logger.info("\n--- Testing Blinky (Red) targeting ---");
    }
    testBlinkyTargeting(blinky, pacman, game);

    // Test Pinky's targeting (targets 4 tiles ahead of Pacman)
    if (Logger.isInfoEnabled()) {
      Logger.info("\n--- Testing Pinky (Pink) targeting ---");
    }
    testPinkyTargeting(pinky, pacman, game);

    // Test Inky's targeting (uses Blinky's position and Pacman's position)
    if (Logger.isInfoEnabled()) {
      Logger.info("\n--- Testing Inky (Blue) targeting ---");
    }
    testInkyTargeting(inky, blinky, pacman, game);

    // Test Clyde's targeting (targets Pacman directly if far, runs away if close)
    if (Logger.isInfoEnabled()) {
      Logger.info("\n--- Testing Clyde (Orange) targeting ---");
    }
    testClydeTargeting(clyde, pacman, game);

    if (Logger.isInfoEnabled()) {
      Logger.info("\nGhost targeting test complete");
    }
  }

  /** Tests if Blinky correctly targets Pacman's position. */
  private static void testBlinkyTargeting(Actor blinky, Actor pacman, PacmanGame game) {
    // Position Pacman at a known location
    placePacmanAtLocation(pacman, 3, 3, Direction.RIGHT, game);

    // Set Blinky to chase mode
    blinky.setMode(Mode.CHASE, null);

    // Ensure Blinky has left the ghost house
    placeGhostAtLocation(blinky, 5, 5, Direction.UP, game);

    // Verify Blinky's target is Pacman's position
    verifyMovingToward(
        blinky, pacman.getCurrentLocation(), "Blinky", "Pacman's current position", game);

    // Move Pacman to a different location and verify Blinky follows
    placePacmanAtLocation(pacman, 20, 5, Direction.DOWN, game);
    verifyMovingToward(
        blinky, pacman.getCurrentLocation(), "Blinky", "Pacman's new position", game);
  }

  /** Tests if Pinky correctly targets 4 tiles ahead of Pacman. */
  private static void testPinkyTargeting(Actor pinky, Actor pacman, PacmanGame game) {
    // Position Pacman facing right
    placePacmanAtLocation(pacman, 10, 10, Direction.RIGHT, game);

    // Set Pinky to chase mode
    pinky.setMode(Mode.CHASE, null);

    // Ensure Pinky has left the ghost house
    placeGhostAtLocation(pinky, 5, 20, Direction.UP, game);

    // Verify Pinky's target is 4 tiles ahead of Pacman
    Location expectedTarget = new Location(10, 14); // 4 tiles to the right
    verifyMovingToward(pinky, expectedTarget, "Pinky", "4 tiles ahead (RIGHT)", game);

    // Test with Pacman facing UP (should include the "bug" where UP adds 4 tiles to the left)
    placePacmanAtLocation(pacman, 15, 15, Direction.UP, game);
    expectedTarget = new Location(11, 11); // 4 tiles up and 4 tiles left
    verifyMovingToward(
        pinky, expectedTarget, "Pinky", "4 tiles up AND 4 tiles left", game);
  }

  /** Tests if Inky correctly uses both Blinky and Pacman for targeting. */
  private static void testInkyTargeting(Actor inky, Actor blinky, Actor pacman, PacmanGame game) {
    // Position Pacman facing right
    placePacmanAtLocation(pacman, 10, 10, Direction.RIGHT, game);

    // Position Blinky at a known location
    placeGhostAtLocation(blinky, 20, 20, Direction.LEFT, game);

    // Set Inky to chase mode
    inky.setMode(Mode.CHASE, null);

    // Ensure Inky has left the ghost house
    placeGhostAtLocation(inky, 15, 5, Direction.DOWN, game);

    // Calculate expected target:
    // 1. Start 2 tiles ahead of Pacman: (10, 12)
    // 2. Draw vector from Blinky to this point
    // 3. Double the vector
    Location twoTilesAhead = new Location(10, 12);
    int vectorRow = twoTilesAhead.row() - blinky.getCurrentLocation().row(); // -10
    int vectorCol = twoTilesAhead.col() - blinky.getCurrentLocation().col(); // -8
    Location expectedTarget =
        new Location(
            twoTilesAhead.row() + vectorRow, // 10 + (-10) = 0
            twoTilesAhead.col() + vectorCol // 12 + (-8) = 4
            );

    verifyMovingToward(inky, expectedTarget, "Inky", "vector calculation result", game);
  }

  /** Tests if Clyde correctly targets Pacman when far and runs away when close. */
  private static void testClydeTargeting(Actor clyde, Actor pacman, PacmanGame game) {
    // First test: Clyde far from Pacman (>8 tiles) - should target Pacman directly
    placePacmanAtLocation(pacman, 5, 5, Direction.RIGHT, game);

    // Position Clyde far from Pacman (>8 tiles away)
    placeGhostAtLocation(clyde, 20, 20, Direction.UP, game);

    // Set Clyde to chase mode
    clyde.setMode(Mode.CHASE, null);

    // Verify Clyde targets Pacman directly when far
    verifyMovingToward(
        clyde, pacman.getCurrentLocation(), "Clyde", "Pacman's position (when far)", game);

    // Second test: Clyde close to Pacman (≤8 tiles) - should target scatter corner
    placeGhostAtLocation(clyde, 8, 8, Direction.LEFT, game);

    // Run for a few frames to let Clyde update his targeting
    for (int i = 0; i < 5; i++) {
      game.updateAll();
    }

    // Verify Clyde is NOT moving toward Pacman when close
    verifyNotMovingToward(
        clyde, pacman.getCurrentLocation(), "Clyde", "away from Pacman (when close)", game);
  }

  /** Helper to place Pacman at a specific location. */
  private static void placePacmanAtLocation(
      Actor pacman, int row, int col, Direction dir, PacmanGame game) {
    pacman.setRowExact(row + 0.5);
    pacman.setColExact(col + 0.5);
    pacman.setDirection(dir);

    // Run update to ensure the game state is consistent
    game.updateAll();

    if (Logger.isInfoEnabled()) {
      Logger.info("Positioned Pacman at (" + row + ", " + col + ") facing " + dir);
    }
  }

  /** Helper to place a ghost at a specific location. */
  private static void placeGhostAtLocation(
      Actor ghost, int row, int col, Direction dir, PacmanGame game) {
    ghost.setRowExact(row + 0.5);
    ghost.setColExact(col + 0.5);
    ghost.setDirection(dir);

    // Run update to ensure the game state is consistent
    game.updateAll();

    if (Logger.isInfoEnabled()) {
      Logger.info(
          "Positioned "
              + ghost.getClass().getSimpleName()
              + " at ("
              + row
              + ", "
              + col
              + ") facing "
              + dir);
    }
  }

  /** Verifies that a ghost is moving toward a target. */
  private static void verifyMovingToward(
      Actor ghost, Location target, String ghostName, String targetDesc, PacmanGame game) {
    Location initialLoc = ghost.getCurrentLocation();
    double initialDistance = calculateDistance(initialLoc, target);

    // Run several updates to let ghost move
    for (int i = 0; i < 10; i++) {
      game.updateAll();
    }

    Location finalLoc = ghost.getCurrentLocation();
    double finalDistance = calculateDistance(finalLoc, target);

    if (Logger.isInfoEnabled()) {
      Logger.info(ghostName + " started at " + initialLoc + ", now at " + finalLoc);
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(
          "Distance to target "
              + targetDesc
              + " "
              + target
              + ": initial="
              + initialDistance
              + ", final="
              + finalDistance);
    }

    // Check if ghost is moving toward target (distance decreasing)
    if (finalDistance < initialDistance && Logger.isInfoEnabled()) {
      Logger.info("✓ PASS: " + ghostName + " is correctly moving toward " + targetDesc);
    } else if (finalDistance >= initialDistance && Logger.isInfoEnabled()) {
      Logger.info("✗ FAIL: " + ghostName + " is NOT moving toward " + targetDesc);
    }
  }

  /** Verifies that a ghost is NOT moving toward a target. */
  private static void verifyNotMovingToward(
      Actor ghost, Location target, String ghostName, String targetDesc, PacmanGame game) {
    Location initialLoc = ghost.getCurrentLocation();
    double initialDistance = calculateDistance(initialLoc, target);

    // Run several updates to let ghost move
    for (int i = 0; i < 10; i++) {
      game.updateAll();
    }

    Location finalLoc = ghost.getCurrentLocation();
    double finalDistance = calculateDistance(finalLoc, target);

    if (Logger.isInfoEnabled()) {
      Logger.info(ghostName + " started at " + initialLoc + ", now at " + finalLoc);
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(
          "Distance to target "
              + targetDesc
              + " "
              + target
              + ": initial="
              + initialDistance
              + ", final="
              + finalDistance);
    }

    // Check if ghost is NOT moving toward target (distance not decreasing significantly)
    if ((finalDistance >= initialDistance || Math.abs(finalDistance - initialDistance) < DISTANCE_IMPROVEMENT_THRESHOLD) && Logger.isInfoEnabled()) {
      Logger.info("✓ PASS: " + ghostName + " is correctly NOT moving toward " + targetDesc);
    } else if (finalDistance < initialDistance && Math.abs(finalDistance - initialDistance) >= DISTANCE_IMPROVEMENT_THRESHOLD && Logger.isInfoEnabled()) {
      Logger.info(
          "✗ FAIL: " + ghostName + " IS moving toward " + targetDesc + " when it shouldn't be");
    }
  }

  /** Calculates Euclidean distance between two locations. */
  private static double calculateDistance(Location loc1, Location loc2) {
    double rowDiff = loc1.row() - loc2.row();
    double colDiff = loc1.col() - loc2.col();
    return Math.sqrt(rowDiff * rowDiff + colDiff * colDiff);
  }
}
