package ui;

import api.Actor;
import api.CellType;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import api.PacmanGame;

/**
 * Test to verify that ghosts never move through walls.
 */
public final class GhostWallCollisionTest {

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

  /** How many frames to run the test for */
  private static final int TOTAL_FRAMES = 5000;

  /**
   * Main method to run the test.
   * 
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    System.out.println("Starting Ghost Wall Collision Test...");
    
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
                         " at " + ghost.getCurrentLocation());
    }
    
    // Variables to track collision violations
    boolean[] hasViolatedWallConstraint = new boolean[ghosts.length];
    Location[] lastKnownLocation = new Location[ghosts.length];
    
    System.out.println("\nRunning simulation for " + TOTAL_FRAMES + " frames checking wall collisions...");
    
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
        if (lastKnownLocation[g] != null && !currentLoc.equals(lastKnownLocation[g])) {
          // Check if there was a wall between last location and current location
          if (isWallBetween(game, lastKnownLocation[g], currentLoc)) {
            hasViolatedWallConstraint[g] = true;
            System.out.println("VIOLATION: " + ghost.getClass().getSimpleName() + 
                             " moved through a wall from " + lastKnownLocation[g] + 
                             " to " + currentLoc + " at frame " + frame);
            System.out.println("  Exact position: (" + ghost.getRowExact() + ", " + ghost.getColExact() + ")");
          }
        }
      }
      
      // Print progress report every 500 frames
      if (frame % 500 == 0 && frame > 0) {
        System.out.println("\nFrame " + frame + " wall collision status:");
        for (int g = 0; g < ghosts.length; g++) {
          Actor ghost = ghosts[g];
          System.out.println(ghost.getClass().getSimpleName() + 
                         " at " + ghost.getCurrentLocation() + 
                         " exact: (" + ghost.getRowExact() + ", " + ghost.getColExact() + ")" +
                         " direction: " + ghost.getCurrentDirection() +
                         " mode: " + ghost.getMode() +
                         " wall violations: " + hasViolatedWallConstraint[g]);
        }
      }
      
      // Occasionally change modes to test different ghost behaviors
      if (frame == 1000) {
        System.out.println("\nSwitching to CHASE mode at frame 1000");
        for (Actor ghost : ghosts) {
          ghost.setMode(Mode.CHASE, null);
        }
      } else if (frame == 2000) {
        System.out.println("\nSwitching to FRIGHTENED mode at frame 2000");
        for (Actor ghost : ghosts) {
          ghost.setMode(Mode.FRIGHTENED, null);
        }
      } else if (frame == 3000) {
        System.out.println("\nSwitching back to SCATTER mode at frame 3000");
        for (Actor ghost : ghosts) {
          ghost.setMode(Mode.SCATTER, null);
        }
      }
    }
    
    // Final report
    System.out.println("\nFinal wall collision report after " + TOTAL_FRAMES + " frames:");
    for (int g = 0; g < ghosts.length; g++) {
      Actor ghost = ghosts[g];
      System.out.println(ghost.getClass().getSimpleName() + 
                       " wall violations: " + hasViolatedWallConstraint[g]);
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
      System.out.println("\nTEST PASSED: All ghosts respected wall boundaries");
    } else {
      System.out.println("\nTEST FAILED: Some ghosts moved through walls");
    }
    
    System.out.println("\nTest complete.");
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
    } catch (Exception e) {
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
    if (rowDiff > 1 || colDiff > 1) {
      // For tunnel wraparound (horizontal), don't consider it a wall violation
      if (loc1.row() == loc2.row() && 
          ((loc1.col() == 0 && loc2.col() >= game.getNumColumns() - 2) || 
           (loc2.col() == 0 && loc1.col() >= game.getNumColumns() - 2) ||
           (loc1.col() == game.getNumColumns() - 1 && loc2.col() <= 1) || 
           (loc2.col() == game.getNumColumns() - 1 && loc1.col() <= 1))) {
        // This is a valid tunnel transition
        return false;
      }
      
      // Starting positions (already in the tunnel)
      if (isGhostHousePosition(loc1) || isGhostHousePosition(loc2)) {
        // Initial positions - don't count as violations because ghosts start in a special area
        return false;
      }
      
      // Any other large movement is a violation 
      return true;
    }
    
    // For orthogonal movement (not diagonal)
    if (rowDiff + colDiff == 1) {
      // Check if the destination is a wall
      if (isWall(game, loc2.row(), loc2.col())) {
        return true;
      }
    } 
    // For diagonal movement (some games allow this, but most Pac-Man implementations don't)
    else if (rowDiff == 1 && colDiff == 1) {
      // Diagonal movement requires checking both orthogonal paths
      if (isWall(game, loc1.row(), loc2.col()) || isWall(game, loc2.row(), loc1.col())) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Checks if a location is in the ghost house.
   * This is used to avoid counting initial ghost movements out of their starting area as violations.
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
    
    return loc.row() >= GHOST_HOUSE_MIN_ROW && loc.row() <= GHOST_HOUSE_MAX_ROW &&
           loc.col() >= GHOST_HOUSE_MIN_COL && loc.col() <= GHOST_HOUSE_MAX_COL;
  }
}