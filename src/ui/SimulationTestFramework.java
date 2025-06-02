package ui;

import api.Actor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import api.PacmanGame;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Deterministic simulation testing framework for Pac-Man ghost behavior.
 * This framework runs the game for a specified number of frames and records
 * all ghost states, then applies various assertions to detect movement bugs.
 */
public class SimulationTestFramework {
  
  /**
   * Represents the state of a ghost at a specific frame.
   */
  public static class GhostFrame {
    public final int frameNumber;
    public final String ghostType;
    public final Location location;
    public final double rowExact;
    public final double colExact;
    public final Direction direction;
    public final Mode mode;
    public final double speed;
    public final boolean isMoving;
    
    public GhostFrame(int frameNumber, String ghostType, Actor ghost) {
      this.frameNumber = frameNumber;
      this.ghostType = ghostType;
      this.location = ghost.getCurrentLocation();
      this.rowExact = ghost.getRowExact();
      this.colExact = ghost.getColExact();
      this.direction = ghost.getCurrentDirection();
      this.mode = ghost.getMode();
      this.speed = ghost.getCurrentIncrement();
      this.isMoving = speed > 0 && mode != Mode.INACTIVE;
    }
    
    @Override
    public String toString() {
      return String.format("Frame %d: %s at %s (%.2f,%.2f) dir=%s mode=%s speed=%.3f moving=%s",
          frameNumber, ghostType, location, rowExact, colExact, direction, mode, speed, isMoving);
    }
  }
  
  /**
   * Represents the complete simulation data for analysis.
   */
  public static class SimulationData {
    public final List<List<GhostFrame>> ghostTraces;
    public final PacmanGame game;
    public final int totalFrames;
    public final String[] mazeLayout;
    
    public SimulationData(List<List<GhostFrame>> ghostTraces, PacmanGame game, int totalFrames, String[] mazeLayout) {
      this.ghostTraces = ghostTraces;
      this.game = game;
      this.totalFrames = totalFrames;
      this.mazeLayout = mazeLayout;
    }
    
    /**
     * Gets all frames for a specific ghost type.
     */
    public List<GhostFrame> getGhostTrace(String ghostType) {
      for (List<GhostFrame> trace : ghostTraces) {
        if (!trace.isEmpty() && trace.get(0).ghostType.equals(ghostType)) {
          return trace;
        }
      }
      return new ArrayList<>();
    }
    
    /**
     * Gets frame data for all ghosts at a specific frame number.
     */
    public List<GhostFrame> getFrameData(int frameNumber) {
      List<GhostFrame> frameData = new ArrayList<>();
      for (List<GhostFrame> trace : ghostTraces) {
        if (frameNumber < trace.size()) {
          frameData.add(trace.get(frameNumber));
        }
      }
      return frameData;
    }
  }
  
  /**
   * Interface for simulation assertions.
   */
  public interface SimulationAssertion {
    AssertionResult check(SimulationData data);
  }
  
  /**
   * Result of an assertion check.
   */
  public static class AssertionResult {
    public final boolean passed;
    public final String message;
    public final List<GhostFrame> evidence;
    
    public AssertionResult(boolean passed, String message) {
      this(passed, message, new ArrayList<>());
    }
    
    public AssertionResult(boolean passed, String message, List<GhostFrame> evidence) {
      this.passed = passed;
      this.message = message;
      this.evidence = evidence;
    }
    
    public static AssertionResult pass(String message) {
      return new AssertionResult(true, message);
    }
    
    public static AssertionResult fail(String message) {
      return new AssertionResult(false, message);
    }
    
    public static AssertionResult fail(String message, List<GhostFrame> evidence) {
      return new AssertionResult(false, message, evidence);
    }
  }
  
  /**
   * Configuration for simulation runs.
   */
  public static class SimulationConfig {
    public final int framesToRun;
    public final long randomSeed;
    public final boolean verboseLogging;
    public final Direction[] playerInputs; // Optional player inputs sequence
    
    public SimulationConfig(int framesToRun, long randomSeed, boolean verboseLogging) {
      this(framesToRun, randomSeed, verboseLogging, null);
    }
    
    public SimulationConfig(int framesToRun, long randomSeed, boolean verboseLogging, Direction[] playerInputs) {
      this.framesToRun = framesToRun;
      this.randomSeed = randomSeed;
      this.verboseLogging = verboseLogging;
      this.playerInputs = playerInputs;
    }
  }
  
  /**
   * Runs a deterministic simulation of the game.
   */
  public static SimulationData runSimulation(String[] mazeLayout, SimulationConfig config) {
    // Create game with deterministic settings
    final int frameRate = 30; // Use consistent frame rate
    PacmanGame game = new PacmanGame(mazeLayout, frameRate);
    
    Actor[] enemies = game.getEnemies();
    List<List<GhostFrame>> ghostTraces = new ArrayList<>();
    
    // Initialize trace lists for each ghost
    for (int i = 0; i < enemies.length; i++) {
      ghostTraces.add(new ArrayList<>());
    }
    
    if (config.verboseLogging) {
      System.out.println("Starting simulation for " + config.framesToRun + " frames");
      System.out.println("Maze size: " + game.getNumRows() + "x" + game.getNumColumns());
      System.out.println("Ghost count: " + enemies.length);
    }
    
    // Run simulation
    for (int frame = 0; frame < config.framesToRun && !game.levelOver(); frame++) {
      // Apply player input if provided
      if (config.playerInputs != null && frame < config.playerInputs.length && config.playerInputs[frame] != null) {
        game.turnPlayer(config.playerInputs[frame]);
      }
      
      // Record ghost states before update
      for (int i = 0; i < enemies.length; i++) {
        String ghostType = enemies[i].getClass().getSimpleName();
        GhostFrame ghostFrame = new GhostFrame(frame, ghostType, enemies[i]);
        ghostTraces.get(i).add(ghostFrame);
        
        if (config.verboseLogging && frame % 30 == 0) { // Log every second at 30fps
          System.out.println("  " + ghostFrame);
        }
      }
      
      // Update game state
      game.updateAll();
    }
    
    if (config.verboseLogging) {
      System.out.println("Simulation completed");
    }
    
    return new SimulationData(ghostTraces, game, config.framesToRun, mazeLayout);
  }
  
  /**
   * Assertion: Ghosts should never stop moving (except when INACTIVE in ghost house).
   */
  public static final SimulationAssertion NO_INAPPROPRIATE_STOPPING = (data) -> {
    List<GhostFrame> violations = new ArrayList<>();
    
    for (List<GhostFrame> trace : data.ghostTraces) {
      for (int i = 1; i < trace.size(); i++) {
        GhostFrame current = trace.get(i);
        GhostFrame previous = trace.get(i - 1);
        
        // Skip if ghost is appropriately inactive
        if (current.mode == Mode.INACTIVE) {
          continue;
        }
        
        // Check if ghost stopped moving inappropriately
        boolean stoppedMoving = Math.abs(current.rowExact - previous.rowExact) < 0.001 && 
                               Math.abs(current.colExact - previous.colExact) < 0.001;
        
        if (stoppedMoving && current.isMoving) {
          violations.add(current);
        }
      }
    }
    
    if (violations.isEmpty()) {
      return AssertionResult.pass("No inappropriate stopping detected");
    } else {
      return AssertionResult.fail("Detected " + violations.size() + " cases of inappropriate stopping", violations);
    }
  };
  
  /**
   * Assertion: Ghosts should never intersect with walls.
   */
  public static final SimulationAssertion NO_WALL_INTERSECTION = (data) -> {
    List<GhostFrame> violations = new ArrayList<>();
    MazeMap maze = new MazeMap(data.game);
    
    for (List<GhostFrame> trace : data.ghostTraces) {
      for (GhostFrame frame : trace) {
        // Check if ghost position is in a wall
        int row = (int) Math.floor(frame.rowExact);
        int col = (int) Math.floor(frame.colExact);
        
        // Ensure bounds checking
        if (row >= 0 && row < maze.getNumRows() && col >= 0 && col < maze.getNumColumns()) {
          if (maze.isWall(row, col)) {
            violations.add(frame);
          }
        }
      }
    }
    
    if (violations.isEmpty()) {
      return AssertionResult.pass("No wall intersections detected");
    } else {
      return AssertionResult.fail("Detected " + violations.size() + " wall intersections", violations);
    }
  };
  
  /**
   * Assertion: Ghost positions should always be within maze bounds.
   */
  public static final SimulationAssertion WITHIN_MAZE_BOUNDS = (data) -> {
    List<GhostFrame> violations = new ArrayList<>();
    int numRows = data.game.getNumRows();
    int numCols = data.game.getNumColumns();
    
    for (List<GhostFrame> trace : data.ghostTraces) {
      for (GhostFrame frame : trace) {
        if (frame.rowExact < 0 || frame.rowExact >= numRows || 
            frame.colExact < 0 || frame.colExact >= numCols) {
          violations.add(frame);
        }
      }
    }
    
    if (violations.isEmpty()) {
      return AssertionResult.pass("All ghosts remained within maze bounds");
    } else {
      return AssertionResult.fail("Detected " + violations.size() + " out-of-bounds positions", violations);
    }
  };
  
  /**
   * Assertion: Ghost movement should be consistent with their speed.
   */
  public static final SimulationAssertion CONSISTENT_MOVEMENT_SPEED = (data) -> {
    List<GhostFrame> violations = new ArrayList<>();
    final double TOLERANCE = 0.01; // Small tolerance for floating point precision
    
    for (List<GhostFrame> trace : data.ghostTraces) {
      for (int i = 1; i < trace.size(); i++) {
        GhostFrame current = trace.get(i);
        GhostFrame previous = trace.get(i - 1);
        
        if (current.mode == Mode.INACTIVE) {
          continue; // Skip inactive ghosts
        }
        
        // Calculate actual distance moved
        double actualDistance = Math.sqrt(
            Math.pow(current.rowExact - previous.rowExact, 2) +
            Math.pow(current.colExact - previous.colExact, 2)
        );
        
        // Expected distance should be close to speed (unless hitting wall or stopping)
        double expectedDistance = current.speed;
        
        // Allow for some cases where movement might be less (wall collisions, direction changes)
        if (actualDistance > expectedDistance + TOLERANCE) {
          violations.add(current);
        }
      }
    }
    
    if (violations.isEmpty()) {
      return AssertionResult.pass("Ghost movement speeds are consistent");
    } else {
      return AssertionResult.fail("Detected " + violations.size() + " cases of inconsistent movement speed", violations);
    }
  };
  
  /**
   * Assertion: Dead ghosts should move toward home.
   */
  public static final SimulationAssertion DEAD_GHOSTS_RETURN_HOME = (data) -> {
    List<GhostFrame> violations = new ArrayList<>();
    
    for (List<GhostFrame> trace : data.ghostTraces) {
      for (int i = 1; i < trace.size(); i++) {
        GhostFrame current = trace.get(i);
        GhostFrame previous = trace.get(i - 1);
        
        if (current.mode != Mode.DEAD) {
          continue;
        }
        
        // For dead ghosts, check if they're moving toward home
        Actor ghost = null;
        for (Actor enemy : data.game.getEnemies()) {
          if (enemy.getClass().getSimpleName().equals(current.ghostType)) {
            ghost = enemy;
            break;
          }
        }
        
        if (ghost != null) {
          Location home = ghost.getHomeLocation();
          
          // Calculate distance to home before and after
          double previousDistance = Math.sqrt(
              Math.pow(previous.rowExact - home.row(), 2) +
              Math.pow(previous.colExact - home.col(), 2)
          );
          
          double currentDistance = Math.sqrt(
              Math.pow(current.rowExact - home.row(), 2) +
              Math.pow(current.colExact - home.col(), 2)
          );
          
          // Dead ghost should generally be getting closer to home (with some tolerance)
          if (currentDistance > previousDistance + 0.1 && !current.location.equals(home)) {
            violations.add(current);
          }
        }
      }
    }
    
    if (violations.isEmpty()) {
      return AssertionResult.pass("Dead ghosts properly return home");
    } else {
      return AssertionResult.fail("Detected " + violations.size() + " cases where dead ghosts moved away from home", violations);
    }
  };
  
  /**
   * Assertion: Ghosts should not get stuck in infinite loops in small areas.
   */
  public static final SimulationAssertion NO_INFINITE_LOOPS = (data) -> {
    List<GhostFrame> violations = new ArrayList<>();
    final int LOOP_DETECTION_WINDOW = 20; // Check for loops over 20 frames
    final int MIN_UNIQUE_POSITIONS = 3; // Should visit at least 3 different positions
    
    for (List<GhostFrame> trace : data.ghostTraces) {
      if (trace.size() < LOOP_DETECTION_WINDOW) {
        continue;
      }
      
      for (int i = LOOP_DETECTION_WINDOW; i < trace.size(); i++) {
        // Check the last LOOP_DETECTION_WINDOW frames
        List<Location> recentPositions = new ArrayList<>();
        for (int j = i - LOOP_DETECTION_WINDOW; j <= i; j++) {
          recentPositions.add(trace.get(j).location);
        }
        
        // Count unique positions
        long uniquePositions = recentPositions.stream().distinct().count();
        
        if (uniquePositions < MIN_UNIQUE_POSITIONS) {
          violations.add(trace.get(i));
        }
      }
    }
    
    if (violations.isEmpty()) {
      return AssertionResult.pass("No infinite loops detected");
    } else {
      return AssertionResult.fail("Detected " + violations.size() + " potential infinite loops", violations);
    }
  };
  
  /**
   * Runs a comprehensive test suite with all standard assertions.
   */
  public static void runComprehensiveTest(String[] mazeLayout, SimulationConfig config) {
    System.out.println("=== Running Comprehensive Ghost Movement Tests ===");
    
    SimulationData data = runSimulation(mazeLayout, config);
    
    SimulationAssertion[] assertions = {
        NO_INAPPROPRIATE_STOPPING,
        NO_WALL_INTERSECTION,
        WITHIN_MAZE_BOUNDS,
        CONSISTENT_MOVEMENT_SPEED,
        DEAD_GHOSTS_RETURN_HOME,
        NO_INFINITE_LOOPS
    };
    
    String[] assertionNames = {
        "No Inappropriate Stopping",
        "No Wall Intersection", 
        "Within Maze Bounds",
        "Consistent Movement Speed",
        "Dead Ghosts Return Home",
        "No Infinite Loops"
    };
    
    int passed = 0;
    int total = assertions.length;
    
    for (int i = 0; i < assertions.length; i++) {
      System.out.println("\n--- " + assertionNames[i] + " ---");
      AssertionResult result = assertions[i].check(data);
      
      if (result.passed) {
        System.out.println("✓ PASSED: " + result.message);
        passed++;
      } else {
        System.out.println("✗ FAILED: " + result.message);
        
        // Show evidence (first few violations)
        int evidenceToShow = Math.min(5, result.evidence.size());
        for (int j = 0; j < evidenceToShow; j++) {
          System.out.println("    Evidence: " + result.evidence.get(j));
        }
        if (result.evidence.size() > evidenceToShow) {
          System.out.println("    ... and " + (result.evidence.size() - evidenceToShow) + " more");
        }
      }
    }
    
    System.out.println("\n=== Summary ===");
    System.out.println("Passed: " + passed + "/" + total + " assertions");
    if (passed == total) {
      System.out.println("🎉 All tests passed!");
    } else {
      System.out.println("❌ " + (total - passed) + " assertions failed");
    }
  }
}