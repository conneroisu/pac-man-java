package ui;

import api.Mode;

import api.Actor;
import api.Descriptor;
import api.Direction;
import api.Location;
import api.PacmanGame;

/**
 * Tests maze navigation, wall detection, and ghost behavior over multiple frames.
 */
public final class MazeNavigationTest {

  /**
   * Private constructor to prevent instantiation of utility class.
   */
  private MazeNavigationTest() {
    // Utility class should not be instantiated
  }

  /** Test maze with walls, Blinky, and pacman. */
  public static final String[] NAVIGATION_TEST = {
    "#########",
    "#B......#",
    "#.#####.#",
    "#.#...#.#",
    "#.#.S.#.#",
    "#.#...#.#",
    "#.#####.#",
    "#.......#",
    "#########"
  };

  /**
   * Main method to run the test.
   * 
   * @param args Command line arguments (not used)
   */
  public static void main(final String[] args) {
    // Create game with test maze (frameRate = 10)
    final int frameRate = 10;
    PacmanGame game = new PacmanGame(NAVIGATION_TEST, frameRate);

    System.out.println("=== MAZE NAVIGATION TEST ===\n");

    // Get Blinky
    Actor blinky = game.getEnemies()[0];

    // Create descriptor
    Descriptor descriptor = makeDescriptor(game);

    // Test CHASE mode navigation
    System.out.println("=== CHASE MODE NAVIGATION TEST ===");
    final int chaseSteps = 20;
    testChaseNavigation(blinky, descriptor, chaseSteps);

    // Reset Blinky
    blinky.reset();

    // Test SCATTER mode navigation
    System.out.println("\n=== SCATTER MODE NAVIGATION TEST ===");
    final int scatterSteps = 20;
    testScatterNavigation(blinky, descriptor, scatterSteps);

    // Reset Blinky
    blinky.reset();

    // Test FRIGHTENED mode navigation
    System.out.println("\n=== FRIGHTENED MODE NAVIGATION TEST ===");
    final int frightenedSteps = 20;
    testFrightenedNavigation(blinky, descriptor, frightenedSteps);

    System.out.println("\n=== TEST COMPLETE ===");
  }

  /**
   * Tests CHASE mode navigation.
   *
   * @param ghost The ghost actor to test
   * @param desc The game descriptor
   * @param steps Number of update steps to simulate
   */
  private static void testChaseNavigation(final Actor ghost, final Descriptor desc, final int steps) {
    // Set ghost to CHASE mode
    ghost.setMode(Mode.CHASE, desc);

    System.out.println("Pacman location: " + desc.getPlayerLocation());
    System.out.println(
        "Initial ghost position: "
            + ghost.getCurrentLocation()
            + ", direction: "
            + ghost.getCurrentDirection());

    // Run simulation for several steps
    for (int i = 0; i < steps; i++) {
      ghost.update(desc);
      System.out.println(
          "Step "
              + (i + 1)
              + ": "
              + ghost.getCurrentLocation()
              + ", exact: ("
              + ghost.getRowExact()
              + ", "
              + ghost.getColExact()
              + "), "
              + "direction: "
              + ghost.getCurrentDirection());
    }
  }

  /**
   * Tests SCATTER mode navigation.
   *
   * @param ghost The ghost actor to test
   * @param desc The game descriptor
   * @param steps Number of update steps to simulate
   */
  private static void testScatterNavigation(final Actor ghost, final Descriptor desc, final int steps) {
    // Set ghost to SCATTER mode
    ghost.setMode(Mode.SCATTER, desc);

    System.out.println(
        "Initial ghost position: "
            + ghost.getCurrentLocation()
            + ", direction: "
            + ghost.getCurrentDirection());

    // Run simulation for several steps
    for (int i = 0; i < steps; i++) {
      ghost.update(desc);
      System.out.println(
          "Step "
              + (i + 1)
              + ": "
              + ghost.getCurrentLocation()
              + ", exact: ("
              + ghost.getRowExact()
              + ", "
              + ghost.getColExact()
              + "), "
              + "direction: "
              + ghost.getCurrentDirection());
    }
  }

  /**
   * Tests FRIGHTENED mode navigation.
   *
   * @param ghost The ghost actor to test
   * @param desc The game descriptor
   * @param steps Number of update steps to simulate
   */
  private static void testFrightenedNavigation(final Actor ghost, final Descriptor desc, final int steps) {
    // Set ghost to FRIGHTENED mode
    ghost.setMode(Mode.FRIGHTENED, desc);

    System.out.println(
        "Initial ghost position: "
            + ghost.getCurrentLocation()
            + ", direction: "
            + ghost.getCurrentDirection());

    // Run simulation for several steps
    for (int i = 0; i < steps; i++) {
      ghost.update(desc);
      System.out.println(
          "Step "
              + (i + 1)
              + ": "
              + ghost.getCurrentLocation()
              + ", exact: ("
              + ghost.getRowExact()
              + ", "
              + ghost.getColExact()
              + "), "
              + "direction: "
              + ghost.getCurrentDirection());
    }
  }

  /**
   * Creates a game descriptor for use in testing.
   *
   * @param game The game to create a descriptor for
   * @return A new descriptor with player and enemy information
   */
  private static Descriptor makeDescriptor(final PacmanGame game) {
    Location enemyLoc = game.getEnemies()[0].getCurrentLocation();
    Location playerLoc = game.getPlayer().getCurrentLocation();
    Direction playerDir = game.getPlayer().getCurrentDirection();
    return new Descriptor(playerLoc, playerDir, enemyLoc);
  }
}
