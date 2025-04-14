package ui;

import api.Actor;
import api.Direction;
import api.Location;
import api.Mode;
import api.PacmanGame;

/**
 * Test to verify that ghosts are always positioned at the center of a cell.
 */
public final class GhostPositionTest {

  /** Test maze with the same structure as the main game. */
  public static final String[] TEST_MAZE = {
    "############################",
    "#............##............#",
    "#.####.#####.##.#####.####.#",
    "#*####.#####.##.#####.####*#",
    "#.####.#####.##.#####.####.#",
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
    "#.####.#####.##.#####.####.#",
    "#.####.#####.##.#####.####.#",
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
    System.out.println("Starting Ghost Position Test...");
    
    // Create a game with a reasonable framerate
    PacmanGame game = new PacmanGame(TEST_MAZE, 20);
    Actor[] ghosts = game.getEnemies();
    
    // Check if we have the expected number of ghosts
    if (ghosts.length != 4) {
      System.err.println("ERROR: Expected 4 ghosts, got " + ghosts.length);
      return;
    }
    
    System.out.println("Found " + ghosts.length + " ghosts");
    
    // Set all ghosts to SCATTER mode to ensure they're active
    System.out.println("Setting all ghosts to SCATTER mode");
    for (Actor ghost : ghosts) {
      ghost.setMode(Mode.SCATTER, null);
      System.out.println(ghost.getClass().getSimpleName() + 
                         " at " + ghost.getCurrentLocation() + 
                         " exact: (" + ghost.getRowExact() + ", " + ghost.getColExact() + ")" +
                         " with direction " + ghost.getCurrentDirection());
    }
    
    // Run initial simulation with normal SCATTER mode
    System.out.println("\nRunning initial simulation for 200 frames in SCATTER mode...");
    final int initialFrames = 200;
    
    // Track position errors
    int[] positionErrorCounts = new int[4];
    double[] maxPositionErrors = new double[4];
    
    for (int i = 0; i < initialFrames; i++) {
      game.updateAll();
      checkGhostPositions(ghosts, positionErrorCounts, maxPositionErrors);
      
      // Report progress and position errors every 100 frames
      if (i % 100 == 0) {
        printGhostPositions(i, ghosts, positionErrorCounts);
      }
    }
    
    // Now switch to FRIGHTENED mode to test random movement at intersections
    System.out.println("\nSwitching ghosts to FRIGHTENED mode for 150 frames...");
    for (Actor ghost : ghosts) {
      ghost.setMode(Mode.FRIGHTENED, null);
    }
    
    for (int i = 0; i < 150; i++) {
      game.updateAll();
      checkGhostPositions(ghosts, positionErrorCounts, maxPositionErrors);
      
      // Report progress more frequently during FRIGHTENED mode
      if (i % 50 == 0) {
        printGhostPositions(initialFrames + i, ghosts, positionErrorCounts);
      }
    }
    
    // Finally switch to CHASE mode and move in corridors
    System.out.println("\nSwitching ghosts to CHASE mode for 150 frames...");
    for (Actor ghost : ghosts) {
      ghost.setMode(Mode.CHASE, null);
    }
    
    for (int i = 0; i < 150; i++) {
      game.updateAll();
      checkGhostPositions(ghosts, positionErrorCounts, maxPositionErrors);
      
      // Report progress during CHASE mode
      if (i % 50 == 0) {
        printGhostPositions(initialFrames + 150 + i, ghosts, positionErrorCounts);
      }
    }
    
    // Final report
    int totalFrames = initialFrames + 150 + 150; // Scatter + Frightened + Chase
    System.out.println("\nPosition error report after " + totalFrames + " frames:");
    for (int g = 0; g < ghosts.length; g++) {
      Actor ghost = ghosts[g];
      System.out.println(ghost.getClass().getSimpleName() + 
                       " position errors: " + positionErrorCounts[g] +
                       " max error distance: " + maxPositionErrors[g]);
    }
    
    // Test result summary
    boolean allGhostsCentered = true;
    for (int count : positionErrorCounts) {
      if (count > 0) {
        allGhostsCentered = false;
        break;
      }
    }
    
    if (allGhostsCentered) {
      System.out.println("\nTEST PASSED: All ghosts maintained center cell positioning");
    } else {
      System.out.println("\nTEST FAILED: Some ghosts had positioning errors");
    }
    
    System.out.println("\nTest complete.");
  }
  
  /**
   * Helper method to check ghost positions and update error counts.
   */
  private static void checkGhostPositions(Actor[] ghosts, int[] positionErrorCounts, double[] maxPositionErrors) {
    // Check each ghost's position after movement
    for (int g = 0; g < ghosts.length; g++) {
      Actor ghost = ghosts[g];
      
      // Calculate how far the ghost is from the center of its cell
      double rowExact = ghost.getRowExact();
      double colExact = ghost.getColExact();
      int cellRow = (int)rowExact;
      int cellCol = (int)colExact;
      
      // Distance from center (center of cell is at x.5, y.5)
      double distFromCenterRow = Math.abs((cellRow + 0.5) - rowExact);
      double distFromCenterCol = Math.abs((cellCol + 0.5) - colExact);
      double totalDistFromCenter = Math.sqrt(distFromCenterRow * distFromCenterRow + 
                                        distFromCenterCol * distFromCenterCol);
      
      // Check if the ghost is at a cell's center when it's on a grid point
      // We consider a position error if the ghost is at a grid point but not centered
      boolean atGridPoint = ghost.getCurrentLocation().equals(new Location(cellRow, cellCol));
      if (atGridPoint && totalDistFromCenter > 0.001) {
        positionErrorCounts[g]++;
        if (totalDistFromCenter > maxPositionErrors[g]) {
          maxPositionErrors[g] = totalDistFromCenter;
        }
      }
    }
  }
  
  /**
   * Helper method to print ghost positions.
   */
  private static void printGhostPositions(int frame, Actor[] ghosts, int[] positionErrorCounts) {
    System.out.println("\nFrame " + frame + " ghost positions:");
    for (int g = 0; g < ghosts.length; g++) {
      Actor ghost = ghosts[g];
      double rowExact = ghost.getRowExact();
      double colExact = ghost.getColExact();
      int cellRow = (int)rowExact;
      int cellCol = (int)colExact;
      double distFromCenterRow = Math.abs((cellRow + 0.5) - rowExact);
      double distFromCenterCol = Math.abs((cellCol + 0.5) - colExact);
      
      System.out.println(ghost.getClass().getSimpleName() + 
                       " at " + ghost.getCurrentLocation() + 
                       " exact: (" + ghost.getRowExact() + ", " + ghost.getColExact() + ")" +
                       " center dist: (row=" + distFromCenterRow + ", col=" + distFromCenterCol + ")" +
                       " direction: " + ghost.getCurrentDirection() +
                       " mode: " + ghost.getMode() +
                       " position errors: " + positionErrorCounts[g]);
    }
  }
}