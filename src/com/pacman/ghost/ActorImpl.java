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
  
  /** Number of frames to consider "stuck". */
  private static final int STUCK_THRESHOLD = 3;
  
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

  // Anti-stuck mechanism
  private int stuckFrameCount = 0;
  private Location previousStuckLocation = null;
  
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
    
    // Ensure ghosts are positioned exactly at the center of the cell
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
    this.currentLocation = new Location((int) rowExact, (int) colExact);

    // Initialize next direction and location
    this.nextDirection = homeDirection;

    // Calculate initial next location based on home direction
    Location nextCellLocation = calculateNextCellLocation();
    this.nextLocation = nextCellLocation;
    
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
    updateLocation(centeredRow, centeredCol);
    
    // Explicitly set the exact coordinates to ensure proper centering
    setRowExact(centeredRow);
    setColExact(centeredCol);
    
    pastCenter = false;
    nextDirection = homeDirection;
    nextLocation = calculateNextCellLocation();

    // Reset anti-stuck mechanism
    stuckFrameCount = 0;
    previousStuckLocation = null;
    logMovement("POST-RESET");
  }

  /**
   * Calculates and returns the next cell location based on current direction. Checks for walls and
   * only returns a valid next location.
   *
   * @return The next valid cell location
   */
  private Location calculateNextCellLocation() {
    // Get the current location
    int row = getCurrentLocation().row();
    int col = getCurrentLocation().col();
    int numRows = maze.getNumRows();
    int numCols = maze.getNumColumns();
    
    logMovement("CALC_NEXT_CELL_START");

    // Calculate potential next location based on current direction
    int newRow = row;
    int newCol = col;

    // If stuck in ghost house (near home location), use special handling
    Location homeLoc = getHomeLocation();
    boolean nearHome = false;

    if (homeLoc != null) {
      // Consider "near home" if in the same row or within 2 cells
      nearHome =
          (row == homeLoc.row() && Math.abs(col - homeLoc.col()) <= 2)
              || (col == homeLoc.col() && Math.abs(row - homeLoc.row()) <= 2);
    }
    
    if (nearHome) {
      logMovement("NEAR_HOME_TRUE");
    }

    // Ghost house escape logic - prioritize movement out of the ghost house
    if (nearHome && getMode() != Mode.INACTIVE) {
      logMovement("TRYING_ESCAPE_GHOST_HOUSE");
      
      // Ghost house exit strategy starts by moving UP, then LEFT/RIGHT based on position
      if (row > 0 && !maze.isWall(row - 1, col)) {
        // If we can move up, that's the preferred direction out of the house
        // This is especially important right after activation
        newRow = row - 1;
        currentDirection = Direction.UP;
        logMovement("GHOST_HOUSE_ESCAPE_UP");
        return new Location(newRow, col);
      } else if (row <= 11) {
        // If we're above row 12 (the center of the ghost house), 
        // we need to move horizontally to find an exit path
        
        // First check if we can move to the center of the ghost box to find paths up
        if (col < 13 && !maze.isWall(row, col + 1)) {
          newCol = col + 1;
          currentDirection = Direction.RIGHT; 
          logMovement("GHOST_HOUSE_ESCAPE_RIGHT_TO_CENTER");
          return new Location(row, newCol);
        } else if (col > 13 && !maze.isWall(row, col - 1)) {
          newCol = col - 1;
          currentDirection = Direction.LEFT;
          logMovement("GHOST_HOUSE_ESCAPE_LEFT_TO_CENTER");
          return new Location(row, newCol);
        }
        
        // If can't move to center, try any valid horizontal move
        if (col > 0 && !maze.isWall(row, col - 1)) {
          newCol = col - 1;
          currentDirection = Direction.LEFT;
          logMovement("GHOST_HOUSE_ESCAPE_LEFT");
          return new Location(row, newCol);
        } else if (col < numCols - 1 && !maze.isWall(row, col + 1)) {
          newCol = col + 1;
          currentDirection = Direction.RIGHT;
          logMovement("GHOST_HOUSE_ESCAPE_RIGHT");
          return new Location(row, newCol);
        }
      }
    }

    // Try current direction first
    switch (currentDirection) {
      case RIGHT:
        newCol = Math.min(col + 1, numCols - 1);
        // Check if this direction would hit a wall
        if (newCol != col && maze.isWall(row, newCol)) {
          // Try alternate directions
          if (row > 0 && !maze.isWall(row - 1, col)) {
            newRow = row - 1;
            newCol = col;
            currentDirection = Direction.UP;
          } else if (row < numRows - 1 && !maze.isWall(row + 1, col)) {
            newRow = row + 1;
            newCol = col;
            currentDirection = Direction.DOWN;
          } else {
            // Stuck - stay in place
            newRow = row;
            newCol = col;
          }
        }
        break;

      case LEFT:
        newCol = Math.max(col - 1, 0);
        // Check if this direction would hit a wall
        if (newCol != col && maze.isWall(row, newCol)) {
          // Try alternate directions
          if (row > 0 && !maze.isWall(row - 1, col)) {
            newRow = row - 1;
            newCol = col;
            currentDirection = Direction.UP;
          } else if (row < numRows - 1 && !maze.isWall(row + 1, col)) {
            newRow = row + 1;
            newCol = col;
            currentDirection = Direction.DOWN;
          } else {
            // Stuck - stay in place
            newRow = row;
            newCol = col;
          }
        }
        break;

      case UP:
        newRow = Math.max(row - 1, 0);
        // Check if this direction would hit a wall
        if (newRow != row && maze.isWall(newRow, col)) {
          // Try alternate directions
          if (col > 0 && !maze.isWall(row, col - 1)) {
            newRow = row;
            newCol = col - 1;
            currentDirection = Direction.LEFT;
          } else if (col < numCols - 1 && !maze.isWall(row, col + 1)) {
            newRow = row;
            newCol = col + 1;
            currentDirection = Direction.RIGHT;
          } else {
            // Stuck - stay in place
            newRow = row;
            newCol = col;
          }
        }
        break;

      case DOWN:
        newRow = Math.min(row + 1, numRows - 1);
        // Check if this direction would hit a wall
        if (newRow != row && maze.isWall(newRow, col)) {
          // Try alternate directions
          if (col > 0 && !maze.isWall(row, col - 1)) {
            newRow = row;
            newCol = col - 1;
            currentDirection = Direction.LEFT;
          } else if (col < numCols - 1 && !maze.isWall(row, col + 1)) {
            newRow = row;
            newCol = col + 1;
            currentDirection = Direction.RIGHT;
          } else {
            // Stuck - stay in place
            newRow = row;
            newCol = col;
          }
        }
        break;
      default:
        // No action needed for default case
        break;
    }

    // Return the next location
    Location result = new Location(newRow, newCol);
    logMovement("CALC_NEXT_CELL_RESULT:" + result);
    return result;
  }

  /**
   * Returns the valid neighboring cells around a target location. Returns locations as an array
   * with null values for invalid moves (walls, out of bounds, or opposite of current direction).
   *
   * @param targetLocation The location for which we want to find valid neighboring cells
   * @return Array of neighboring locations (null values for invalid moves)
   */
  private Location[] getNeighbors(final Location targetLocation) {
    Location[] neighbors = new Location[4];
    int numRows = maze.getNumRows();
    int numCols = maze.getNumColumns();

    // Safety check for invalid location
    if (targetLocation == null
        || targetLocation.row() < 0
        || targetLocation.row() >= numRows
        || targetLocation.col() < 0
        || targetLocation.col() >= numCols) {
      return neighbors; // Return array of nulls if targetLocation is invalid
    }

    // Check UP neighbor
    if (targetLocation.row() - 1 >= 0
        && !maze.isWall(targetLocation.row() - 1, targetLocation.col())
        && getCurrentDirection() != Direction.DOWN) {
      neighbors[0] = new Location(targetLocation.row() - 1, targetLocation.col());
    } else {
      neighbors[0] = null;
    }

    // Check DOWN neighbor
    if (targetLocation.row() + 1 < numRows
        && !maze.isWall(targetLocation.row() + 1, targetLocation.col())
        && getCurrentDirection() != Direction.UP) {
      neighbors[1] = new Location(targetLocation.row() + 1, targetLocation.col());
    } else {
      neighbors[1] = null;
    }

    // Check LEFT neighbor
    if (targetLocation.col() - 1 >= 0
        && !maze.isWall(targetLocation.row(), targetLocation.col() - 1)
        && getCurrentDirection() != Direction.RIGHT) {
      neighbors[2] = new Location(targetLocation.row(), targetLocation.col() - 1);
    } else {
      neighbors[2] = null;
    }

    // Check RIGHT neighbor
    if (targetLocation.col() + 1 < numCols
        && !maze.isWall(targetLocation.row(), targetLocation.col() + 1)
        && getCurrentDirection() != Direction.LEFT) {
      neighbors[3] = new Location(targetLocation.row(), targetLocation.col() + 1);
    } else {
      neighbors[3] = null;
    }

    return neighbors;
  }

  /**
   * Calculates the next cell to move to based on the current mode and game state.
   * 
   * @param d The current game descriptor
   */
  public void calculateNextCell(final Descriptor d) {
    logMovement("CALCULATE_NEXT_CELL_START");
    
    // Skip calculation if we're past the center and still in the same cell
    // (This prevents oscillation/jittering)
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

    // Define direction priority array (UP has highest priority for ties)
    Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    // Get current location and validate
    Location currentLoc = getCurrentLocation();
    if (currentLoc == null) {
      logMovement("CURRENT_LOC_NULL");
      return;
    }

    // --- STEP 1: Determine valid directions ---
    int row = currentLoc.row();
    int col = currentLoc.col();
    int numRows = maze.getNumRows();
    int numCols = maze.getNumColumns();

    // Check all four directions (avoiding walls and no reversing)
    boolean[] canMove = new boolean[4]; // UP, DOWN, LEFT, RIGHT

    // UP direction
    canMove[0] = row > 0 && !maze.isWall(row - 1, col) && currentDirection != Direction.DOWN;

    // DOWN direction
    canMove[1] =
        row < numRows - 1 && !maze.isWall(row + 1, col) && currentDirection != Direction.UP;

    // LEFT direction
    canMove[2] = col > 0 && !maze.isWall(row, col - 1) && currentDirection != Direction.RIGHT;

    // RIGHT direction
    canMove[3] =
        col < numCols - 1 && !maze.isWall(row, col + 1) && currentDirection != Direction.LEFT;
        
    // Log valid directions
    logMovement("VALID_DIRS: UP=" + canMove[0] + ", DOWN=" + canMove[1] + 
                ", LEFT=" + canMove[2] + ", RIGHT=" + canMove[3]);

    // --- STEP 2: Create neighbor locations for valid moves ---
    Location[] neighbors = new Location[4];
    int validMoveCount = 0;

    for (int i = 0; i < 4; i++) {
      if (canMove[i]) {
        int newRow = row;
        int newCol = col;

        switch (i) {
          case 0:
            newRow--;
            break; // UP
          case 1:
            newRow++;
            break; // DOWN
          case 2:
            newCol--;
            break; // LEFT
          case 3:
            newCol++;
            break; // RIGHT
          default:
            // No action needed for default case
            break;
        }

        neighbors[i] = new Location(newRow, newCol);
        validMoveCount++;
      }
    }
    
    logMovement("VALID_MOVE_COUNT=" + validMoveCount);

    // --- STEP 3: Get target based on mode ---
    Location targetLocation = getTargetLocation(d);

    // Fallback to scatter target if no target is available
    if (targetLocation == null) {
      targetLocation = getScatterTarget();
    }
    
    logMovement("TARGET_LOC=" + targetLocation);

    // --- STEP 4: Select best direction based on distance to target ---

    // If we're in DEAD mode and there's a valid direction toward home, take it
    if (getMode() == Mode.DEAD && validMoveCount > 0) {
      Direction homeDir = getDirectionToTarget(currentLoc, getHomeLocation());

      // Convert direction to index
      int homeDirIndex = -1;
      for (int i = 0; i < directions.length; i++) {
        if (directions[i] == homeDir) {
          homeDirIndex = i;
          break;
        }
      }

      // If this direction is valid, use it
      if (homeDirIndex >= 0 && canMove[homeDirIndex]) {
        nextDirection = directions[homeDirIndex];
        nextLocation = neighbors[homeDirIndex];
        logMovement("DEAD_MODE_NEXT_DIR=" + nextDirection + ", NEXT_LOC=" + nextLocation);
        return;
      }
    }

    // For other modes, choose the direction that minimizes distance to target
    int bestDirectionIndex = -1;
    double shortestDistance = Double.MAX_VALUE;
    
    // Add a small random factor for each direction to prevent identical distances
    // causing predictable patterns (especially around ghost house)
    // This helps break oscillation patterns at decision points
    double[] distanceAdjustments = new double[4];
    
    // Add a tiny bit of randomness to the distance calculations to break ties
    // in a way that doesn't significantly change behavior but prevents fixed patterns
    final double RANDOMNESS_FACTOR = 0.02; // Just enough to break ties
    for (int i = 0; i < 4; i++) {
        distanceAdjustments[i] = rand.nextDouble() * RANDOMNESS_FACTOR;
    }

    // Evaluate each direction
    for (int i = 0; i < 4; i++) {
      if (!canMove[i] || neighbors[i] == null) {
        continue;
      }

      // Calculate the distance to target for this direction
      double distance = calculateDistanceTween(neighbors[i], targetLocation);
      
      // Apply the tiny adjustment factor to break ties randomly
      distance -= distanceAdjustments[i];

      // Take this direction if it's better, or equal but higher priority
      if (distance < shortestDistance
          || (Math.abs(distance - shortestDistance) < ERR && i < bestDirectionIndex)) {
        shortestDistance = distance;
        bestDirectionIndex = i;
      }
    }
    
    logMovement("BEST_DIR_INDEX=" + bestDirectionIndex + ", SHORTEST_DIST=" + shortestDistance);

    // --- STEP 5: Choose final direction and next location ---

    // If we found a valid direction, use it
    if (bestDirectionIndex >= 0 && bestDirectionIndex < 4) {
      nextDirection = directions[bestDirectionIndex];
      nextLocation = neighbors[bestDirectionIndex];
      logMovement("FOUND_VALID_DIR: " + nextDirection + ", NEXT_LOC=" + nextLocation);
      return;
    }

    // If we can't find a better move, try to continue in current direction
    boolean canContinueForward = false;
    Location nextForward = null;

    switch (currentDirection) {
      case UP:
        if (row > 0 && !maze.isWall(row - 1, col)) {
          canContinueForward = true;
          nextForward = new Location(row - 1, col);
        }
        break;
      case DOWN:
        if (row < numRows - 1 && !maze.isWall(row + 1, col)) {
          canContinueForward = true;
          nextForward = new Location(row + 1, col);
        }
        break;
      case LEFT:
        if (col > 0 && !maze.isWall(row, col - 1)) {
          canContinueForward = true;
          nextForward = new Location(row, col - 1);
        }
        break;
      case RIGHT:
        if (col < numCols - 1 && !maze.isWall(row, col + 1)) {
          canContinueForward = true;
          nextForward = new Location(row, col + 1);
        }
        break;
      default:
        // No action needed for default case
        break;
    }

    // If we can continue forward, do so
    if (canContinueForward) {
      nextDirection = currentDirection;
      nextLocation = nextForward;
      logMovement("CONTINUING_FORWARD: " + nextDirection + ", NEXT_LOC=" + nextLocation);
      return;
    }

    // Last resort: find any valid direction
    for (int i = 0; i < 4; i++) {
      if (canMove[i]) {
        nextDirection = directions[i];
        nextLocation = neighbors[i];
        logMovement("LAST_RESORT_DIR: " + nextDirection + ", NEXT_LOC=" + nextLocation);
        return;
      }
    }

    // If we get here, we're completely stuck - just stay in place
    nextLocation = currentLoc;
    logMovement("COMPLETELY_STUCK");
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

    // Get maze dimensions
    int numRows = maze.getNumRows();
    int numCols = maze.getNumColumns();
    int row = currentLoc.row();
    int col = currentLoc.col();

    // Get valid directions (no reversing)
    Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    boolean[] canMove = new boolean[4];
    int validMoveCount = 0;

    // Check which directions are valid (not a wall, not reversing)
    canMove[0] = row > 0 && !maze.isWall(row - 1, col) && currentDirection != Direction.DOWN;
    canMove[1] =
        row < numRows - 1 && !maze.isWall(row + 1, col) && currentDirection != Direction.UP;
    canMove[2] = col > 0 && !maze.isWall(row, col - 1) && currentDirection != Direction.RIGHT;
    canMove[3] =
        col < numCols - 1 && !maze.isWall(row, col + 1) && currentDirection != Direction.LEFT;

    // Count valid moves
    for (boolean move : canMove) {
      if (move) {
        validMoveCount++;
      }
    }
    
    logMovement("FRIGHTENED_VALID_MOVES=" + validMoveCount);

    // If we're at an intersection (>1 possible direction)
    if (validMoveCount > 1) {
      // Create a list of valid directions to choose from
      Direction[] validDirections = new Direction[validMoveCount];
      Location[] validLocations = new Location[validMoveCount];
      int index = 0;

      for (int i = 0; i < 4; i++) {
        if (canMove[i]) {
          validDirections[index] = directions[i];

          int newRow = row;
          int newCol = col;

          switch (directions[i]) {
            case UP:
              newRow--;
              break;
            case DOWN:
              newRow++;
              break;
            case LEFT:
              newCol--;
              break;
            case RIGHT:
              newCol++;
              break;
            default:
              // No action needed for default case
              break;
          }

          validLocations[index] = new Location(newRow, newCol);
          index++;
        }
      }

      // Choose random direction, but with a bias against immediately reversing
      // This prevents most oscillation
      int randomIndex;

      // If we just changed direction, slightly prefer continuing in same direction
      if (stuckFrameCount < 2 && previousStuckLocation != null) {
        // 75% chance to continue in current direction if possible
        final double preferCurrentDirectionProbability = 0.75;
        boolean canContinueSameDirection = false;
        int currentDirIndex = -1;

        // Find index of current direction
        for (int i = 0; i < validMoveCount; i++) {
          if (validDirections[i] == currentDirection) {
            canContinueSameDirection = true;
            currentDirIndex = i;
            break;
          }
        }

        if (canContinueSameDirection && rand.nextDouble() < preferCurrentDirectionProbability) {
          randomIndex = currentDirIndex;
          logMovement("FRIGHTENED_CONTINUE_SAME_DIR");
        } else {
          // Otherwise random choice
          randomIndex = rand.nextInt(validMoveCount);
          logMovement("FRIGHTENED_RANDOM_CHOICE1");
        }
      } else {
        // Normal random choice
        randomIndex = rand.nextInt(validMoveCount);
        logMovement("FRIGHTENED_RANDOM_CHOICE2");
      }

      // Set the next direction and location
      nextDirection = validDirections[randomIndex];
      nextLocation = validLocations[randomIndex];
      logMovement("FRIGHTENED_NEXT_DIR=" + nextDirection + ", NEXT_LOC=" + nextLocation);
    } else if (validMoveCount == 1) {
      // Only one possible direction, take it
      for (int i = 0; i < 4; i++) {
        if (canMove[i]) {
          nextDirection = directions[i];
          int newRow = row;
          int newCol = col;

          switch (directions[i]) {
            case UP:
              newRow--;
              break;
            case DOWN:
              newRow++;
              break;
            case LEFT:
              newCol--;
              break;
            case RIGHT:
              newCol++;
              break;
            default:
              // No action needed for default case
              break;
          }

          nextLocation = new Location(newRow, newCol);
          logMovement("FRIGHTENED_ONLY_ONE_DIR=" + nextDirection);
          return;
        }
      }
    } else {
      // No valid direction (should rarely happen)
      // Try to continue in current direction if possible
      boolean canContinue = false;

      switch (currentDirection) {
        case UP:
          if (row > 0 && !maze.isWall(row - 1, col)) {
            nextLocation = new Location(row - 1, col);
            canContinue = true;
          }
          break;
        case DOWN:
          if (row < numRows - 1 && !maze.isWall(row + 1, col)) {
            nextLocation = new Location(row + 1, col);
            canContinue = true;
          }
          break;
        case LEFT:
          if (col > 0 && !maze.isWall(row, col - 1)) {
            nextLocation = new Location(row, col - 1);
            canContinue = true;
          }
          break;
        case RIGHT:
          if (col < numCols - 1 && !maze.isWall(row, col + 1)) {
            nextLocation = new Location(row, col + 1);
            canContinue = true;
          }
          break;
        default:
          // No action needed for default case
          break;
      }

      if (canContinue) {
        nextDirection = currentDirection;
        logMovement("FRIGHTENED_CONTINUING_CURRENT_DIR");
      } else {
        // Nowhere to go, stay in place
        nextLocation = currentLoc;
        logMovement("FRIGHTENED_NOWHERE_TO_GO");
      }
    }
  }

  /**
   * Helper method to determine the direction toward a target.
   * 
   * @param current The current location
   * @param target The target location
   * @return The direction to move toward the target
   */
  private Direction getDirectionToTarget(final Location current, final Location target) {
    if (current == null || target == null) {
      return currentDirection;
    }

    // Calculate Manhattan distance in each direction
    int rowDiff = target.row() - current.row();
    int colDiff = target.col() - current.col();

    // Prioritize the largest difference
    if (Math.abs(rowDiff) > Math.abs(colDiff)) {
      // Vertical movement is more important
      return rowDiff < 0 ? Direction.UP : Direction.DOWN;
    } else {
      // Horizontal movement is more important
      return colDiff < 0 ? Direction.LEFT : Direction.RIGHT;
    }
  }

  /**
   * Calculates the distance between two given locations using the distance formula
   * \sqrt{(x_{1}-x_{2})^{2}+(y_{1}-y_{2})^{2}}.
   *
   * @param loc1 The first location
   * @param loc2 The second location
   * @return The Euclidean distance between the two locations
   */
  protected double calculateDistanceTween(final Location loc1, final Location loc2) {
    // Calculate the distance between a Location and another Location
    double x1 = loc1.col();
    double y1 = loc1.row();
    double x2 = loc2.col();
    double y2 = loc2.row();
    return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
  }

  @Override
  public Direction getHomeDirection() {
    // Gets the direction of the ghost's home location from the current location
    Location currentLoc = new Location((int) getRowExact(), (int) getColExact());
    Location homeLoc = getHomeLocation();
    if (currentLoc.row() > homeLoc.row()) {
      return UP;
    } else if (currentLoc.row() < homeLoc.row()) {
      return DOWN;
    } else if (currentLoc.col() > homeLoc.col()) {
      return LEFT;
    } else {
      return RIGHT;
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

    // Calculate next cell if not already done
    if (nextLocation == null) {
      logMovement("UPDATE_CALCULATING_NEXT_CELL");
      calculateNextCell(description);
    }

    // Get current state
    double increment = getCurrentIncrement();
    double currentColumnExact = getColExact();
    double currentRowExact = getRowExact();
    int numRows = maze.getNumRows();
    int numCols = maze.getNumColumns();
    int currentRow = (int) currentRowExact;
    int currentCol = (int) currentColumnExact;

    // Get distance to center of current cell
    double distanceToCenter = distanceToCenter();
    boolean isNearCenter = Math.abs(distanceToCenter) < ERR;

    // Flags for next decision
    boolean atCenterPoint = false;
    boolean hitWall = false;

    // Special case: if we're at the center point or extremely close
    if (isNearCenter) {
      atCenterPoint = true;
      logMovement("AT_CENTER_POINT");
    }

    // // PHASE 1: ANTI-STUCK MECHANISM
    // // Check if the ghost is stuck in the same location for multiple frames
    // if (previousStuckLocation != null && previousStuckLocation.equals(getCurrentLocation())) {
    //   stuckFrameCount++;
    //   logMovement("STUCK_COUNT_INCREMENT=" + stuckFrameCount);
    //
    //   // If stuck for too many frames, force a direction change
    //   if (stuckFrameCount >= STUCK_THRESHOLD) {
    //     logMovement("STUCK_THRESHOLD_REACHED");
    //     // Force ghost to choose a new direction (anything except the current one)
    //     Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    //     Direction originalDirection = currentDirection;
    //
    //     // Shuffle the directions array to introduce randomness
    //     for (int i = 0; i < directions.length; i++) {
    //       int j = rand.nextInt(directions.length);
    //       Direction temp = directions[i];
    //       directions[i] = directions[j];
    //       directions[j] = temp;
    //     }
    //
    //     // First try directions that aren't opposite to current, to avoid ping-ponging
    //     for (Direction dir : directions) {
    //       // Skip opposite direction as the first choice
    //       if ((dir == Direction.UP && originalDirection == Direction.DOWN) ||
    //           (dir == Direction.DOWN && originalDirection == Direction.UP) ||
    //           (dir == Direction.LEFT && originalDirection == Direction.RIGHT) ||
    //           (dir == Direction.RIGHT && originalDirection == Direction.LEFT)) {
    //         continue;
    //       }
    //
    //       boolean canMove = false;
    //       switch (dir) {
    //         case UP:
    //           canMove = currentRow > 0 && !maze.isWall(currentRow - 1, currentCol);
    //           break;
    //         case DOWN:
    //           canMove = currentRow < numRows - 1 && !maze.isWall(currentRow + 1, currentCol);
    //           break;
    //         case LEFT:
    //           canMove = currentCol > 0 && !maze.isWall(currentRow, currentCol - 1);
    //           break;
    //         case RIGHT:
    //           canMove = currentCol < numCols - 1 && !maze.isWall(currentRow, currentCol + 1);
    //           break;
    //         default:
    //           // No action needed for default case
    //           break;
    //       }
    //
    //       if (canMove) {
    //         // Force new direction
    //         currentDirection = dir;
    //         logMovement("ANTI_STUCK_FORCING_DIR=" + dir);
    //
    //         // Reset stuck counter and pastCenter flag
    //         stuckFrameCount = 0;
    //         pastCenter = false;
    //
    //         // Recalculate next cell with new direction
    //         calculateNextCell(description);
    //         break;
    //       }
    //     }
    //
    //     // If we couldn't find a non-opposite direction, try any direction including opposite
    //     if (stuckFrameCount >= STUCK_THRESHOLD) {
    //       logMovement("TRYING_ANY_DIRECTION");
    //       for (Direction dir : directions) {
    //         boolean canMove = false;
    //         switch (dir) {
    //           case UP:
    //             canMove = currentRow > 0 && !maze.isWall(currentRow - 1, currentCol);
    //             break;
    //           case DOWN:
    //             canMove = currentRow < numRows - 1 && !maze.isWall(currentRow + 1, currentCol);
    //             break;
    //           case LEFT:
    //             canMove = currentCol > 0 && !maze.isWall(currentRow, currentCol - 1);
    //             break;
    //           case RIGHT:
    //             canMove = currentCol < numCols - 1 && !maze.isWall(currentRow, currentCol + 1);
    //             break;
    //           default:
    //             // No action needed for default case
    //             break;
    //         }
    //
    //         if (canMove) {
    //           // Force new direction
    //           currentDirection = dir;
    //           logMovement("ANTI_STUCK_DESPERATE_DIR=" + dir);
    //
    //           // Reset stuck counter and pastCenter flag
    //           stuckFrameCount = 0;
    //           pastCenter = false;
    //
    //           // Recalculate next cell with new direction
    //           calculateNextCell(description);
    //           break;
    //         }
    //       }
    //     }
    //   }
    // } else {
    //   // Reset stuck counter if we've moved
    //   stuckFrameCount = 0;
    //   previousStuckLocation = getCurrentLocation();
    //   logMovement("RESET_STUCK_COUNTER");
    // }

    // First, prepare for movement based on current direction
    switch (getCurrentDirection()) {
      case LEFT:
        // Check if we might hit a wall
        int nextCol = (int) (currentColumnExact - increment);
        if (nextCol != currentCol && nextCol >= 0 && maze.isWall(currentRow, nextCol)) {
          // We'd hit a wall - need to stop at cell center
          if (distanceToCenter > 0 && distanceToCenter < increment) {
            // Approach center but don't go past it
            increment = distanceToCenter;
            atCenterPoint = true;
          } else {
            // Already at or past center - stop
            increment = 0;
            atCenterPoint = isNearCenter;
          }
          hitWall = true;
          pastCenter = false; // Reset pastCenter when hitting a wall to allow recalculation
          logMovement("HIT_WALL_LEFT");
        }

        // Apply movement
        currentColumnExact -= increment;

        // Handle tunnel wraparound
        if (currentColumnExact < 0) {
          currentColumnExact += numCols;
          logMovement("TUNNEL_WRAP_LEFT");
        }
        break;

      case RIGHT:
        // Check if we might hit a wall
        nextCol = (int) (currentColumnExact + increment);
        if (nextCol != currentCol && nextCol < numCols && maze.isWall(currentRow, nextCol)) {
          // We'd hit a wall - need to stop at cell center
          if (distanceToCenter > 0 && distanceToCenter < increment) {
            // Approach center but don't go past it
            increment = distanceToCenter;
            atCenterPoint = true;
          } else {
            // Already at or past center - stop
            increment = 0;
            atCenterPoint = isNearCenter;
          }
          hitWall = true;
          pastCenter = false; // Reset pastCenter when hitting a wall to allow recalculation
          logMovement("HIT_WALL_RIGHT");
        }

        // Apply movement
        currentColumnExact += increment;

        // Handle tunnel wraparound
        if (currentColumnExact >= numCols) {
          currentColumnExact -= numCols;
          logMovement("TUNNEL_WRAP_RIGHT");
        }
        break;

      case UP:
        // Check if we might hit a wall
        int nextRow = (int) (currentRowExact - increment);
        if (nextRow != currentRow && nextRow >= 0 && maze.isWall(nextRow, currentCol)) {
          // We'd hit a wall - need to stop at cell center
          if (distanceToCenter > 0 && distanceToCenter < increment) {
            // Approach center but don't go past it
            increment = distanceToCenter;
            atCenterPoint = true;
          } else {
            // Already at or past center - stop
            increment = 0;
            atCenterPoint = isNearCenter;
          }
          hitWall = true;
          pastCenter = false; // Reset pastCenter when hitting a wall to allow recalculation
          logMovement("HIT_WALL_UP");
        }

        // Apply movement
        currentRowExact -= increment;

        // Handle edge case
        if (currentRowExact < 0) {
          currentRowExact = 0;
          logMovement("EDGE_CASE_UP");
        }
        break;

      case DOWN:
        // Check if we might hit a wall
        nextRow = (int) (currentRowExact + increment);
        if (nextRow != currentRow && nextRow < numRows && maze.isWall(nextRow, currentCol)) {
          // We'd hit a wall - need to stop at cell center
          if (distanceToCenter > 0 && distanceToCenter < increment) {
            // Approach center but don't go past it
            increment = distanceToCenter;
            atCenterPoint = true;
          } else {
            // Already at or past center - stop
            increment = 0;
            atCenterPoint = isNearCenter;
          }
          hitWall = true;
          pastCenter = false; // Reset pastCenter when hitting a wall to allow recalculation
          logMovement("HIT_WALL_DOWN");
        }

        // Apply movement
        currentRowExact += increment;

        // Handle edge case
        if (currentRowExact >= numRows) {
          currentRowExact = numRows - 1;
          logMovement("EDGE_CASE_DOWN");
        }
        break;
        
      default:
        // No action needed for default case
        break;
    }

    // Update position and get new location
    updateLocation(currentRowExact, currentColumnExact);
    Location newLocation = getCurrentLocation();
    boolean movedToNewCell = !previousLocation.equals(newLocation);
    
    if (movedToNewCell) {
      logMovement("MOVED_TO_NEW_CELL");
    }

    // PHASE 3: DIRECTION DECISIONS & RECALCULATION
    // We should make a direction change if:
    // 1. We're at a cell center point (or very close)
    // 2. We have a next direction already calculated
    // 3. The next direction isn't blocked by a wall
    // 4. We're not in the middle of an oscillation
    if (atCenterPoint && nextDirection != null) {
      logMovement("DECIDING_DIRECTION_CHANGE");
      // Verify the next direction isn't immediately blocked
      boolean canChangeDirection = false;

      switch (nextDirection) {
        case UP:
          canChangeDirection =
              newLocation.row() > 0 && !maze.isWall(newLocation.row() - 1, newLocation.col());
          break;
        case DOWN:
          canChangeDirection =
              newLocation.row() < numRows - 1
                  && !maze.isWall(newLocation.row() + 1, newLocation.col());
          break;
        case LEFT:
          canChangeDirection =
              newLocation.col() > 0 && !maze.isWall(newLocation.row(), newLocation.col() - 1);
          break;
        case RIGHT:
          canChangeDirection =
              newLocation.col() < numCols - 1
                  && !maze.isWall(newLocation.row(), newLocation.col() + 1);
          break;
        default:
          // No action needed for default case
          break;
      }

      // Make the change if it's valid
      if (canChangeDirection) {
        logMovement("CAN_CHANGE_DIRECTION=" + nextDirection);
        // Completely new approach to prevent direction oscillation
        // that considers the ghost's current and historical movement
        
        // First count available directions to determine intersection type
        int availableDirections = 0;
        if (newLocation.row() > 0 && !maze.isWall(newLocation.row() - 1, newLocation.col())) {
          availableDirections++;
        }
        if (newLocation.row() < maze.getNumRows() - 1 && 
            !maze.isWall(newLocation.row() + 1, newLocation.col())) {
          availableDirections++;
        }
        if (newLocation.col() > 0 && !maze.isWall(newLocation.row(), newLocation.col() - 1)) {
          availableDirections++;
        }
        if (newLocation.col() < maze.getNumColumns() - 1 && 
            !maze.isWall(newLocation.row(), newLocation.col() + 1)) {
          availableDirections++;
        }
        
        logMovement("AVAILABLE_DIRECTIONS=" + availableDirections);
        
        // Special cases for decision making:
        boolean isReversal = false;
        boolean isNearGhostHouse = false;
        
        // Check if this is a reversal of direction
        if ((currentDirection == Direction.UP && nextDirection == Direction.DOWN)
            || (currentDirection == Direction.DOWN && nextDirection == Direction.UP)
            || (currentDirection == Direction.LEFT && nextDirection == Direction.RIGHT)
            || (currentDirection == Direction.RIGHT && nextDirection == Direction.LEFT)) {
          isReversal = true;
          logMovement("IS_REVERSAL=TRUE");
        }
        
        // Check if we're near the ghost house - where special movement rules apply
        Location homeLocation = getHomeLocation();
        if (homeLocation != null) {
          int homeRow = homeLocation.row();
          int homeCol = homeLocation.col();
          // Consider near ghost house if within 5 cells horizontally and 5 cells vertically
          if (Math.abs(newLocation.row() - homeRow) <= 5 && 
              Math.abs(newLocation.col() - homeCol) <= 5) {
            isNearGhostHouse = true;
            logMovement("IS_NEAR_GHOST_HOUSE=TRUE");
          }
        }
        
        // Decision logic based on the situation:
        boolean allowDirectionChange = true;
        
        // 1. If we're in a corridor (only 2 directions possible), be careful about oscillation
        if (availableDirections <= 2) {
          logMovement("IN_CORRIDOR=TRUE");
          
          // In corridors, generally don't allow immediate reversals unless:
          if (isReversal) {
            // a) We've been trying the same direction for several frames (possibly stuck)
            if (stuckFrameCount >= 3) {
              allowDirectionChange = true;
              logMovement("ALLOWING_REVERSAL_DUE_TO_STUCK_COUNT");
            } 
            // b) We need to get out of the ghost house area
            else if (isNearGhostHouse && getMode() != Mode.INACTIVE) {
              allowDirectionChange = true; 
              logMovement("ALLOWING_REVERSAL_NEAR_GHOST_HOUSE");
            }
            // c) Otherwise, don't allow reversals in corridors to prevent oscillation
            else {
              allowDirectionChange = false;
              logMovement("PREVENTING_CORRIDOR_REVERSAL");
            }
          }
        } 
        // 2. At intersections (3+ directions), allow direction changes more freely
        else {
          // At intersections, always allow non-reversal turns
          if (!isReversal) {
            allowDirectionChange = true;
            logMovement("ALLOWING_INTERSECTION_TURN");
          }
          // For reversals at intersections, be selective
          else {
            // Allow reversals at intersections in certain conditions
            if (stuckFrameCount > 0 || isNearGhostHouse) {
              allowDirectionChange = true;
              logMovement("ALLOWING_INTERSECTION_REVERSAL");
            } else {
              // Avoid unnecessary reversals when we have other options
              // Ghosts shouldn't turn around for no reason at intersections
              allowDirectionChange = false;
              logMovement("PREVENTING_UNNECESSARY_REVERSAL");
            }
          }
        }
        
        // Apply the final decision
        if (allowDirectionChange) {
          Direction oldDirection = currentDirection;
          currentDirection = nextDirection;
          
          // When changing direction at an intersection, ensure we're exactly
          // at the center of the cell to prevent position drift
          if (atCenterPoint) {
            // Snap to center of current cell
            double centeredRow = newLocation.row() + 0.5;
            double centeredCol = newLocation.col() + 0.5;
            setRowExact(centeredRow);
            setColExact(centeredCol);
          }
          
          pastCenter = false;
          stuckFrameCount = 0; // Reset stuck counter when making a decision
          logMovement("CHANGED_DIRECTION_FROM=" + oldDirection + "_TO=" + currentDirection);
        } else {
          logMovement("DIRECTION_CHANGE_PREVENTED");
        }
      } else {
        logMovement("CANNOT_CHANGE_DIRECTION");
      }
    }

    // Update pastCenter - only set to true if we've passed center
    // but reset to false if at center or a new cell
    if (atCenterPoint || movedToNewCell) {
      pastCenter = false;
      logMovement("RESET_PAST_CENTER_FLAG");
    } else {
      // Check if we've passed the center
      boolean passedCenter = distanceToCenter < 0;

      // Only update pastCenter to true if we weren't already past center
      // This prevents recalculating after we've committed to a direction
      if (!pastCenter && passedCenter) {
        pastCenter = true;
        logMovement("SET_PAST_CENTER_FLAG");
      }
    }

    // Recalculate next cell when:
    // 1. We've moved to a new cell
    // 2. We've hit a wall
    // 3. We're at the center of a cell and not currently past the center
    boolean needsRecalculation = movedToNewCell || hitWall || (atCenterPoint && !pastCenter);
    
    if (needsRecalculation) {
      String reason = movedToNewCell ? "NEW_CELL" : hitWall ? "HIT_WALL" : "AT_CENTER";
      logMovement("NEEDS_RECALCULATION=" + reason);
      calculateNextCell(description);
    }
    
    logMovement("UPDATE_END");
  }

  /**
   * Helper method to ensure that the location is updated in the same way every time.
   * Ensures the ghost is always positioned at the center of the cell when reaching a new cell.
   * 
   * @param curRowExact Exact row position
   * @param curColExact Exact column position
   */
  private void updateLocation(final double curRowExact, final double curColExact) {
    // Calculate the current cell coordinates
    int newRow = (int) curRowExact;
    int newCol = (int) curColExact;
    
    // Check if we're moving to a new cell
    boolean newCell = currentLocation == null || 
                     newRow != currentLocation.row() || 
                     newCol != currentLocation.col();

    // If we're moving to a new cell, ensure we're positioned at the center
    if (newCell) {
      // Center the ghost in the new cell
      double centeredRowExact = newRow + 0.5;
      double centeredColExact = newCol + 0.5;
      
      // Update the exact coordinates with the centered position
      setRowExact(centeredRowExact);
      setColExact(centeredColExact);
    } else {
      // For movement within the same cell, use the original position
      setRowExact(curRowExact);
      setColExact(curColExact);
    }

    // Get the current discrete cell coordinates
    int row = (int) getRowExact();
    int col = (int) getColExact();

    // Update the current location
    currentLocation = new Location(row, col);
  }

  @Override
  public double getBaseIncrement() {
    return baseIncrement;
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
      // Reset stuck detection when mode changes
      stuckFrameCount = 0;
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
   * Calculates the distance from the current position to the center of the current cell.
   *
   * @return A value that indicates: - Positive: we are approaching the center - Negative: we have
   *     passed the center - Near zero: we are at the center
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

  /**
   * Gets the next cell location that the ghost is moving toward.
   * 
   * @return The next cell location
   */
  public Location getNextCell() {
    return nextLocation;
  }

  @Override
  public double getCurrentIncrement() {
    return currentIncrement;
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
