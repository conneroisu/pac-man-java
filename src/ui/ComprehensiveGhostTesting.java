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
 * Comprehensive ghost testing to find real movement issues.
 */
public class ComprehensiveGhostTesting {
  
  /**
   * Test with the real game maze to find actual stuck scenarios.
   */
  public static void testRealGameMaze() {
    System.out.println("=== COMPREHENSIVE REAL GAME MAZE TEST ===");
    
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
    
    runStuckDetectionTest(maze, "Real Game Maze", 5000); // Run for 100 seconds
  }
  
  /**
   * Test with simple corridor to isolate basic movement issues.
   */
  public static void testSimpleCorridor() {
    System.out.println("\\n=== SIMPLE CORRIDOR EXTENSIVE TEST ===");
    
    String[] maze = {
      "###########",
      "#.........#", 
      "#.........#",
      "#...B.....#", 
      "#.........#",
      "#....S....#",
      "###########"
    };
    
    runStuckDetectionTest(maze, "Simple Corridor", 2000);
  }
  
  /**
   * Test ghost house scenario specifically.
   */
  public static void testGhostHouseDetailed() {
    System.out.println("\\n=== GHOST HOUSE DETAILED TEST ===");
    
    String[] maze = {
      "###############",
      "#.............#",
      "#.............#", 
      "#......###....#",
      "#......#B#....#",
      "#......#P#....#",
      "#......###....#",
      "#.............#",
      "#......S......#",
      "###############"
    };
    
    runStuckDetectionTest(maze, "Ghost House", 3000);
  }
  
  /**
   * Run stuck detection test with detailed analysis.
   */
  private static void runStuckDetectionTest(String[] mazeLayout, String testName, int maxFrames) {
    System.out.println("Running " + testName + " for " + maxFrames + " frames...");
    
    PacmanGame game = new PacmanGame(mazeLayout, 50);
    Actor[] ghosts = game.getEnemies();
    
    // Track stuck patterns
    Map<Integer, List<StuckEvent>> ghostStuckEvents = new HashMap<>();
    Map<Integer, Location> lastPositions = new HashMap<>();
    Map<Integer, Integer> stuckCounters = new HashMap<>();
    Map<Integer, Integer> maxStuckFrames = new HashMap<>();
    Map<Integer, List<Location>> positionHistory = new HashMap<>();
    
    // Initialize tracking
    for (int i = 0; i < ghosts.length; i++) {
      ghostStuckEvents.put(i, new ArrayList<>());
      lastPositions.put(i, ghosts[i].getCurrentLocation());
      stuckCounters.put(i, 0);
      maxStuckFrames.put(i, 0);
      positionHistory.put(i, new ArrayList<>());
    }
    
    // Run simulation
    for (int frame = 0; frame < maxFrames; frame++) {
      game.updateAll();
      
      // Check each ghost
      for (int i = 0; i < ghosts.length; i++) {
        Actor ghost = ghosts[i];
        Location currentPos = ghost.getCurrentLocation();
        Mode currentMode = ghost.getMode();
        Direction currentDir = ghost.getCurrentDirection();
        
        // Record position
        positionHistory.get(i).add(currentPos);
        
        // Check if stuck (only for active ghosts)
        if (currentMode != Mode.INACTIVE) {
          if (currentPos.equals(lastPositions.get(i))) {
            int stuckCount = stuckCounters.get(i) + 1;
            stuckCounters.put(i, stuckCount);
            maxStuckFrames.put(i, Math.max(maxStuckFrames.get(i), stuckCount));
            
            // Record significant stuck events
            if (stuckCount == 50) { // 1 second stuck
              StuckEvent event = new StuckEvent(frame, currentPos, currentMode, currentDir, stuckCount);
              ghostStuckEvents.get(i).add(event);
              System.out.println("STUCK EVENT: Frame " + frame + " Ghost " + i + " stuck for " + stuckCount + " frames at " + currentPos);
            }
          } else {
            // Ghost moved - reset counter
            int stuckCount = stuckCounters.get(i);
            if (stuckCount > 30) {
              StuckEvent event = new StuckEvent(frame - stuckCount, lastPositions.get(i), currentMode, currentDir, stuckCount);
              ghostStuckEvents.get(i).add(event);
              System.out.println("RECOVERED: Frame " + frame + " Ghost " + i + " was stuck for " + stuckCount + " frames at " + lastPositions.get(i));
            }
            stuckCounters.put(i, 0);
          }
        } else {
          stuckCounters.put(i, 0); // Reset for inactive ghosts
        }
        
        lastPositions.put(i, currentPos);
      }
      
      // Print detailed status every 500 frames
      if (frame % 500 == 0) {
        System.out.println("\\nFrame " + frame + " Status:");
        for (int i = 0; i < ghosts.length; i++) {
          Actor ghost = ghosts[i];
          System.out.println("  Ghost " + i + " (" + ghost.getClass().getSimpleName() + "): " + 
                           ghost.getCurrentLocation() + " exact=(" + String.format("%.2f,%.2f", ghost.getRowExact(), ghost.getColExact()) + ") " +
                           "mode=" + ghost.getMode() + " dir=" + ghost.getCurrentDirection() + 
                           " stuck=" + stuckCounters.get(i));
        }
      }
    }
    
    // Analyze results
    System.out.println("\\n" + "=".repeat(60));
    System.out.println("ANALYSIS FOR " + testName.toUpperCase());
    System.out.println("=".repeat(60));
    
    boolean foundIssues = false;
    
    for (int i = 0; i < ghosts.length; i++) {
      System.out.println("\\nGhost " + i + " (" + ghosts[i].getClass().getSimpleName() + "):");
      System.out.println("  Max consecutive stuck frames: " + maxStuckFrames.get(i));
      System.out.println("  Total stuck events: " + ghostStuckEvents.get(i).size());
      
      if (maxStuckFrames.get(i) > 100) { // More than 2 seconds stuck
        System.out.println("  ❌ CRITICAL: Ghost gets stuck for extended periods!");
        foundIssues = true;
        
        // Show stuck events
        for (StuckEvent event : ghostStuckEvents.get(i)) {
          System.out.println("    - Frame " + event.frame + ": Stuck " + event.duration + " frames at " + 
                           event.location + " (mode=" + event.mode + " dir=" + event.direction + ")");
        }
      } else if (maxStuckFrames.get(i) > 50) {
        System.out.println("  ⚠️  WARNING: Ghost occasionally gets stuck!");
        foundIssues = true;
      } else {
        System.out.println("  ✅ OK: Normal movement");
      }
      
      // Check for oscillation patterns
      List<Location> positions = positionHistory.get(i);
      if (positions.size() > 100) {
        Map<String, Integer> patterns = findOscillationPatterns(positions);
        if (!patterns.isEmpty()) {
          System.out.println("  🔄 OSCILLATION PATTERNS DETECTED:");
          foundIssues = true;
          for (Map.Entry<String, Integer> pattern : patterns.entrySet()) {
            if (pattern.getValue() > 10) {
              System.out.println("    - " + pattern.getKey() + " (repeated " + pattern.getValue() + " times)");
            }
          }
        }
      }
    }
    
    if (!foundIssues) {
      System.out.println("\\n✅ TEST PASSED: No significant issues detected");
    } else {
      System.out.println("\\n❌ TEST FAILED: Issues detected that need fixing");
    }
  }
  
  /**
   * Find oscillation patterns in position history.
   */
  private static Map<String, Integer> findOscillationPatterns(List<Location> positions) {
    Map<String, Integer> patterns = new HashMap<>();
    
    // Look for 2-cell oscillations (A->B->A->B)
    for (int i = 0; i < positions.size() - 3; i++) {
      Location loc1 = positions.get(i);
      Location loc2 = positions.get(i + 1);
      Location loc3 = positions.get(i + 2);
      Location loc4 = positions.get(i + 3);
      
      if (loc1.equals(loc3) && loc2.equals(loc4) && !loc1.equals(loc2)) {
        String pattern = loc1 + "<->" + loc2;
        patterns.put(pattern, patterns.getOrDefault(pattern, 0) + 1);
      }
    }
    
    // Look for 3-cell cycles (A->B->C->A->B->C)
    for (int i = 0; i < positions.size() - 5; i++) {
      Location loc1 = positions.get(i);
      Location loc2 = positions.get(i + 1);
      Location loc3 = positions.get(i + 2);
      Location loc4 = positions.get(i + 3);
      Location loc5 = positions.get(i + 4);
      Location loc6 = positions.get(i + 5);
      
      if (loc1.equals(loc4) && loc2.equals(loc5) && loc3.equals(loc6)) {
        String pattern = loc1 + "->" + loc2 + "->" + loc3;
        patterns.put(pattern, patterns.getOrDefault(pattern, 0) + 1);
      }
    }
    
    return patterns;
  }
  
  /**
   * Represents a stuck event.
   */
  private static class StuckEvent {
    final int frame;
    final Location location;
    final Mode mode;
    final Direction direction;
    final int duration;
    
    StuckEvent(int frame, Location location, Mode mode, Direction direction, int duration) {
      this.frame = frame;
      this.location = location;
      this.mode = mode;
      this.direction = direction;
      this.duration = duration;
    }
  }
  
  public static void main(String[] args) {
    System.out.println("COMPREHENSIVE GHOST MOVEMENT TESTING");
    System.out.println("=====================================");
    System.out.println("This will run extensive tests to find real movement issues...");
    
    // Run all tests
    testSimpleCorridor();
    testGhostHouseDetailed();
    testRealGameMaze();
    
    System.out.println("\\n" + "=".repeat(60));
    System.out.println("COMPREHENSIVE TESTING COMPLETE");
    System.out.println("=".repeat(60));
  }
}