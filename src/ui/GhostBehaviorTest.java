package ui;

import static api.Mode.*;

import api.Actor;
import api.Descriptor;
import api.Direction;
import api.Location;
import api.Mode;
import api.PacmanGame;
import hw4.Blinky;
import hw4.Clyde;
import hw4.Inky;
import hw4.Pinky;

/**
 * Test for verifying all ghost behaviors (Blinky, Pinky, Inky, Clyde)
 * in various modes (CHASE, SCATTER, FRIGHTENED).
 */
public class GhostBehaviorTest {

  // Test maze with all ghosts and pacman
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

  public static void main(String[] args) {
    // Create game with test maze (frameRate = 10)
    PacmanGame game = new PacmanGame(TEST_MAZE, 10);
    
    System.out.println("=== GHOST BEHAVIOR TEST ===\n");
    
    // Get all ghosts
    Actor[] ghosts = game.getEnemies();
    if (ghosts.length < 4) {
      System.out.println("ERROR: Not all ghosts are implemented. Found only " + ghosts.length);
      return;
    }
    
    // Verify we have all ghost types
    Blinky blinky = null;
    Pinky pinky = null;
    Inky inky = null;
    Clyde clyde = null;
    
    // Testing ghost types
    for (Actor ghost : ghosts) {
      if (ghost instanceof Blinky) blinky = (Blinky) ghost;
      else if (ghost instanceof Pinky) pinky = (Pinky) ghost;
      else if (ghost instanceof Inky) inky = (Inky) ghost;
      else if (ghost instanceof Clyde) clyde = (Clyde) ghost;
    }
    
    // Check that all ghost types are present
    boolean allImplemented = true;
    if (blinky == null) {
      System.out.println("ERROR: Blinky is not implemented");
      allImplemented = false;
    }
    if (pinky == null) {
      System.out.println("ERROR: Pinky is not implemented");
      allImplemented = false;
    }
    if (inky == null) {
      System.out.println("ERROR: Inky is not implemented");
      allImplemented = false;
    }
    if (clyde == null) {
      System.out.println("ERROR: Clyde is not implemented");
      allImplemented = false;
    }
    
    if (!allImplemented) {
      return;
    }
    
    System.out.println("All ghost types are implemented!\n");
    
    // Create descriptors for testing
    Descriptor descriptor = makeDescriptor(game);
    
    // Test SCATTER mode for all ghosts
    System.out.println("=== TESTING SCATTER MODE ===");
    testScatterMode(blinky, pinky, inky, clyde, descriptor);
    
    // Test CHASE mode for all ghosts
    System.out.println("\n=== TESTING CHASE MODE ===");
    testChaseMode(blinky, pinky, inky, clyde, descriptor);
    
    // Test FRIGHTENED mode for all ghosts
    System.out.println("\n=== TESTING FRIGHTENED MODE ===");
    testFrightenedMode(blinky, pinky, inky, clyde, descriptor);
    
    // Test movement and wall detection
    System.out.println("\n=== TESTING MOVEMENT AND WALL DETECTION ===");
    testMovementAndWallDetection(blinky, descriptor);
    
    System.out.println("\n=== TEST COMPLETE ===");
  }

  /**
   * Tests SCATTER mode behavior for all ghosts.
   */
  private static void testScatterMode(Blinky blinky, Pinky pinky, Inky inky, Clyde clyde, Descriptor desc) {
    // Set all ghosts to SCATTER mode
    blinky.setMode(SCATTER, desc);
    pinky.setMode(SCATTER, desc);
    inky.setMode(SCATTER, desc);
    clyde.setMode(SCATTER, desc);
    
    // Verify they are all in SCATTER mode
    verifyMode(blinky, SCATTER, "Blinky");
    verifyMode(pinky, SCATTER, "Pinky");
    verifyMode(inky, SCATTER, "Inky");
    verifyMode(clyde, SCATTER, "Clyde");
    
    // Get initial and target locations
    Location blinkyTarget = getNextTargetLocation(blinky, desc);
    Location pinkyTarget = getNextTargetLocation(pinky, desc);
    Location inkyTarget = getNextTargetLocation(inky, desc);
    Location clydeTarget = getNextTargetLocation(clyde, desc);
    
    // Check that each ghost has a different corner target in SCATTER mode
    System.out.println("Blinky SCATTER target: " + blinkyTarget);
    System.out.println("Pinky SCATTER target: " + pinkyTarget);
    System.out.println("Inky SCATTER target: " + inkyTarget);
    System.out.println("Clyde SCATTER target: " + clydeTarget);
  }
  
  /**
   * Tests CHASE mode behavior for all ghosts.
   */
  private static void testChaseMode(Blinky blinky, Pinky pinky, Inky inky, Clyde clyde, Descriptor desc) {
    // Set all ghosts to CHASE mode
    blinky.setMode(CHASE, desc);
    pinky.setMode(CHASE, desc);
    inky.setMode(CHASE, desc);
    clyde.setMode(CHASE, desc);
    
    // Verify they are all in CHASE mode
    verifyMode(blinky, CHASE, "Blinky");
    verifyMode(pinky, CHASE, "Pinky");
    verifyMode(inky, CHASE, "Inky");
    verifyMode(clyde, CHASE, "Clyde");
    
    // Get initial and target locations
    Location pacmanLocation = desc.getPlayerLocation();
    Location blinkyTarget = getNextTargetLocation(blinky, desc);
    Location pinkyTarget = getNextTargetLocation(pinky, desc);
    Location inkyTarget = getNextTargetLocation(inky, desc);
    Location clydeTarget = getNextTargetLocation(clyde, desc);
    
    // Test Blinky (should target Pacman directly)
    System.out.println("Pacman location: " + pacmanLocation);
    System.out.println("Blinky CHASE target: " + blinkyTarget);
    System.out.println("Pinky CHASE target: " + pinkyTarget);
    System.out.println("Inky CHASE target: " + inkyTarget);
    System.out.println("Clyde CHASE target: " + clydeTarget);
  }
  
  /**
   * Tests FRIGHTENED mode behavior for all ghosts.
   */
  private static void testFrightenedMode(Blinky blinky, Pinky pinky, Inky inky, Clyde clyde, Descriptor desc) {
    // Set all ghosts to FRIGHTENED mode
    blinky.setMode(FRIGHTENED, desc);
    pinky.setMode(FRIGHTENED, desc);
    inky.setMode(FRIGHTENED, desc);
    clyde.setMode(FRIGHTENED, desc);
    
    // Verify they are all in FRIGHTENED mode
    verifyMode(blinky, FRIGHTENED, "Blinky");
    verifyMode(pinky, FRIGHTENED, "Pinky");
    verifyMode(inky, FRIGHTENED, "Inky");
    verifyMode(clyde, FRIGHTENED, "Clyde");
    
    // Check speed reduction
    verifySpeedReduction(blinky, "Blinky");
    verifySpeedReduction(pinky, "Pinky");
    verifySpeedReduction(inky, "Inky");
    verifySpeedReduction(clyde, "Clyde");
    
    // Run each ghost for a few updates to check random movement
    System.out.println("\nTesting random movement in FRIGHTENED mode:");
    testRandomMovement(blinky, desc, "Blinky");
    testRandomMovement(pinky, desc, "Pinky");
    testRandomMovement(inky, desc, "Inky");
    testRandomMovement(clyde, desc, "Clyde");
  }
  
  /**
   * Tests ghost movement and wall detection.
   */
  private static void testMovementAndWallDetection(Actor ghost, Descriptor desc) {
    // Set ghost to a known state
    ghost.setMode(SCATTER, desc);
    
    System.out.println("Initial position: " + ghost.getCurrentLocation());
    System.out.println("Initial direction: " + ghost.getCurrentDirection());
    
    // Run updates and check position
    for (int i = 0; i < 10; i++) {
      ghost.update(desc);
      System.out.println("After update " + (i + 1) + ": " + 
                        ghost.getCurrentLocation() + ", " +
                        "exact: (" + ghost.getRowExact() + ", " + ghost.getColExact() + "), " +
                        "direction: " + ghost.getCurrentDirection());
    }
  }
  
  /**
   * Helper method to test random movement in FRIGHTENED mode.
   */
  private static void testRandomMovement(Actor ghost, Descriptor desc, String name) {
    System.out.println("\n" + name + " random movement test:");
    Location start = ghost.getCurrentLocation();
    Direction startDir = ghost.getCurrentDirection();
    
    System.out.println("Start: " + start + ", direction: " + startDir);
    
    // Update a few times to see if direction changes
    for (int i = 0; i < 5; i++) {
      ghost.update(desc);
      System.out.println("Update " + (i + 1) + ": " + ghost.getCurrentLocation() + 
                        ", direction: " + ghost.getCurrentDirection());
    }
  }
  
  /**
   * Verifies that a ghost is in the expected mode.
   */
  private static void verifyMode(Actor ghost, Mode expectedMode, String name) {
    if (ghost.getMode() == expectedMode) {
      System.out.println(name + " is correctly in " + expectedMode + " mode");
    } else {
      System.out.println("ERROR: " + name + " should be in " + expectedMode + 
                        " mode but is in " + ghost.getMode() + " mode");
    }
  }
  
  /**
   * Verifies that a ghost's speed is reduced in FRIGHTENED mode.
   */
  private static void verifySpeedReduction(Actor ghost, String name) {
    double baseSpeed = ghost.getBaseIncrement();
    double currentSpeed = ghost.getCurrentIncrement();
    
    if (currentSpeed < baseSpeed) {
      System.out.println(name + " speed is reduced from " + baseSpeed + " to " + currentSpeed);
    } else {
      System.out.println("ERROR: " + name + " speed should be reduced in FRIGHTENED mode");
    }
  }
  
  /**
   * Gets a ghost's next target location by setting a marker.
   */
  private static Location getNextTargetLocation(Actor ghost, Descriptor desc) {
    // We don't have direct access to getTargetLocation, so we'll infer from behavior
    // Store the original location and direction
    Location originalLoc = ghost.getCurrentLocation();
    Direction originalDir = ghost.getCurrentDirection();
    
    // Do an update to see where the ghost wants to go
    ghost.update(desc);
    
    // Record new position and direction
    Location newLoc = ghost.getCurrentLocation();
    Direction newDir = ghost.getCurrentDirection();
    
    // Reset to original position since this is just a test
    ghost.setRowExact(originalLoc.row() + 0.5);
    ghost.setColExact(originalLoc.col() + 0.5);
    ghost.setDirection(originalDir);
    
    // Return the direction the ghost was moving in
    // This doesn't give us the exact target, but lets us see the general direction
    return newLoc;
  }
  
  /**
   * Creates a game descriptor for testing.
   */
  private static Descriptor makeDescriptor(PacmanGame game) {
    Location enemyLoc = game.getEnemies()[0].getCurrentLocation();
    Location playerLoc = game.getPlayer().getCurrentLocation();
    Direction playerDir = game.getPlayer().getCurrentDirection();
    return new Descriptor(playerLoc, playerDir, enemyLoc);
  }
}