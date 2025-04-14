package ui;

import static api.Mode.*;

import api.Actor;
import api.Descriptor;
import api.Direction;
import api.Location;
import api.Mode;
import api.PacmanGame;
import hw4.Blinky;

/**
 * Tests maze navigation, wall detection, and ghost behavior over multiple frames.
 */
public class MazeNavigationTest {

  // Test maze with walls, Blinky, and pacman
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

  public static void main(String[] args) {
    // Create game with test maze (frameRate = 10)
    PacmanGame game = new PacmanGame(NAVIGATION_TEST, 10);
    
    System.out.println("=== MAZE NAVIGATION TEST ===\n");
    
    // Get Blinky
    Actor blinky = game.getEnemies()[0];
    
    // Create descriptor
    Descriptor descriptor = makeDescriptor(game);
    
    // Test CHASE mode navigation
    System.out.println("=== CHASE MODE NAVIGATION TEST ===");
    testChaseNavigation(blinky, descriptor, 20);
    
    // Reset Blinky
    blinky.reset();
    
    // Test SCATTER mode navigation
    System.out.println("\n=== SCATTER MODE NAVIGATION TEST ===");
    testScatterNavigation(blinky, descriptor, 20);
    
    // Reset Blinky
    blinky.reset();
    
    // Test FRIGHTENED mode navigation
    System.out.println("\n=== FRIGHTENED MODE NAVIGATION TEST ===");
    testFrightenedNavigation(blinky, descriptor, 20);
    
    System.out.println("\n=== TEST COMPLETE ===");
  }
  
  /**
   * Tests CHASE mode navigation.
   */
  private static void testChaseNavigation(Actor ghost, Descriptor desc, int steps) {
    // Set ghost to CHASE mode
    ghost.setMode(CHASE, desc);
    
    System.out.println("Pacman location: " + desc.getPlayerLocation());
    System.out.println("Initial ghost position: " + ghost.getCurrentLocation() + 
                      ", direction: " + ghost.getCurrentDirection());
    
    // Run simulation for several steps
    for (int i = 0; i < steps; i++) {
      ghost.update(desc);
      System.out.println("Step " + (i + 1) + ": " + ghost.getCurrentLocation() + 
                        ", exact: (" + ghost.getRowExact() + ", " + ghost.getColExact() + "), " +
                        "direction: " + ghost.getCurrentDirection());
    }
  }
  
  /**
   * Tests SCATTER mode navigation.
   */
  private static void testScatterNavigation(Actor ghost, Descriptor desc, int steps) {
    // Set ghost to SCATTER mode
    ghost.setMode(SCATTER, desc);
    
    System.out.println("Initial ghost position: " + ghost.getCurrentLocation() + 
                      ", direction: " + ghost.getCurrentDirection());
    
    // Run simulation for several steps
    for (int i = 0; i < steps; i++) {
      ghost.update(desc);
      System.out.println("Step " + (i + 1) + ": " + ghost.getCurrentLocation() + 
                        ", exact: (" + ghost.getRowExact() + ", " + ghost.getColExact() + "), " +
                        "direction: " + ghost.getCurrentDirection());
    }
  }
  
  /**
   * Tests FRIGHTENED mode navigation.
   */
  private static void testFrightenedNavigation(Actor ghost, Descriptor desc, int steps) {
    // Set ghost to FRIGHTENED mode
    ghost.setMode(FRIGHTENED, desc);
    
    System.out.println("Initial ghost position: " + ghost.getCurrentLocation() + 
                      ", direction: " + ghost.getCurrentDirection());
    
    // Run simulation for several steps
    for (int i = 0; i < steps; i++) {
      ghost.update(desc);
      System.out.println("Step " + (i + 1) + ": " + ghost.getCurrentLocation() + 
                        ", exact: (" + ghost.getRowExact() + ", " + ghost.getColExact() + "), " +
                        "direction: " + ghost.getCurrentDirection());
    }
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