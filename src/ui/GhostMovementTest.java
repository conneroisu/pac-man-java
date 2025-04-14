package ui;

import api.Actor;
import api.Direction;
import api.Location;
import api.Mode;
import api.PacmanGame;

/**
 * Simple test to check ghost movement and log generation.
 */
public final class GhostMovementTest {

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
    System.out.println("Starting Ghost Movement Test with Logging...");
    
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
                         " with direction " + ghost.getCurrentDirection());
    }
    
    // Run for a number of frames to let ghosts move
    System.out.println("\nRunning simulation for 300 frames...");
    final int framesToRun = 300;
    
    // Track movement for oscillation detection
    Location[] previousLocations = new Location[4];
    int[] oscillationCounts = new int[4];
    
    for (int i = 0; i < framesToRun; i++) {
      game.updateAll();
      
      // Check for oscillation (returning to same position)
      for (int g = 0; g < ghosts.length; g++) {
        Actor ghost = ghosts[g];
        Location currentLoc = ghost.getCurrentLocation();
        
        if (previousLocations[g] != null) {
          // If we've returned to a previous position after moving away,
          // that might indicate oscillation
          if (i > 2 && currentLoc.equals(previousLocations[g])) {
            oscillationCounts[g]++;
          }
        }
        
        previousLocations[g] = currentLoc;
      }
      
      // Report ghost positions every 50 frames
      if (i % 50 == 0) {
        System.out.println("\nFrame " + i + " ghost positions:");
        for (int g = 0; g < ghosts.length; g++) {
          Actor ghost = ghosts[g];
          System.out.println(ghost.getClass().getSimpleName() + 
                            " at " + ghost.getCurrentLocation() + 
                            " exact: (" + ghost.getRowExact() + ", " + ghost.getColExact() + ")" +
                            " with direction " + ghost.getCurrentDirection() + 
                            " mode: " + ghost.getMode() +
                            " oscillation count: " + oscillationCounts[g]);
        }
      }
    }
    
    // Final report
    System.out.println("\nFinal ghost positions after " + framesToRun + " frames:");
    for (int g = 0; g < ghosts.length; g++) {
      Actor ghost = ghosts[g];
      System.out.println(ghost.getClass().getSimpleName() + 
                        " at " + ghost.getCurrentLocation() + 
                        " exact: (" + ghost.getRowExact() + ", " + ghost.getColExact() + ")" +
                        " with direction " + ghost.getCurrentDirection() + 
                        " mode: " + ghost.getMode() +
                        " oscillation count: " + oscillationCounts[g]);
    }
    
    // Check for log file
    java.io.File logFile = new java.io.File("ghost_movement.log");
    if (logFile.exists()) {
      System.out.println("\nLog file created at: " + logFile.getAbsolutePath());
      System.out.println("Log file size: " + logFile.length() + " bytes");
      
      // Read and display the last few lines of the log for verification
      try {
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.FileReader(logFile));
        String line;
        String[] lastLines = new String[10];
        int lineCount = 0;
        
        while ((line = reader.readLine()) != null) {
          lastLines[lineCount % 10] = line;
          lineCount++;
        }
        
        reader.close();
        
        System.out.println("\nLast few log entries:");
        for (int i = 0; i < 10; i++) {
          int index = (lineCount + i) % 10;
          if (lastLines[index] != null) {
            System.out.println(lastLines[index]);
          }
        }
      } catch (Exception e) {
        System.err.println("Error reading log file: " + e.getMessage());
      }
    } else {
      System.err.println("\nERROR: Log file not created!");
    }
    
    System.out.println("\nTest complete.");
  }
}