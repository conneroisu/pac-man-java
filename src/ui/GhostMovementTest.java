package ui;

import api.Mode;
import static org.junit.Assert.*;

import api.Actor;
import api.Descriptor;
import api.Direction;
import api.Location;
import api.PacmanGame;
import com.pacman.ghost.Blinky;
import com.pacman.ghost.Clyde;
import com.pacman.ghost.Inky;
import com.pacman.ghost.Pinky;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for ghost movement and behavior. Specifically designed to test for oscillation/jitter
 * issues.
 */
public class GhostMovementTest {

  // Simple test maze with Pacman and all ghosts
  public static final String[] TEST_MAZE = {
    "#####################",
    "#...................#",
    "#.###.###.###.###.#.#",
    "#.#...............#.#",
    "#.#.###########.#.#.#",
    "#.#.#.........#.#.#.#",
    "#.#.#.##...##.#.#.#.#",
    "#.....#BICPS#.......#",
    "#.#.#.#######.#.#.#.#",
    "#.#.#.........#.#.#.#",
    "#.#.###########.#.#.#",
    "#.#...............#.#",
    "#.###.###.###.###.#.#",
    "#...................#",
    "#####################"
  };

  private PacmanGame game;
  private Blinky blinky;
  private Pinky pinky;
  private Inky inky;
  private Clyde clyde;

  @Before
  public void setUp() {
    game = new PacmanGame(TEST_MAZE, 10);
    Actor[] enemies = game.getEnemies();

    // Find our ghosts
    for (Actor ghost : enemies) {
      if (ghost instanceof Blinky) blinky = (Blinky) ghost;
      else if (ghost instanceof Pinky) pinky = (Pinky) ghost;
      else if (ghost instanceof Inky) inky = (Inky) ghost;
      else if (ghost instanceof Clyde) clyde = (Clyde) ghost;
    }

    // Make sure we have all ghosts
    assertNotNull("Blinky should be implemented", blinky);
    assertNotNull("Pinky should be implemented", pinky);
    assertNotNull("Inky should be implemented", inky);
    assertNotNull("Clyde should be implemented", clyde);
  }

  /** Creates a game descriptor for testing. */
  private Descriptor makeDescriptor() {
    Location blinkyLoc = blinky.getCurrentLocation();
    Location playerLoc = game.getPlayer().getCurrentLocation();
    Direction playerDir = game.getPlayer().getCurrentDirection();
    return new Descriptor(playerLoc, playerDir, blinkyLoc);
  }

  /**
   * Test that ghosts don't oscillate (move up and down repeatedly) when they should be making
   * consistent progress.
   */
  @Test
  public void testNoOscillation() {
    // Put ghosts in SCATTER mode
    Descriptor desc = makeDescriptor();
    blinky.setMode(Mode.SCATTER, desc);

    // Record starting position
    Location startLocation = blinky.getCurrentLocation();

    // Update several times
    for (int i = 0; i < 30; i++) {
      blinky.update(desc);
    }

    // Check final position
    Location endLocation = blinky.getCurrentLocation();

    // Ghost should have moved from its starting position
    assertFalse("Ghost should make progress and not get stuck", startLocation.equals(endLocation));

    // Test for oscillation by checking if position changes consistently
    Location prevLocation = null;
    Direction prevDirection = null;
    int directionChanges = 0;
    int positionChanges = 0;

    // Reset ghost to starting position
    blinky.reset();
    blinky.setMode(Mode.SCATTER, desc);

    // Track positions and direction changes over several updates
    for (int i = 0; i < 20; i++) {
      Location curLocation = blinky.getCurrentLocation();
      Direction curDirection = blinky.getCurrentDirection();

      if (prevLocation != null) {
        if (!curLocation.equals(prevLocation)) {
          positionChanges++;
        }
      }

      if (prevDirection != null) {
        if (curDirection != prevDirection) {
          directionChanges++;
        }
      }

      prevLocation = curLocation;
      prevDirection = curDirection;
      blinky.update(desc);
    }

    // For 20 updates, a properly moving ghost should have significantly
    // more position changes than direction changes
    assertTrue(
        "Ghost should change position more often than direction",
        positionChanges > directionChanges * 2);
  }

  /** Test ghost behavior at intersections - should make proper turns without oscillation. */
  @Test
  public void testIntersectionBehavior() {
    // Put ghost in a position approaching an intersection
    blinky.reset();
    blinky.setMode(Mode.CHASE, makeDescriptor());

    // Track previous locations
    Location[] previousLocations = new Location[10];
    Direction[] previousDirections = new Direction[10];

    // Update several times, recording positions
    for (int i = 0; i < 10; i++) {
      blinky.update(makeDescriptor());
      previousLocations[i] = blinky.getCurrentLocation();
      previousDirections[i] = blinky.getCurrentDirection();
    }

    // Count oscillations (back-and-forth movements)
    int oscillationCount = 0;
    for (int i = 2; i < 10; i++) {
      if (previousLocations[i].equals(previousLocations[i - 2])
          && !previousLocations[i].equals(previousLocations[i - 1])) {
        oscillationCount++;
      }
    }

    // Should have minimal oscillation
    assertTrue("Ghost should not oscillate at intersections", oscillationCount <= 1);
  }

  /** Test all ghost behaviors in CHASE mode. */
  @Test
  public void testChaseMode() {
    Descriptor desc = makeDescriptor();

    // Set all ghosts to CHASE mode
    blinky.setMode(Mode.CHASE, desc);
    pinky.setMode(Mode.CHASE, desc);
    inky.setMode(Mode.CHASE, desc);
    clyde.setMode(Mode.CHASE, desc);

    // Run updates
    for (int i = 0; i < 10; i++) {
      desc = makeDescriptor(); // Update descriptor
      blinky.update(desc);
      pinky.update(desc);
      inky.update(desc);
      clyde.update(desc);
    }

    // All ghosts should be in CHASE mode
    assertEquals("Blinky should be in CHASE mode", Mode.CHASE, blinky.getMode());
    assertEquals("Pinky should be in CHASE mode", Mode.CHASE, pinky.getMode());
    assertEquals("Inky should be in CHASE mode", Mode.CHASE, inky.getMode());
    assertEquals("Clyde should be in CHASE mode", Mode.CHASE, clyde.getMode());

    // Check speeds
    assertEquals(
        "Ghost speed should be base speed in CHASE mode",
        blinky.getBaseIncrement(),
        blinky.getCurrentIncrement(),
        0.001);
  }

  /** Test all ghost behaviors in FRIGHTENED mode. */
  @Test
  public void testFrightenedMode() {
    Descriptor desc = makeDescriptor();

    // Set all ghosts to FRIGHTENED mode
    blinky.setMode(Mode.FRIGHTENED, desc);
    pinky.setMode(Mode.FRIGHTENED, desc);
    inky.setMode(Mode.FRIGHTENED, desc);
    clyde.setMode(Mode.FRIGHTENED, desc);

    // All ghosts should be in FRIGHTENED mode with reduced speed
    assertEquals("Blinky should be in FRIGHTENED mode", Mode.FRIGHTENED, blinky.getMode());
    assertTrue(
        "Blinky should have reduced speed in FRIGHTENED mode",
        blinky.getCurrentIncrement() < blinky.getBaseIncrement());

    assertEquals("Pinky should be in FRIGHTENED mode", Mode.FRIGHTENED, pinky.getMode());
    assertTrue(
        "Pinky should have reduced speed in FRIGHTENED mode",
        pinky.getCurrentIncrement() < pinky.getBaseIncrement());

    assertEquals("Inky should be in FRIGHTENED mode", Mode.FRIGHTENED, inky.getMode());
    assertTrue(
        "Inky should have reduced speed in FRIGHTENED mode",
        inky.getCurrentIncrement() < inky.getBaseIncrement());

    assertEquals("Clyde should be in FRIGHTENED mode", Mode.FRIGHTENED, clyde.getMode());
    assertTrue(
        "Clyde should have reduced speed in FRIGHTENED mode",
        clyde.getCurrentIncrement() < clyde.getBaseIncrement());

    // Run updates and check that ghosts move without getting stuck
    for (int i = 0; i < 30; i++) {
      desc = makeDescriptor(); // Update descriptor
      blinky.update(desc);
    }

    Location startLoc = blinky.getCurrentLocation();
    double startRow = blinky.getRowExact();
    double startCol = blinky.getColExact();

    // Continue updating
    for (int i = 0; i < 10; i++) {
      desc = makeDescriptor();
      blinky.update(desc);
    }

    // Ghost should have moved
    double endRow = blinky.getRowExact();
    double endCol = blinky.getColExact();

    assertTrue(
        "Ghost should make progress in FRIGHTENED mode",
        Math.abs(endRow - startRow) > 0.1 || Math.abs(endCol - startCol) > 0.1);
  }
}