package ui;

import api.Direction;
import ui.SimulationTestFramework.SimulationConfig;
import java.util.Locale;

/**
 * Comprehensive test suite for detecting ghost movement bugs. This class contains various maze
 * scenarios designed to expose common ghost movement issues like getting stuck or intersecting
 * walls.
 */
public final class GhostMovementBugTests {

  /** Common maze border line. */
  private static final String MAZE_BORDER = "###############";
  /** Common open area line. */
  private static final String OPEN_AREA = "#.............#";
  /** Common narrow passage line. */
  private static final String NARROW_PASSAGE = "#.#.........#.#";
  /** Common player spawn line. */
  private static final String PLAYER_SPAWN = "#......S......#";
  /** Separator line for output formatting. */
  private static final String SEPARATOR_LINE = "=";

  /** Private constructor to prevent instantiation of utility class. */
  private GhostMovementBugTests() {
    // Utility class should not be instantiated
  }

  /** Simple corridor test - likely to expose stuck behavior. */
  public static final String[] SIMPLE_CORRIDOR = {
    "###########",
    "#.........#",
    "#.........#",
    "#...B.....#",
    "#.........#",
    "#....S....#",
    "###########"
  };

  /** Complex intersection test - tests direction choosing logic. */
  public static final String[] COMPLEX_INTERSECTION = {
    MAZE_BORDER,
    OPEN_AREA,
    "#.###.....###.#",
    NARROW_PASSAGE,
    "#.#....B....#.#",
    NARROW_PASSAGE,
    "#.###.......###",
    OPEN_AREA,
    PLAYER_SPAWN,
    OPEN_AREA,
    MAZE_BORDER
  };

  /** Ghost house test - tests escape behavior. */
  public static final String[] GHOST_HOUSE_TEST = {
    MAZE_BORDER,
    OPEN_AREA,
    OPEN_AREA,
    "#......###....#",
    "#......#B#....#",
    "#......#P#....#",
    "#......###....#",
    OPEN_AREA,
    PLAYER_SPAWN,
    MAZE_BORDER
  };

  /** Narrow passages test - exposes wall collision issues. */
  public static final String[] NARROW_PASSAGES = {
    MAZE_BORDER,
    "#.#.#.#.#.#.#.#",
    OPEN_AREA,
    "#.#.#.#B#.#.#.#",
    OPEN_AREA,
    "#.#.#.#.#.#.#.#",
    OPEN_AREA,
    "#.#.#.#S#.#.#.#",
    MAZE_BORDER
  };

  /** Multiple ghosts test - tests interaction and collision. */
  public static final String[] MULTIPLE_GHOSTS = {
    MAZE_BORDER,
    OPEN_AREA,
    OPEN_AREA,
    "#.B.........P.#",
    OPEN_AREA,
    OPEN_AREA,
    "#.I.........C.#",
    OPEN_AREA,
    PLAYER_SPAWN,
    MAZE_BORDER
  };

  /** Tunnel test - tests wraparound behavior. */
  public static final String[] TUNNEL_TEST = {
    MAZE_BORDER,
    "..............",
    MAZE_BORDER,
    OPEN_AREA,
    "#.....B.......#",
    OPEN_AREA,
    "#.....S.......#",
    MAZE_BORDER
  };

  /** Dead end test - tests what happens when ghosts reach dead ends. */
  public static final String[] DEAD_END_TEST = {
    MAZE_BORDER,
    OPEN_AREA,
    "#.###########.#",
    NARROW_PASSAGE,
    "#.#....B....#.#",
    NARROW_PASSAGE,
    "#.###########.#",
    OPEN_AREA,
    PLAYER_SPAWN,
    MAZE_BORDER
  };

  /** Stress test maze - complex layout to stress test all behaviors. */
  public static final String[] STRESS_TEST_MAZE = {
    "###################",
    "#.................#",
    "#.###.#######.###.#",
    "#.#.............#.#",
    "#.#.###.B.I.###.#.#",
    "#...#.......#.....#",
    "###.#.#####.#.#####",
    "#...#.......#.....#",
    "#.#.###.P.C.###.#.#",
    "#.#.............#.#",
    "#.###.#######.###.#",
    "#.................#",
    "#........S........#",
    "###################"
  };

  /** Run a single test scenario. */
  public static void runSingleTest(String testName, String[] maze, int frames) {
    if (Logger.isInfoEnabled()) {
      Logger.info("\n" + SEPARATOR_LINE.repeat(50));
      Logger.info("RUNNING TEST: " + testName);
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(SEPARATOR_LINE.repeat(50));
    }

    SimulationConfig config = new SimulationConfig(frames, 42L, false);
    SimulationTestFramework.runComprehensiveTest(maze, config);
  }

  /** Run all ghost movement bug detection tests. */
  public static void runAllTests() {
    Logger.info("GHOST MOVEMENT BUG DETECTION SUITE");
    Logger.info("This test suite runs deterministic simulations to detect:");
    Logger.info("- Ghosts getting stuck and not moving");
    Logger.info("- Ghosts intersecting with walls");
    Logger.info("- Inconsistent movement speeds");
    Logger.info("- Dead ghosts not returning home");
    Logger.info("- Infinite loops in small areas");
    Logger.info("- Out-of-bounds movement");

    // Test scenarios with different frame counts based on complexity
    runSingleTest("Simple Corridor", SIMPLE_CORRIDOR, 300);
    runSingleTest("Complex Intersection", COMPLEX_INTERSECTION, 500);
    runSingleTest("Ghost House Escape", GHOST_HOUSE_TEST, 400);
    runSingleTest("Narrow Passages", NARROW_PASSAGES, 600);
    runSingleTest("Multiple Ghosts", MULTIPLE_GHOSTS, 800);
    runSingleTest("Tunnel Wraparound", TUNNEL_TEST, 350);
    runSingleTest("Dead End Navigation", DEAD_END_TEST, 450);
    runSingleTest("Stress Test", STRESS_TEST_MAZE, 1000);

    if (Logger.isInfoEnabled()) {
      Logger.info("\n" + SEPARATOR_LINE.repeat(70));
      Logger.info("ALL TESTS COMPLETED");
      Logger.info(SEPARATOR_LINE.repeat(70));
    }
  }

  /** Run a targeted test to reproduce specific bugs. */
  public static void runBugReproductionTest() {
    Logger.info("BUG REPRODUCTION TEST");
    Logger.info("Running extended simulations to catch intermittent bugs...");

    // Run the same test multiple times with different seeds to catch intermittent issues
    String[][] testMazes = {SIMPLE_CORRIDOR, COMPLEX_INTERSECTION, GHOST_HOUSE_TEST};
    String[] testNames = {"Simple Corridor", "Complex Intersection", "Ghost House"};
    long[] seeds = {42L, 123L, 456L, 789L, 999L};

    for (int mazeIdx = 0; mazeIdx < testMazes.length; mazeIdx++) {
      for (long seed : seeds) {
        if (Logger.isInfoEnabled()) {
          Logger.info("\nTesting " + testNames[mazeIdx] + " with seed " + seed);
        }
        SimulationConfig config = new SimulationConfig(200, seed, false);
        SimulationTestFramework.runComprehensiveTest(testMazes[mazeIdx], config);
      }
    }
  }

  /** Test with specific player movements to trigger edge cases. */
  public static void runPlayerInteractionTest() {
    Logger.info("PLAYER INTERACTION TEST");
    Logger.info("Testing ghost behavior with specific player movements...");

    // Create a sequence of player movements
    Direction[] playerMoves = new Direction[100];
    for (int i = 0; i < playerMoves.length; i++) {
      // Alternate between moving right and up to create interesting scenarios
      playerMoves[i] = (i % 4 < 2) ? Direction.RIGHT : Direction.UP;
    }

    SimulationConfig config = new SimulationConfig(100, 42L, true, playerMoves);
    SimulationTestFramework.runComprehensiveTest(COMPLEX_INTERSECTION, config);
  }

  /** Main method to run the ghost movement bug detection tests. */
  public static void main(String[] args) {
    Logger.info("Starting Ghost Movement Bug Detection Tests...\n");

    if (args.length > 0) {
      switch (args[0].toLowerCase(Locale.ENGLISH)) {
        case "all":
          runAllTests();
          break;
        case "bug":
          runBugReproductionTest();
          break;
        case "player":
          runPlayerInteractionTest();
          break;
        case "simple":
          runSingleTest("Simple Test", SIMPLE_CORRIDOR, 200);
          break;
        default:
          Logger.info("Usage: java ui.GhostMovementBugTests [all|bug|player|simple]");
          return;
      }
    } else {
      // Default: run all tests
      runAllTests();
    }

    Logger.info("\nTest run completed. Check output above for any detected bugs.");
  }
}
