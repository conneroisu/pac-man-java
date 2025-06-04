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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Base implementation of the Actor interface for all ghosts. Provides common ghost behavior like
 * movement, collision detection, and mode handling.
 */
public abstract class ActorImpl implements Actor {
  /** Margin of error for comparing exact coordinates to the center of a cell. */
  private static final double ERR = 0.001;

  /** Half cell offset for exact positioning. */
  private static final double HALF_CELL = 0.5;

  /** Ghost house row radius. */
  private static final int GHOST_HOUSE_ROW_RADIUS = 2;

  /** Ghost house column radius. */
  private static final int GHOST_HOUSE_COL_RADIUS = 3;

  /** Divisor for finding center position. */
  private static final int CENTER_DIVISOR = 2;

  /** First array index. */
  private static final int FIRST_INDEX = 0;

  /** Minimum array bounds. */
  private static final int MINIMUM_BOUNDS = 0;

  /** Single decrement offset. */
  private static final int SINGLE_DECREMENT = 1;

  /** Boundary check offset. */
  private static final double BOUNDARY_OFFSET = 0.5;

  /** Half-second update threshold. */
  private static final double HALF_SECOND_THRESHOLD = 1.5;

  /** Frightened continue probability. */
  private static final double FRIGHTENED_CONTINUE_PROBABILITY = 0.8;

  /** Array element count threshold. */
  private static final int ARRAY_ELEMENT_COUNT_THRESHOLD = 1;

  /** Multiple move count threshold for intersection decisions. */
  private static final int MULTIPLE_MOVE_THRESHOLD = 1;

  /** Distance tolerance for path preferences. */
  private static final double DISTANCE_TOLERANCE = 5.0;

  /** Base speed increment. */
  private final double baseIncrement;

  /** A read-only representation of the maze for detecting walls and edges. */
  protected MazeMap maze;

  /** The scatter location for scatter mode. */
  private final Location scatterTarget;

  /** The home/starting location. */
  private final Location home;

  /** The current location. */
  private Location currentLocation;

    /** The next target location. */
  private Location nextLocation;

  /** The next direction to move. */
  private Direction nextDirection;

  /** Current direction of movement. */
  private Direction currentDirection;

  /** Initial direction at home position. */
  private final Direction homeDirection;

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
  private final String ghostType;
  private static boolean loggingInitialized = false;
  private static int frameCounter = 0;

  /** Static initializer for logging. */
  static {
    try {
      logWriter = new PrintWriter(new FileWriter("ghost_movement.log", false));
      logWriter.println(
          "Frame,GhostType,Location,ExactLoc,Direction,NextLocation,Mode,StuckCounter,PastCenter");
      logWriter.flush();
      loggingInitialized = true;
    } catch (IOException e) {
      // Log file creation failed - continue without logging
      // This is acceptable as logging is not critical to game functionality
      loggingInitialized = false;
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
    this.rowExact = home.row() + HALF_CELL;
    this.colExact = home.col() + HALF_CELL;
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

    // Log initialization - moved after constructor to avoid calling overridable method
    initializeLogging();
  }

  /**
   * Initialize logging for this ghost after construction is complete.
   */
  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
  private void initializeLogging() {
    logMovement("INIT");
  }

  @Override
  public void reset() {
    logMovement("PRE-RESET");
    setMode(Mode.INACTIVE, null);
    currentIncrement = baseIncrement;
    setDirection(homeDirection);

    // Always position ghost exactly at cell center when resetting
    double centeredRow = home.row() + BOUNDARY_OFFSET;
    double centeredCol = home.col() + BOUNDARY_OFFSET;

    setRowExact(centeredRow);
    setColExact(centeredCol);
    currentLocation = new Location(home.row(), home.col());

    pastCenter = false;
    nextDirection = homeDirection;
    nextLocation = getNextLocation(currentLocation, nextDirection);

    logMovement("POST-RESET");
  }

  /**
   * Gets the location in the given direction from the current location. Handles wall checking and
   * maze boundaries.
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
        if (row > MINIMUM_BOUNDS && !maze.isWall(row - SINGLE_DECREMENT, col)) {
          return new Location(row - SINGLE_DECREMENT, col);
        }
        break;
      case DOWN:
        if (row < numRows - SINGLE_DECREMENT && !maze.isWall(row + SINGLE_DECREMENT, col)) {
          return new Location(row + SINGLE_DECREMENT, col);
        }
        break;
      case LEFT:
        if (col > MINIMUM_BOUNDS && !maze.isWall(row, col - SINGLE_DECREMENT)) {
          return new Location(row, col - SINGLE_DECREMENT);
        } else if (col == FIRST_INDEX) {
          // Tunnel wrap around to right side
          return new Location(row, numCols - SINGLE_DECREMENT);
        }
        break;
      case RIGHT:
        if (col < numCols - SINGLE_DECREMENT && !maze.isWall(row, col + SINGLE_DECREMENT)) {
          return new Location(row, col + SINGLE_DECREMENT);
        } else if (col == numCols - SINGLE_DECREMENT) {
          // Tunnel wrap around to left side
          return new Location(row, FIRST_INDEX);
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
   * Calculates and returns the next cell location based on current direction. Checks for walls and
   * tries to find a valid direction if the current one is blocked.
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
      int centerRow = maze.getNumRows() / CENTER_DIVISOR;
      int centerCol = maze.getNumColumns() / CENTER_DIVISOR;
      inGhostHouse =
          Math.abs(currentLoc.row() - centerRow) <= GHOST_HOUSE_ROW_RADIUS
              && Math.abs(currentLoc.col() - centerCol) <= GHOST_HOUSE_COL_RADIUS;
    }

    if (inGhostHouse && getMode() != Mode.INACTIVE) {
      // For ghost house escape, prefer moving toward the exit (usually upward)
      // But avoid walls by checking each direction carefully
      Direction[] escapeOrder = {Direction.UP, Direction.LEFT, Direction.RIGHT, Direction.DOWN};

      for (Direction dir : escapeOrder) {
        Location loc = getNextLocation(currentLoc, dir);
        if (loc != null && !maze.isWall(loc.row(), loc.col())) {
          nextDirection = dir;
          nextLocation = loc;
          logMovement("GHOST_HOUSE_ESCAPE_" + dir);
          return;
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
    Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
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

  /** Gets the opposite direction. */
  private Direction getOppositeDirection(Direction dir) {
      return switch (dir) {
          case UP -> Direction.DOWN;
          case DOWN -> Direction.UP;
          case LEFT -> Direction.RIGHT;
          case RIGHT -> Direction.LEFT;
          default -> dir;
      };
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

  /** Calculates the best path to a target location. */
  private void calculatePathToTarget(Location currentLoc, Location targetLoc) {
    if (currentLoc == null || targetLoc == null) {
      // If we have no valid locations, just continue in current direction
      calculateNextCellLocation();
      return;
    }

    // Get all possible directions
    Direction oppositeDir = getOppositeDirection(currentDirection);
    Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    Direction bestDir = null;
    Location bestLoc = null;
    double shortestDistance = Double.MAX_VALUE;

    // Count valid moves to detect dead-end situations
    int validMoveCount = 0;
    Direction[] validDirections = new Direction[4];
    Location[] validLocations = new Location[4];
    double[] validDistances = new double[4];

    // First pass: collect all valid directions except opposite
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

      validDirections[validMoveCount] = dir;
      validLocations[validMoveCount] = nextLoc;
      validDistances[validMoveCount] = distance;
      validMoveCount++;

      // Choose this direction if it's better
      if (distance < shortestDistance) {
        shortestDistance = distance;
        bestDir = dir;
        bestLoc = nextLoc;
      }
    }

    // Strong anti-oscillation: if we have multiple valid moves, strongly prefer continuing straight
    if (validMoveCount > MULTIPLE_MOVE_THRESHOLD) {
      for (int i = 0; i < validMoveCount; i++) {
        if (validDirections[i] == currentDirection) {
          // Give STRONG preference to continuing in same direction
          // Only change if another direction is MUCH better (5+ units)
          double currentDirDistance = validDistances[i];
          if (currentDirDistance <= shortestDistance + DISTANCE_TOLERANCE) {
            bestDir = currentDirection;
            bestLoc = validLocations[i];
            logMovement("CONTINUING_STRAIGHT_TO_AVOID_OSCILLATION");
            break;
          }
        }
      }
    }

    // For very constrained situations (only 1-2 moves), add extra anti-oscillation
    if (validMoveCount <= 2 && bestDir != null) {
      // In constrained areas, be even more reluctant to change direction
      for (int i = 0; i < validMoveCount; i++) {
        if (validDirections[i] == currentDirection) {
          bestDir = currentDirection;
          bestLoc = validLocations[i];
          logMovement("CONSTRAINED_AREA_CONTINUING_STRAIGHT");
          break;
        }
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
      logMovement(
          "TARGET_PATH_DIR="
              + bestDir
              + ", DISTANCE="
              + shortestDistance
              + ", VALID_MOVES="
              + validMoveCount);
    } else {
      // Extremely rare - no valid moves at all
      nextLocation = currentLoc;
      logMovement("NO_VALID_TARGET_PATH");
    }
  }

  /**
   * Helper method to handle ghost movement in frightened mode. In this mode, ghosts move randomly
   * at intersections.
   */
  private void handleFrightenedMode() {
    logMovement("FRIGHTENED_MODE_START");
    Location currentLoc = getCurrentLocation();
    if (currentLoc == null) {
      return;
    }

    // Get valid directions (no reversing)
    Direction oppositeDir = getOppositeDirection(currentDirection);
    Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    // Count valid moves and collect valid directions
    int validMoveCount = 0;
    Direction[] validDirections = new Direction[4];
    Location[] validLocations = new Location[4];

      for (Direction direction : directions) {
          // Skip the opposite direction (no reversing)
          if (direction == oppositeDir) {
              continue;
          }

          Location nextLoc = getNextLocation(currentLoc, direction);
          if (nextLoc != null && !maze.isWall(nextLoc.row(), nextLoc.col())) {
              validDirections[validMoveCount] = direction;
              validLocations[validMoveCount] = nextLoc;
              validMoveCount++;
          }
      }

    logMovement("FRIGHTENED_VALID_MOVES=" + validMoveCount);

    // Handle case based on number of valid moves
    if (validMoveCount > MULTIPLE_MOVE_THRESHOLD) {
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
      if (canContinueSameDirection && rand.nextDouble() < FRIGHTENED_CONTINUE_PROBABILITY) {
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

    } else if (validMoveCount == ARRAY_ELEMENT_COUNT_THRESHOLD) {
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

  /** Calculates the distance between two given locations using the distance formula. */
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
      Location previousLocation = getCurrentLocation();

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

    // Check if we need to make a decision (at center or entering new cell)
    boolean movedToNewCell = !previousLocation.equals(getCurrentLocation());
    if (movedToNewCell) {
      pastCenter = false; // Reset for new cell
      logMovement("MOVED_TO_NEW_CELL");
    }

    // At cell center - make movement decisions
    if (atCellCenter && !pastCenter) {
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

    // Simple directional movement with proper axis alignment
    double newRowExact = currentRowExact;
    double newColExact = currentColExact;

    // Check if we can move in current direction
    Location testNextLoc = getNextLocation(getCurrentLocation(), currentDirection);
    boolean canMove = (testNextLoc != null && !maze.isWall(testNextLoc.row(), testNextLoc.col()));

    if (!canMove) {
      // We're blocked - recalculate direction
      logMovement("BLOCKED_RECALCULATING");
      calculateNextCell(description);
      if (nextDirection != null && nextDirection != currentDirection) {
        currentDirection = nextDirection;
        logMovement("DIRECTION_CHANGED_TO=" + currentDirection);
      }
    }

    // Apply movement in current direction
    if (canMove
        || (nextDirection != null
            && getNextLocation(getCurrentLocation(), currentDirection) != null)) {
      switch (currentDirection) {
        case UP:
          newRowExact = Math.max(BOUNDARY_OFFSET, currentRowExact - increment);
          newColExact = Math.floor(currentColExact) + BOUNDARY_OFFSET; // Stay centered on column
          break;
        case DOWN:
          newRowExact = Math.min(maze.getNumRows() - HALF_SECOND_THRESHOLD, currentRowExact + increment);
          newColExact = Math.floor(currentColExact) + BOUNDARY_OFFSET; // Stay centered on column
          break;
        case LEFT:
          newColExact = currentColExact - increment;
          newRowExact = Math.floor(currentRowExact) + BOUNDARY_OFFSET; // Stay centered on row
          // Handle tunnel wraparound
          if (newColExact < BOUNDARY_OFFSET) {
            newColExact = maze.getNumColumns() - BOUNDARY_OFFSET;
          }
          break;
        case RIGHT:
          newColExact = currentColExact + increment;
          newRowExact = Math.floor(currentRowExact) + BOUNDARY_OFFSET; // Stay centered on row
          // Handle tunnel wraparound
          if (newColExact >= maze.getNumColumns() - BOUNDARY_OFFSET) {
            newColExact = BOUNDARY_OFFSET;
          }
          break;
        default:
          break;
      }

      // Final safety check - ensure we don't enter a wall
      int newCellRow = (int) Math.floor(newRowExact);
      int newCellCol = (int) Math.floor(newColExact);

      if (newCellRow >= MINIMUM_BOUNDS
          && newCellRow < maze.getNumRows()
          && newCellCol >= MINIMUM_BOUNDS
          && newCellCol < maze.getNumColumns()
          && !maze.isWall(newCellRow, newCellCol)) {
        // Safe to move
        setRowExact(newRowExact);
        setColExact(newColExact);
        currentLocation = new Location(newCellRow, newCellCol);
        logMovement("MOVED_TO_POSITION");
      } else {
        // Would hit wall - stay in place and force recalculation
        setRowExact(rowCenter);
        setColExact(colCenter);
        pastCenter = false; // Force recalculation next frame
        logMovement("WALL_COLLISION_STAYING_PUT");
      }
    } else {
      // Can't move at all - stay in place
      logMovement("COMPLETELY_BLOCKED");
    }

    logMovement("UPDATE_END");
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
      return switch (getCurrentDirection()) {
          case LEFT ->
              // When moving left, a positive value means we're to the right of center
                  colPosition - centerCol;
          case RIGHT ->
              // When moving right, a positive value means we're to the left of center
                  centerCol - colPosition;
          case UP ->
              // When moving up, a positive value means we're below the center
                  rowPosition - centerRow;
          case DOWN ->
              // When moving down, a positive value means we're above the center
                  centerRow - rowPosition;
          default ->
              // Default return if we somehow get here
                  0;
      };
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
   * Returns the next cell the ghost is headed towards. Used by test classes.
   *
   * @return The next location
   */
  public Location getNextCell() {
    return nextLocation;
  }

  /**
   * Gets the target location based on the current mode. This method is implemented differently by
   * each ghost.
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

      logWriter.println(
          frameCounter
              + ","
              + ghostType
              + ","
              + currentLocation
              + ","
              + String.format("%.2f,%.2f", rowExact, colExact)
              + ","
              + directionStr
              + ","
              + nextLocationStr
              + ","
              + modeStr
              + ","
              + "0"
              + ","
              + pastCenter
              + ","
              + message);
      logWriter.flush();
    }
  }

}
