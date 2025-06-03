package ui;

import api.PacmanGame;
import api.Actor;
import api.Location;
import api.Direction;
import api.Mode;
import api.Descriptor;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyze ghost movement to detect stuck patterns.
 */
public class GhostStuckAnalysis {
  
  public static void main(String[] args) {
    String[] maze = {
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
    
    PacmanGame game = new PacmanGame(maze, 50);
    Actor[] ghosts = game.getEnemies();
    
    // Track ghost positions over time
    Map<Actor, List<Location>> positionHistory = new HashMap<>();
    Map<Actor, List<Direction>> directionHistory = new HashMap<>();
    
    for (Actor ghost : ghosts) {
      positionHistory.put(ghost, new ArrayList<>());
      directionHistory.put(ghost, new ArrayList<>());
    }
    
    System.out.println("=== Running simulation to detect stuck ghosts ===");
    
    // Run for 1000 frames (20 seconds)
    for (int frame = 0; frame < 1000; frame++) {
      game.updateAll();
      
      // Record positions and directions
      for (Actor ghost : ghosts) {
        if (ghost.getMode() != Mode.INACTIVE) {
          positionHistory.get(ghost).add(ghost.getCurrentLocation());
          directionHistory.get(ghost).add(ghost.getCurrentDirection());
        }
      }
    }
    
    // Analyze for stuck patterns
    System.out.println("\n=== Stuck Pattern Analysis ===");
    
    for (int i = 0; i < ghosts.length; i++) {
      Actor ghost = ghosts[i];
      List<Location> positions = positionHistory.get(ghost);
      List<Direction> directions = directionHistory.get(ghost);
      
      if (positions.size() < 50) continue; // Skip if not enough data
      
      System.out.println("\nGhost " + i + " (" + ghost.getClass().getSimpleName() + "):");
      
      // Check for oscillation patterns
      Map<String, Integer> patternCount = new HashMap<>();
      
      // Look for 2-cell oscillations
      for (int j = 0; j < positions.size() - 3; j++) {
        Location loc1 = positions.get(j);
        Location loc2 = positions.get(j + 1);
        Location loc3 = positions.get(j + 2);
        Location loc4 = positions.get(j + 3);
        
        if (loc1.equals(loc3) && loc2.equals(loc4) && !loc1.equals(loc2)) {
          String pattern = loc1 + "->" + loc2;
          patternCount.put(pattern, patternCount.getOrDefault(pattern, 0) + 1);
        }
      }
      
      // Look for 3-cell cycles
      for (int j = 0; j < positions.size() - 5; j++) {
        Location loc1 = positions.get(j);
        Location loc2 = positions.get(j + 1);
        Location loc3 = positions.get(j + 2);
        Location loc4 = positions.get(j + 3);
        Location loc5 = positions.get(j + 4);
        Location loc6 = positions.get(j + 5);
        
        if (loc1.equals(loc4) && loc2.equals(loc5) && loc3.equals(loc6)) {
          String pattern = loc1 + "->" + loc2 + "->" + loc3;
          patternCount.put(pattern, patternCount.getOrDefault(pattern, 0) + 1);
        }
      }
      
      // Report patterns found
      boolean isStuck = false;
      for (Map.Entry<String, Integer> entry : patternCount.entrySet()) {
        if (entry.getValue() > 5) { // Pattern repeated more than 5 times
          System.out.println("  STUCK PATTERN: " + entry.getKey() + " (repeated " + entry.getValue() + " times)");
          isStuck = true;
        }
      }
      
      if (!isStuck) {
        // Check if ghost hasn't moved much
        int uniquePositions = (int) positions.stream().distinct().count();
        System.out.println("  Visited " + uniquePositions + " unique positions in " + positions.size() + " frames");
        
        if (uniquePositions < positions.size() / 10) {
          System.out.println("  PROBLEM: Ghost is not exploring maze effectively!");
        }
      }
      
      // Show last few positions
      System.out.println("  Last 10 positions:");
      int start = Math.max(0, positions.size() - 10);
      for (int j = start; j < positions.size(); j++) {
        System.out.println("    " + positions.get(j) + " (dir: " + directions.get(j) + ")");
      }
    }
    
    // Let's also manually check a specific ghost's movement
    System.out.println("\n=== Manual Movement Test for Blinky ===");
    Actor blinky = ghosts[0];
    
    // Reset the game
    game.resetAll();
    
    // Manually activate Blinky
    Descriptor desc = new Descriptor(
        game.getPlayer().getCurrentLocation(),
        game.getPlayer().getCurrentDirection(),
        blinky.getCurrentLocation()
    );
    blinky.setMode(Mode.SCATTER, desc);
    
    System.out.println("Initial state:");
    System.out.println("  Location: " + blinky.getCurrentLocation());
    System.out.println("  Direction: " + blinky.getCurrentDirection());
    
    // Track movement for a few updates
    Location lastLoc = blinky.getCurrentLocation();
    int stuckCounter = 0;
    
    for (int i = 0; i < 100; i++) {
      blinky.update(desc);
      Location currentLoc = blinky.getCurrentLocation();
      
      if (currentLoc.equals(lastLoc)) {
        stuckCounter++;
      } else {
        if (stuckCounter > 10) {
          System.out.println("  Ghost was stuck at " + lastLoc + " for " + stuckCounter + " frames!");
        }
        stuckCounter = 0;
      }
      
      if (i % 10 == 0 || !currentLoc.equals(lastLoc)) {
        System.out.println("  Frame " + i + ": " + currentLoc + " (dir: " + blinky.getCurrentDirection() + ")");
      }
      
      lastLoc = currentLoc;
    }
    
    if (stuckCounter > 10) {
      System.out.println("  Ghost is currently stuck at " + lastLoc + " for " + stuckCounter + " frames!");
    }
  }
}