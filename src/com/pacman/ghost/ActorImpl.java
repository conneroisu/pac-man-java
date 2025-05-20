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

  // Movement tracking variables
  private Location previousStuckLocation = null;
  private int stuckFrameCount = 0;
  
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

    // Reset location tracking
    previousStuckLocation = null;
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

    // Special case: If we're in the ghost house, try to move upward to escape
    Location homeLoc = getHomeLocation();
    boolean nearHome = false;

    if (homeLoc != null) {
      nearHome = (currentLoc.row() == homeLoc.row() && Math.abs(currentLoc.col() - homeLoc.col()) <= 2)
          || (currentLoc.col() == homeLoc.col() && Math.abs(currentLoc.row() - homeLoc.row()) <= 2);
    }

    if (nearHome && getMode() != Mode.INACTIVE) {
      // Try to move UP to escape ghost house
      Location upLoc = getNextLocation(currentLoc, Direction.UP);
      if (upLoc != null) {
        nextDirection = Direction.UP;
        nextLocation = upLoc;
        logMovement("GHOST_HOUSE_ESCAPE_UP");
        return;
      }
      
      // If we can't move up, try other directions to navigate to an exit
      Location leftLoc = getNextLocation(currentLoc, Direction.LEFT);
      Location rightLoc = getNextLocation(currentLoc, Direction.RIGHT);
      
      if (currentLoc.col() < 13 && rightLoc != null) {
        nextDirection = Direction.RIGHT;
        nextLocation = rightLoc;
        logMovement("GHOST_HOUSE_ESCAPE_RIGHT");
        return;
      } else if (currentLoc.col() > 13 && leftLoc != null) {
        nextDirection = Direction.LEFT;
        nextLocation = leftLoc;
        logMovement("GHOST_HOUSE_ESCAPE_LEFT");
        return;
      }
    }

    // Try the current direction first
    Location nextLoc = getNextLocation(currentLoc, currentDirection);
    
    // If we can move in the current direction, keep going
    if (nextLoc != null) {
      nextDirection = currentDirection;
      nextLocation = nextLoc;
      logMovement("CONTINUE_CURRENT_DIRECTION");
      return;
    }
    
    // If we can't move in current direction, look for alternatives
    // Try all directions except the opposite of current direction
    Direction[] directions = { Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT };
    Direction oppositeDir = getOppositeDirection(currentDirection);
    
    for (Direction dir : directions) {
      // Skip the opposite direction (no reversing)
      if (dir == oppositeDir) {
        continue;
      }
      
      Location loc = getNextLocation(currentLoc, dir);
      if (loc != null) {
        nextDirection = dir;
        nextLocation = loc;
        logMovement("FOUND_ALTERNATIVE_DIRECTION: " + dir);
        return;
      }
    }
    
    // If we're completely stuck, allow reversing as a last resort
    Location reverseLocation = getNextLocation(currentLoc, oppositeDir);
    if (reverseLocation != null) {
      nextDirection = oppositeDir;
      nextLocation = reverseLocation;
      logMovement("REVERSING_DIRECTION_AS_LAST_RESORT");
      return;
    }
    
    // If we couldn't find any valid direction including reverse (extremely rare in a valid maze),
    // we'll just stay in the current location until conditions change
    if (nextLocation == null) {
      nextLocation = currentLoc;
      logMovement("NO_VALID_MOVES_AVAILABLE");
    }
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
    
    // Skip calculation if we're past the center and still in the same cell
    if (pastCenter && nextLocation != null) {
      logMovement("SKIPPING_CALC_PAST_CENTER");
      return;
    }

    // Check for INACTIVE mode - in which case we do nothing
    if (getMode() == Mode.INACTIVE) {
      logMovement("SKIPPING_CALC_INACTIVE");
      return;
    }

    // Special handling for FRIGHTENED mode
    if (getMode() == Mode.FRIGHTENED) {
      logMovement("HANDLING_FRIGHTENED_MODE");
      handleFrightenedMode();
      return;
    }

    // Get current location and validate
    Location currentLoc = getCurrentLocation();
    if (currentLoc == null) {
      logMovement("CURRENT_LOC_NULL");
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
    
    // Get all possible next locations (excluding reversal)
    Direction oppositeDir = getOppositeDirection(currentDirection);
    Direction[] directions = { Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT };
    
    Direction bestDir = null;
    Location bestLoc = null;
    double shortestDistance = Double.MAX_VALUE;
    
    // Add tiny random factor to break ties consistently
    double[] distanceAdjustments = new double[4];
    final double RANDOMNESS_FACTOR = 0.02;
    for (int i = 0; i < 4; i++) {
      distanceAdjustments[i] = rand.nextDouble() * RANDOMNESS_FACTOR;
    }
    
    // Evaluate each direction
    for (int i = 0; i < directions.length; i++) {
      Direction dir = directions[i];
      
      // Skip the opposite direction (no reversing)
      if (dir == oppositeDir) {
        continue;
      }
      
      Location nextLoc = getNextLocation(currentLoc, dir);
      if (nextLoc == null) {
        // Wall or boundary in this direction
        continue;
      }
      
      // Calculate distance to target
      double distance = calculateDistanceTween(nextLoc, targetLoc);
      
      // Apply small adjustment to break ties
      distance -= distanceAdjustments[i];
      
      // Choose this direction if it's better than what we've seen so far
      // For UP direction, give it slightly higher priority when distances are equal
      if (distance < shortestDistance || 
          (Math.abs(distance - shortestDistance) < ERR && dir == Direction.UP && bestDir != Direction.UP)) {
        shortestDistance = distance;
        bestDir = dir;
        bestLoc = nextLoc;
      }
    }
    
    // If we found a valid direction, use it
    if (bestDir != null) {
      nextDirection = bestDir;
      nextLocation = bestLoc;
      logMovement("BEST_DIR=" + bestDir + ", DISTANCE=" + shortestDistance);
    } else {
      // If we couldn't find a valid direction (unlikely), just continue
      calculateNextCellLocation();
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
      if (nextLoc != null) {
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
      
      // 75% chance to continue in current direction if possible
      if (canContinueSameDirection && rand.nextDouble() < 0.75) {
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
      if (reverseLoc != null) {
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
    int currentRow = (int) currentRowExact;
    int currentCol = (int) currentColExact;
    
    // Calculate distance to cell center
    double rowCenter = currentRow + 0.5;
    double colCenter = currentCol + 0.5;
    double distToRowCenter = Math.abs(currentRowExact - rowCenter);
    double distToColCenter = Math.abs(currentColExact - colCenter);
    boolean atCellCenter = distToRowCenter < ERR && distToColCenter < ERR;
    
    // If we're at a cell center, check if we need to calculate a new target
    if (atCellCenter) {
      // Always recalculate next target cell at cell centers
      calculateNextCell(description);
      logMovement("AT_CENTER_RECALCULATING");

      // If we need to change direction and we're at a center, do it
      if (nextDirection != null && nextDirection != currentDirection) {
        // First snap exactly to center to avoid precision issues
        setRowExact(rowCenter);
        setColExact(colCenter);
        
        // Change direction
        Direction oldDirection = currentDirection;
        currentDirection = nextDirection;
        logMovement("CHANGED_DIRECTION_FROM=" + oldDirection + "_TO=" + currentDirection);
        
        // Reset pastCenter flag when changing direction
        pastCenter = false;
      } else {
        // Mark that we're at the center point (used for decision making)
        pastCenter = false;
      }
    } else {
      // Not at center, check if we've passed the center point in our current direction
      // This is important for determining when to make the next decision
      switch (currentDirection) {
        case UP:
          pastCenter = currentRowExact < rowCenter;
          break;
        case DOWN:
          pastCenter = currentRowExact > rowCenter;
          break;
        case LEFT:
          pastCenter = currentColExact < colCenter;
          break;
        case RIGHT:
          pastCenter = currentColExact > colCenter;
          break;
        default:
          break;
      }
    }
    
    // Determine next position based on direction with precise wall collision detection
    double newRowExact = currentRowExact;
    double newColExact = currentColExact;
    
    // Before attempting to move, check if the move is valid (not into a wall)
    boolean canMove = true;
    boolean willCrossCell = false;
    int nextCellRow = currentRow;
    int nextCellCol = currentCol;
    
    // Calculate next cell we might enter based on direction
    switch (currentDirection) {
      case UP:
        // Moving up means row decreases
        if (currentRowExact - increment < currentRow) {
          // Will cross to previous row
          willCrossCell = true;
          nextCellRow = currentRow - 1;
          nextCellCol = currentCol;
        }
        break;
      case DOWN:
        // Moving down means row increases
        if (currentRowExact + increment >= currentRow + 1) {
          // Will cross to next row
          willCrossCell = true;
          nextCellRow = currentRow + 1;
          nextCellCol = currentCol;
        }
        break;
      case LEFT:
        // Moving left means column decreases
        if (currentColExact - increment < currentCol) {
          // Will cross to previous column
          willCrossCell = true;
          nextCellRow = currentRow;
          nextCellCol = currentCol - 1;
          
          // Handle wraparound tunnel
          if (nextCellCol < 0) {
            // Wraparound to the right side of the maze
            nextCellCol = maze.getNumColumns() - 1;
          }
        }
        break;
      case RIGHT:
        // Moving right means column increases
        if (currentColExact + increment >= currentCol + 1) {
          // Will cross to next column
          willCrossCell = true;
          nextCellRow = currentRow;
          nextCellCol = currentCol + 1;
          
          // Handle wraparound tunnel
          if (nextCellCol >= maze.getNumColumns()) {
            // Wraparound to the left side of the maze
            nextCellCol = 0;
          }
        }
        break;
      default:
        // No movement for default
        canMove = false;
        break;
    }
    
    // Check if the next cell is a wall and adjust movement accordingly
    boolean hitWall = false;
    if (willCrossCell) {
      // Check if next cell is within maze bounds
      boolean inBounds = nextCellRow >= 0 && nextCellRow < maze.getNumRows() &&
                          nextCellCol >= 0 && nextCellCol < maze.getNumColumns();
      
      // If next cell is a wall, we can't move into it
      if (inBounds && maze.isWall(nextCellRow, nextCellCol)) {
        hitWall = true;
        
        // Calculate exact position at cell boundary to stop at
        switch (currentDirection) {
          case UP:
            // Stop at bottom edge of wall cell
            newRowExact = nextCellRow + 1.0;
            break;
          case DOWN:
            // Stop at top edge of wall cell
            newRowExact = nextCellRow;
            break;
          case LEFT:
            // Stop at right edge of wall cell
            newColExact = nextCellCol + 1.0;
            break;
          case RIGHT:
            // Stop at left edge of wall cell
            newColExact = nextCellCol;
            break;
          default:
            break;
        }
        
        logMovement("HIT_WALL_BOUNDARY_STOP");
        canMove = false;
      }
    }
    
    // If we can move, apply movement
    if (canMove) {
      switch (currentDirection) {
        case UP:
          newRowExact = currentRowExact - increment;
          break;
        case DOWN:
          newRowExact = currentRowExact + increment;
          break;
        case LEFT:
          newColExact = currentColExact - increment;
          // Handle tunnel wraparound
          if (newColExact < 0) {
            newColExact += maze.getNumColumns();
            logMovement("TUNNEL_WRAP_LEFT");
          }
          break;
        case RIGHT:
          newColExact = currentColExact + increment;
          // Handle tunnel wraparound
          if (newColExact >= maze.getNumColumns()) {
            newColExact -= maze.getNumColumns();
            logMovement("TUNNEL_WRAP_RIGHT");
          }
          break;
        default:
          break;
      }
    }
    
    // Update position
    setRowExact(newRowExact);
    setColExact(newColExact);
    
    // Update current location (integer grid position)
    // Java's (int) cast truncates toward zero, need to handle negative values explicitly
    int newRow = (int) Math.floor(newRowExact);
    int newCol = (int) Math.floor(newColExact);
    
    // Always create a new Location object to avoid reference issues
    currentLocation = new Location(newRow, newCol);
    
    // Check if we've moved to a new cell (for tracking purposes)
    boolean movedToNewCell = !previousLocation.equals(currentLocation);
    if (movedToNewCell) {
      logMovement("MOVED_TO_NEW_CELL");
    }
    
    // If we hit a wall, we need to choose a new direction
    if (hitWall) {
      logMovement("HIT_WALL_CHOOSING_NEW_DIRECTION");
      
      // Get available directions at current position
      Direction oppositeDir = getOppositeDirection(currentDirection);
      Direction[] directions = { Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT };
      
      // Find a valid direction that's not opposite (no immediate reversal)
      for (Direction dir : directions) {
        // Skip the opposite direction (no reversing)
        if (dir == oppositeDir) {
          continue;
        }
        
        // Skip current direction (we just hit a wall)
        if (dir == currentDirection) {
          continue;
        }
        
        // Check if this direction leads to a valid cell
        Location loc = getNextLocation(currentLocation, dir);
        if (loc != null) {
          // We found a valid direction to move
          currentDirection = dir;
          nextDirection = dir;
          
          // Make sure we're in a valid position for the new direction
          // For horizontal movements, ensure row is centered
          // For vertical movements, ensure column is centered
          if (dir == Direction.LEFT || dir == Direction.RIGHT) {
            setRowExact(newRow + 0.5);
          } else {
            setColExact(newCol + 0.5);
          }
          
          logMovement("NEW_DIRECTION_AFTER_WALL=" + dir);
          
          // Recalculate next cell with new direction
          calculateNextCell(description);
          break;
        }
      }
      
      // If we still couldn't find a valid direction, allow reversal as last resort
      if (currentDirection == null || currentDirection == oppositeDir) {
        Location reverseLocation = getNextLocation(currentLocation, oppositeDir);
        if (reverseLocation != null) {
          currentDirection = oppositeDir;
          nextDirection = oppositeDir;
          
          // Center position for reliability
          setRowExact(newRow + 0.5);
          setColExact(newCol + 0.5);
          
          logMovement("REVERSE_DIRECTION_AFTER_WALL");
          
          // Recalculate next cell with new direction
          calculateNextCell(description);
        }
      }
    }
    
    // If we haven't moved in a while, check if we're stuck
    if (previousStuckLocation != null && previousStuckLocation.equals(currentLocation)) {
      stuckFrameCount++;
      
      // If stuck for several frames, force recalculation and new direction
      if (stuckFrameCount > 5) {
        logMovement("STUCK_DETECTION");
        
        // Center position for more reliable decision making
        setRowExact(newRow + 0.5);
        setColExact(newCol + 0.5);
        
        // Force recalculation
        calculateNextCell(description);
        
        // Try all directions if still stuck
        Direction[] allDirections = { Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT };
        for (Direction dir : allDirections) {
          Location loc = getNextLocation(currentLocation, dir);
          if (loc != null) {
            currentDirection = dir;
            nextDirection = dir;
            logMovement("UNSTUCK_CHANGE_TO=" + dir);
            stuckFrameCount = 0;
            break;
          }
        }
      }
    } else {
      // Reset stuck counter if we moved
      stuckFrameCount = 0;
    }
    
    // Store current location for stuck detection
    previousStuckLocation = currentLocation;
    
    // Final safety check - ensure we're in valid maze coordinates
    if (getColExact() < 0) {
      setColExact(0);
    } else if (getColExact() >= maze.getNumColumns()) {
      setColExact(maze.getNumColumns() - 0.001);
    }
    
    if (getRowExact() < 0) {
      setRowExact(0);
    } else if (getRowExact() >= maze.getNumRows()) {
      setRowExact(maze.getNumRows() - 0.001);
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

    // Set the new mode
    currentMode = gMode;

    // If ghost is transitioning from INACTIVE to active state
    // or if we're entering a new mode, always recalculate path
    if (previousMode == Mode.INACTIVE || previousMode != gMode) {
      // Reset location tracking when mode changes
      previousStuckLocation = null;

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

    // Mode based speed adjustments
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
          + stuckFrameCount + "," 
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