package com.pacman.ghost;

import static api.Direction.DOWN;
import static api.Direction.LEFT;
import static api.Direction.RIGHT;
import static api.Direction.UP;

import api.Actor;
import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import java.util.Random;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Base implementation of the Actor interface for all ghosts.
 * Provides common ghost behavior like movement, collision detection, and mode handling.
 */
public abstract class ActorImpl implements Actor {
  /** Margin of error for comparing exact coordinates to the center of a cell. */
  private static final double ERR = 0.001;
  
  /** Base speed increment. */
  private double baseIncrement;

  /** A read-only representation of the maze for detecting walls and edges. */
  protected MazeMap maze;
  
  /** The scatter location for scatter mode. */
  private Location scatterTarget;
  
  /** The home/starting location. */
  private Location home;
  
  /** The current location. */
  private Location currentLocation;
  
  /** Previous location for detecting movement. */
  private Location previousLocation;
  
  /** The next target location. */
  private Location nextLocation;
  
  /** The next direction to move. */
  private Direction nextDirection;
  
  /** Current direction of movement. */
  private Direction currentDirection;
  
  /** Initial direction at home position. */
  private Direction homeDirection;
  
  /** The current mode (CHASE, SCATTER, FRIGHTENED, etc.). */
  private Mode currentMode;
  
  /** Exact column position. */
  private double colExact;
  
  /** Exact row position. */
  private double rowExact;
  
  /** Current movement speed. */
  private double currentIncrement;
  
  /** Flag indicating ghost has passed center of current cell. */
  private boolean pastCenter;
  
  /** Random number generator for movement decisions. */
  protected Random rand;
  

  
  // Logging
  private static PrintWriter logWriter;
  private String ghostType;
  private static boolean loggingInitialized = false;
  private static int frameCounter = 0;
  
  /**
   * Static initializer for logging.
   */
  static {
    try {
      logWriter = new PrintWriter(new FileWriter("ghost_movement.log", false));
      logWriter.println("Frame,GhostType,Location,ExactLoc,Direction,NextLocation,Mode,StuckCounter,PastCenter");
      logWriter.flush();
      loggingInitialized = true;
    } catch (IOException e) {
      System.err.println("Failed to create log file: " + e.getMessage());
    }
  }

  /**
   * Constructor for the ghost base class.
   *
   * @param maze The maze map
   * @param home The home/starting location
   * @param baseSpeed The base movement speed
   * @param homeDirection The initial direction
   * @param scatterTarget The target location during scatter mode
   * @param rand Random number generator for movement
   */
  ActorImpl(
      final MazeMap maze,
      final Location home,
      final double baseSpeed,
      final Direction homeDirection,
      final Location scatterTarget,
      final Random rand) {
    this.baseIncrement = baseSpeed;
    this.maze = maze;
    this.home = home;
    this.rowExact = home.row() + 0.5;
    this.colExact = home.col() + 0.5;
    this.scatterTarget = scatterTarget;
    this.currentIncrement = baseSpeed;
    this.homeDirection = homeDirection;
    this.currentDirection = homeDirection;
    this.rand = rand;
    this.pastCenter = false;
    
    // Set ghost type based on class name
    this.ghostType = this.getClass().getSimpleName();

    // Initialize current location
    this.currentLocation = new Location(home.row(), home.col());
    
    // Initialize next direction and location
    this.nextDirection = homeDirection;
    nextLocation = getNextLocation(currentLocation, nextDirection);
    
    // Log initialization
    logMovement("INIT");
  }

  @Override
  public void reset() {
    logMovement("PRE-RESET");
    setMode(Mode.INACTIVE, null);
    currentIncrement = baseIncrement;
    setDirection(homeDirection);
    
    // Always position ghost exactly at cell center when resetting
    double centeredRow = home.row() + 0.5;
    double centeredCol = home.col() + 0.5;
    
    setRowExact(centeredRow);
    setColExact(centeredCol);
    currentLocation = new Location(home.row(), home.col());
    
    pastCenter = false;
    nextDirection = homeDirection;
    nextLocation = getNextLocation(currentLocation, nextDirection);

    logMovement("POST-RESET");
  }

  /**
   * Gets the location in the given direction from the current location.
   * Handles wall checking and maze boundaries.
   *
   * @param from The starting location
   * @param dir The direction to move
   * @return The next location, or null if there's a wall
   */
  private Location getNextLocation(Location from, Direction dir) {
    if (from == null || dir == null) {
      return null;
    }
    
    int row = from.row();
    int col = from.col();
    int numRows = maze.getNumRows();
    int numCols = maze.getNumColumns();
    
    // Calculate the new position based on direction
    switch (dir) {
      case UP:
        if (row > 0 && !maze.isWall(row - 1, col)) {
          return new Location(row - 1, col);
        }
        break;
      case DOWN:
        if (row < numRows - 1 && !maze.isWall(row + 1, col)) {
          return new Location(row + 1, col);
        }
        break;
      case LEFT:
        if (col > 0 && !maze.isWall(row, col - 1)) {
          return new Location(row, col - 1);
        } else if (col == 0) {
          // Tunnel wrap around to right side
          return new Location(row, numCols - 1);
        }
        break;
      case RIGHT:
        if (col < numCols - 1 && !maze.isWall(row, col + 1)) {
          return new Location(row, col + 1);
        } else if (col == numCols - 1) {
          // Tunnel wrap around to left side
          return new Location(row, 0);
        }
        break;
      default:
        // No action for default case
        break;
    }
    
    // If we get here, we couldn't move in the requested direction (wall or edge)
    return null;
  }

  /**
   * Calculates and returns the next cell location based on current direction.
   * Checks for walls and tries to find a valid direction if the current one is blocked.
   */
  private void calculateNextCellLocation() {
    // Get current location
    Location currentLoc = getCurrentLocation();
    if (currentLoc == null) {
      return;
    }

    // Special case: If we're in the ghost house area, navigate carefully
    Location homeLoc = getHomeLocation();
    boolean inGhostHouse = false;

    if (homeLoc != null) {
      // Check if we're in the ghost house area (typically center of maze)
      int centerRow = maze.getNumRows() / 2;
      int centerCol = maze.getNumColumns() / 2;
      inGhostHouse = Math.abs(currentLoc.row() - centerRow) <= 2 && 
                     Math.abs(currentLoc.col() - centerCol) <= 3;
    }

    if (inGhostHouse && getMode() != Mode.INACTIVE) {
      // For ghost house escape, prefer moving toward the exit (usually upward)
      // But avoid walls by checking each direction carefully
      Direction[] escapeOrder = { Direction.UP, Direction.LEFT, Direction.RIGHT, Direction.DOWN };
      
      for (Direction dir : escapeOrder) {
        Location loc = getNextLocation(currentLoc, dir);
        if (loc != null) {
          // Extra check: make sure the destination isn't a wall
          if (!maze.isWall(loc.row(), loc.col())) {
            nextDirection = dir;
            nextLocation = loc;
            logMovement("GHOST_HOUSE_ESCAPE_" + dir);
            return;
          }
        }
      }
    }

    // Try the current direction first
    Location nextLoc = getNextLocation(currentLoc, currentDirection);
    
    // If we can move in the current direction, keep going
    if (nextLoc != null && !maze.isWall(nextLoc.row(), nextLoc.col())) {
      nextDirection = currentDirection;
      nextLocation = nextLoc;
      logMovement("CONTINUE_CURRENT_DIRECTION");
      return;
    }
    
    // If we can't move in current direction, we need to choose a new one
    // This prevents infinite loops by ensuring we always find a valid path
    Direction[] directions = { Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT };
    Direction oppositeDir = getOppositeDirection(currentDirection);
    
    // First try non-reverse directions
    for (Direction dir : directions) {
      if (dir == oppositeDir || dir == currentDirection) {
        continue;
      }
      
      Location loc = getNextLocation(currentLoc, dir);
      if (loc != null && !maze.isWall(loc.row(), loc.col())) {
        nextDirection = dir;
        nextLocation = loc;
        logMovement("FOUND_ALTERNATIVE_DIRECTION: " + dir);
        return;
      }
    }
    
    // If no other direction works, allow reversing
    Location reverseLocation = getNextLocation(currentLoc, oppositeDir);
    if (reverseLocation != null && !maze.isWall(reverseLocation.row(), reverseLocation.col())) {
      nextDirection = oppositeDir;
      nextLocation = reverseLocation;
      logMovement("REVERSING_DIRECTION_AS_LAST_RESORT");
      return;
    }
    
    // Fallback: stay in current location but set a valid direction for next attempt
    nextLocation = currentLoc;
    // Try to find ANY valid direction to unstick
    for (Direction dir : directions) {
      Location loc = getNextLocation(currentLoc, dir);
      if (loc != null && !maze.isWall(loc.row(), loc.col())) {
        nextDirection = dir;
        currentDirection = dir; // Force direction change
        logMovement("EMERGENCY_UNSTICK_DIRECTION=" + dir);
        return;
      }
    }
    logMovement("NO_VALID_MOVES_AVAILABLE");
  }

  /**
   * Gets the opposite direction.
   */
  private Direction getOppositeDirection(Direction dir) {
    switch (dir) {
      case UP: return Direction.DOWN;
      case DOWN: return Direction.UP;
      case LEFT: return Direction.RIGHT;
      case RIGHT: return Direction.LEFT;
      default: return dir;
    }
  }

  /**
   * Calculates the next cell to move to based on the current mode and game state.
   * 
   * @param d The current game descriptor
   */
  public void calculateNextCell(final Descriptor d) {
    logMovement("CALCULATE_NEXT_CELL_START");

    // Check for INACTIVE mode - in which case we do nothing
    if (getMode() == Mode.INACTIVE) {
      logMovement("SKIPPING_CALC_INACTIVE");
      return;
    }

    // Get current location and validate
    Location currentLoc = getCurrentLocation();
    if (currentLoc == null) {
      logMovement("CURRENT_LOC_NULL");
      return;
    }

    // Special handling for FRIGHTENED mode
    if (getMode() == Mode.FRIGHTENED) {
      logMovement("HANDLING_FRIGHTENED_MODE");
      handleFrightenedMode();
      return;
    }

    // --- Get target based on mode ---
    Location targetLocation = getTargetLocation(d);

    // Fallback to scatter target if no target is available
    if (targetLocation == null) {
      targetLocation = getScatterTarget();
    }
    
    logMovement("TARGET_LOC=" + targetLocation);

    // --- For DEAD mode, head directly toward home ---
    if (getMode() == Mode.DEAD) {
      calculatePathToTarget(currentLoc, getHomeLocation());
      logMovement("DEAD_MODE_NEXT_DIR=" + nextDirection + ", NEXT_LOC=" + nextLocation);
      return;
    }

    // --- For other modes, choose best direction toward target ---
    calculatePathToTarget(currentLoc, targetLocation);
    logMovement("FOUND_PATH_TO_TARGET: " + nextDirection + ", NEXT_LOC=" + nextLocation);
  }

  /**
   * Calculates the best path to a target location.
   */
  private void calculatePathToTarget(Location currentLoc, Location targetLoc) {
    if (currentLoc == null || targetLoc == null) {
      // If we have no valid locations, just continue in current direction
      calculateNextCellLocation();
      return;
    }
    
    // Get all possible directions
    Direction oppositeDir = getOppositeDirection(currentDirection);
    Direction[] directions = { Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT };
    
    Direction bestDir = null;
    Location bestLoc = null;
    double shortestDistance = Double.MAX_VALUE;
    
    // First pass: try all directions except opposite
    for (Direction dir : directions) {
      // Skip the opposite direction unless it's the only option
      if (dir == oppositeDir) {
        continue;
      }
      
      Location nextLoc = getNextLocation(currentLoc, dir);
      if (nextLoc == null || maze.isWall(nextLoc.row(), nextLoc.col())) {
        // Wall or boundary in this direction
        continue;
      }
      
      
      // Calculate distance to target
      double distance = calculateDistanceTween(nextLoc, targetLoc);
      
      // Choose this direction if it's better
      if (distance < shortestDistance) {
        shortestDistance = distance;
        bestDir = dir;
        bestLoc = nextLoc;
      }
    }
    
    // If no valid direction found, allow reversal as last resort
    if (bestDir == null) {
      Location reverseLoc = getNextLocation(currentLoc, oppositeDir);
      if (reverseLoc != null && !maze.isWall(reverseLoc.row(), reverseLoc.col())) {
        bestDir = oppositeDir;
        bestLoc = reverseLoc;
        logMovement("FORCED_REVERSAL_TO_TARGET");
      }
    }
    
    // Set the result
    if (bestDir != null) {
      nextDirection = bestDir;
      nextLocation = bestLoc;
      logMovement("TARGET_PATH_DIR=" + bestDir + ", DISTANCE=" + shortestDistance);
    } else {
      // Extremely rare - no valid moves at all
      nextLocation = currentLoc;
      logMovement("NO_VALID_TARGET_PATH");
    }
  }

  /**
   * Helper method to handle ghost movement in frightened mode.
   * In this mode, ghosts move randomly at intersections.
   */
  private void handleFrightenedMode() {
    logMovement("FRIGHTENED_MODE_START");
    Location currentLoc = getCurrentLocation();
    if (currentLoc == null) {
      return;
    }

    // Get valid directions (no reversing)
    Direction oppositeDir = getOppositeDirection(currentDirection);
    Direction[] directions = { Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT };
    
    // Count valid moves and collect valid directions
    int validMoveCount = 0;
    Direction[] validDirections = new Direction[4];
    Location[] validLocations = new Location[4];
    
    for (int i = 0; i < directions.length; i++) {
      // Skip the opposite direction (no reversing)
      if (directions[i] == oppositeDir) {
        continue;
      }
      
      Location nextLoc = getNextLocation(currentLoc, directions[i]);
      if (nextLoc != null && !maze.isWall(nextLoc.row(), nextLoc.col())) {
        validDirections[validMoveCount] = directions[i];
        validLocations[validMoveCount] = nextLoc;
        validMoveCount++;
      }
    }
    
    logMovement("FRIGHTENED_VALID_MOVES=" + validMoveCount);
    
    // Handle case based on number of valid moves
    if (validMoveCount > 1) {
      // At an intersection with multiple options, choose randomly
      
      // With a slight preference for continuing in same direction if possible
      boolean canContinueSameDirection = false;
      int currentDirIndex = -1;
      
      // Find index of current direction in valid moves
      for (int i = 0; i < validMoveCount; i++) {
        if (validDirections[i] == currentDirection) {
          canContinueSameDirection = true;
          currentDirIndex = i;
          break;
        }
      }
      
      // 80% chance to continue in current direction if possible (reduce oscillation)
      if (canContinueSameDirection && rand.nextDouble() < 0.8) {
        nextDirection = validDirections[currentDirIndex];
        nextLocation = validLocations[currentDirIndex];
        logMovement("FRIGHTENED_CONTINUE_SAME_DIR");
        return;
      }
      
      // Otherwise, make a random choice
      int randomIndex = rand.nextInt(validMoveCount);
      nextDirection = validDirections[randomIndex];
      nextLocation = validLocations[randomIndex];
      logMovement("FRIGHTENED_RANDOM_CHOICE");
      
    } else if (validMoveCount == 1) {
      // Only one option, take it
      nextDirection = validDirections[0];
      nextLocation = validLocations[0];
      logMovement("FRIGHTENED_ONLY_ONE_DIR=" + nextDirection);
      
    } else {
      // No valid directions (rare) - allow reverse as last resort
      Location reverseLoc = getNextLocation(currentLoc, oppositeDir);
      if (reverseLoc != null && !maze.isWall(reverseLoc.row(), reverseLoc.col())) {
        nextDirection = oppositeDir;
        nextLocation = reverseLoc;
        logMovement("FRIGHTENED_REVERSING");
      } else {
        // Totally stuck - just stay put
        nextLocation = currentLoc;
        logMovement("FRIGHTENED_NOWHERE_TO_GO");
      }
    }
  }
  
  
  /**
   * Calculates the distance between two given locations using the distance formula.
   */
  protected double calculateDistanceTween(final Location loc1, final Location loc2) {
    // Calculate the Euclidean distance between locations
    double x1 = loc1.col();
    double y1 = loc1.row();
    double x2 = loc2.col();
    double y2 = loc2.row();
    return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
  }

  @Override
  public Direction getHomeDirection() {
    // Gets the direction toward the home location
    Location currentLoc = new Location((int) getRowExact(), (int) getColExact());
    Location homeLoc = getHomeLocation();
    
    // Calculate direction based on relative position
    int rowDiff = currentLoc.row() - homeLoc.row();
    int colDiff = currentLoc.col() - homeLoc.col();
    
    // Prioritize the larger difference
    if (Math.abs(rowDiff) > Math.abs(colDiff)) {
      return rowDiff > 0 ? Direction.UP : Direction.DOWN;
    } else {
      return colDiff > 0 ? Direction.LEFT : Direction.RIGHT;
    }
  }

  @Override
  public void update(final Descriptor description) {
    frameCounter++;
    logMovement("UPDATE_START");
    
    // If in INACTIVE mode, don't move
    if (getMode() == Mode.INACTIVE) {
      logMovement("UPDATE_SKIPPED_INACTIVE");
      return;
    }

    // Store previous location for change detection
    previousLocation = getCurrentLocation();

    // Get current state
    double increment = getCurrentIncrement();
    double currentRowExact = getRowExact();
    double currentColExact = getColExact();
    int currentRow = (int) Math.floor(currentRowExact);
    int currentCol = (int) Math.floor(currentColExact);
    
    // Calculate distance to cell center
    double rowCenter = currentRow + 0.5;
    double colCenter = currentCol + 0.5;
    double distToRowCenter = Math.abs(currentRowExact - rowCenter);
    double distToColCenter = Math.abs(currentColExact - colCenter);
    boolean atCellCenter = distToRowCenter < ERR && distToColCenter < ERR;
    
    // At cell center - recalculate movement decisions
    if (atCellCenter) {
      // Snap exactly to center for precision
      setRowExact(rowCenter);
      setColExact(colCenter);
      
      // Recalculate next move
      calculateNextCell(description);
      logMovement("AT_CENTER_RECALCULATING");

      // Change direction if needed
      if (nextDirection != null && nextDirection != currentDirection) {
        Direction oldDirection = currentDirection;
        currentDirection = nextDirection;
        logMovement("CHANGED_DIRECTION_FROM=" + oldDirection + "_TO=" + currentDirection);
      }
      
      pastCenter = true;
    }
    
    // Calculate new position based on current direction
    double newRowExact = currentRowExact;
    double newColExact = currentColExact;
    
    // Check if we can move in the current direction before applying movement
    Location testNextLoc = getNextLocation(currentLocation, currentDirection);
    boolean canMoveForward = (testNextLoc != null);
    
    if (!canMoveForward) {
      // We're blocked - need to find a new direction immediately
      logMovement("BLOCKED_FINDING_NEW_DIRECTION");
      
      // Force recalculation to find a valid direction
      calculateNextCell(description);
      
      if (nextDirection != null && nextDirection != currentDirection) {
        currentDirection = nextDirection;
        logMovement("CHANGED_TO_VALID_DIRECTION=" + currentDirection);
        // Try again with new direction
        testNextLoc = getNextLocation(currentLocation, currentDirection);
        canMoveForward = (testNextLoc != null);
      }
    }
    
    // Apply movement if we can move
    if (canMoveForward) {
      switch (currentDirection) {
        case UP:
          newRowExact = Math.max(0.5, currentRowExact - increment);
          break;
        case DOWN:
          newRowExact = Math.min(maze.getNumRows() - 1.5, currentRowExact + increment);
          break;
        case LEFT:
          newColExact = currentColExact - increment;
          // Handle tunnel wraparound
          if (newColExact < 0.5) {
            newColExact = maze.getNumColumns() - 0.5;
            logMovement("TUNNEL_WRAP_LEFT");
          }
          break;
        case RIGHT:
          newColExact = currentColExact + increment;
          // Handle tunnel wraparound
          if (newColExact >= maze.getNumColumns() - 0.5) {
            newColExact = 0.5;
            logMovement("TUNNEL_WRAP_RIGHT");
          }
          break;
        default:
          break;
      }
      
      // Final safety check - ensure we don't enter a wall
      int newCellRow = (int) Math.floor(newRowExact);
      int newCellCol = (int) Math.floor(newColExact);
      
      if (newCellRow >= 0 && newCellRow < maze.getNumRows() && 
          newCellCol >= 0 && newCellCol < maze.getNumColumns() &&
          !maze.isWall(newCellRow, newCellCol)) {
        // Safe to update position
        setRowExact(newRowExact);
        setColExact(newColExact);
        currentLocation = new Location(newCellRow, newCellCol);
        logMovement("MOVED_TO_POSITION");
      } else {
        // Would enter a wall - stay in place and find new direction
        logMovement("WOULD_ENTER_WALL_STAYING_PUT");
        calculateNextCell(description);
        if (nextDirection != null) {
          currentDirection = nextDirection;
          logMovement("FORCED_DIRECTION_CHANGE=" + currentDirection);
        }
      }
    } else {
      // Can't move at all - this shouldn't happen with proper direction calculation
      logMovement("COMPLETELY_STUCK");
    }
    
    // Check if we've moved to a new cell
    boolean movedToNewCell = !previousLocation.equals(currentLocation);
    if (movedToNewCell) {
      logMovement("MOVED_TO_NEW_CELL");
      pastCenter = false; // Reset for new cell
    }
    
    logMovement("UPDATE_END");
  }

  /**
   * Calculates the distance from the current position to the center of the current cell.
   *
   * @return A value that indicates: 
   * - Positive: we are approaching the center 
   * - Negative: we have passed the center 
   * - Near zero: we are at the center
   */
  protected double distanceToCenter() {
    // Get exact position
    double colPosition = getColExact();
    double rowPosition = getRowExact();
    
    final double cellCenterOffset = 0.5;

    // Default to 0 if no direction
    if (getCurrentDirection() == null) {
      return 0;
    }

    // Calculate offsets from the center of the current cell
    // The center of a cell is at (row+0.5, col+0.5)
    int cellRow = (int) rowPosition;
    int cellCol = (int) colPosition;
    double centerRow = cellRow + cellCenterOffset;
    double centerCol = cellCol + cellCenterOffset;

    // Calculate the distance based on current direction
    switch (getCurrentDirection()) {
      case LEFT:
        // When moving left, a positive value means we're to the right of center
        return colPosition - centerCol;

      case RIGHT:
        // When moving right, a positive value means we're to the left of center
        return centerCol - colPosition;

      case UP:
        // When moving up, a positive value means we're below the center
        return rowPosition - centerRow;

      case DOWN:
        // When moving down, a positive value means we're above the center
        return centerRow - rowPosition;
        
      default:
        // Default return if we somehow get here
        return 0;
    }
  }

  @Override
  public Direction getCurrentDirection() {
    if (currentDirection == null) {
      return homeDirection;
    }
    return currentDirection;
  }

  @Override
  public void setDirection(final Direction dir) {
    currentDirection = dir;
  }

  @Override
  public Location getCurrentLocation() {
    return currentLocation;
  }

  @Override
  public Location getHomeLocation() {
    return home;
  }

  @Override
  public void setColExact(final double c) {
    colExact = c;
  }

  @Override
  public void setRowExact(final double r) {
    rowExact = r;
  }

  @Override
  public double getColExact() {
    return colExact;
  }

  @Override
  public double getRowExact() {
    return rowExact;
  }

  @Override
  public double getBaseIncrement() {
    return baseIncrement;
  }

  @Override
  public double getCurrentIncrement() {
    return currentIncrement;
  }

  @Override
  public void setMode(final Mode gMode, final Descriptor description) {
    // Store previous mode to check for transitions
    Mode previousMode = currentMode;
    logMovement("SET_MODE_FROM_" + previousMode + "_TO_" + gMode);

    // Mode based speed adjustments - do this before setting mode
    final double frightenedSpeedFactor = 2.0 / 3.0;
    final double deadSpeedFactor = 2.0;
    
    if (gMode == Mode.FRIGHTENED) {
      currentIncrement = baseIncrement * frightenedSpeedFactor;
      logMovement("SET_SPEED_FRIGHTENED=" + currentIncrement);
    } else if (gMode == Mode.DEAD) {
      currentIncrement = baseIncrement * deadSpeedFactor;
      logMovement("SET_SPEED_DEAD=" + currentIncrement);
    } else {
      currentIncrement = baseIncrement;
      logMovement("SET_SPEED_NORMAL=" + currentIncrement);
    }

    // Set the new mode
    currentMode = gMode;

    // If ghost is transitioning from INACTIVE to active state
    // or if we're entering a new mode, always recalculate path
    if (previousMode == Mode.INACTIVE || previousMode != gMode) {
      // If transitioning out of INACTIVE, force an upward movement
      // This helps ghosts escape the ghost house
      if (previousMode == Mode.INACTIVE && getCurrentLocation().equals(getHomeLocation())) {
        currentDirection = Direction.UP;
        logMovement("TRANSITIONING_FROM_INACTIVE_TO_UP");
      }

      // Recalculate next cell with new direction/mode
      pastCenter = false;
      if (description != null) {
        calculateNextCell(description);
      }
    }
  }

  @Override
  public Mode getMode() {
    return currentMode;
  }

  /**
   * Gets the scatter target location for this ghost.
   * 
   * @return The scatter target location
   */
  protected Location getScatterTarget() {
    return scatterTarget;
  }
  
  /**
   * Returns the next cell the ghost is headed towards.
   * Used by test classes.
   *
   * @return The next location
   */
  public Location getNextCell() {
    return nextLocation;
  }

  /**
   * Gets the target location based on the current mode.
   * This method is implemented differently by each ghost.
   *
   * @param desc The game descriptor containing game state
   * @return The target location based on the ghost's targeting strategy
   */
  protected abstract Location getTargetLocation(final Descriptor desc);
  
  /**
   * Log ghost movement for debugging.
   * 
   * @param message The message to log
   */
  protected void logMovement(String message) {
    if (loggingInitialized) {
      String directionStr = currentDirection != null ? currentDirection.toString() : "NULL";
      String nextLocationStr = nextLocation != null ? nextLocation.toString() : "NULL";
      String modeStr = currentMode != null ? currentMode.toString() : "NULL";
      
      logWriter.println(frameCounter + "," 
          + ghostType + "," 
          + currentLocation + "," 
          + String.format("%.2f,%.2f", rowExact, colExact) + "," 
          + directionStr + "," 
          + nextLocationStr + "," 
          + modeStr + "," 
          + "0" + "," 
          + pastCenter + ","
          + message);
      logWriter.flush();
    }
  }
  
  /**
   * Finalize method to ensure that the log file is closed when the object is garbage collected.
   */
  @Override
  protected void finalize() throws Throwable {
    try {
      if (logWriter != null) {
        logWriter.close();
      }
    } finally {
      super.finalize();
    }
  }
}