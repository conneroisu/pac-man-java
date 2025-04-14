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

/**
 * Base implementation of the Actor interface for all ghosts.
 * Provides common ghost behavior like movement, collision detection, and mode handling.
 */
public abstract class ActorImpl implements Actor {
  /** Margin of error for comparing exact coordinates to the center of a cell. */
  private static final double ERR = 0.001;
  
  /** Number of frames to consider "stuck". */
  private static final int STUCK_THRESHOLD = 5;
  
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

    // Initialize current location
    this.currentLocation = new Location((int) rowExact, (int) colExact);

    // Initialize next direction and location
    this.nextDirection = homeDirection;

    // Calculate initial next location based on home direction
    Location nextCellLocation = calculateNextCellLocation();
    this.nextLocation = nextCellLocation;
  }

  @Override
  public void reset() {
    setMode(Mode.INACTIVE, null);
    currentIncrement = baseIncrement;
    setDirection(homeDirection);
    updateLocation(home.row() + 0.5, home.col() + 0.5);
    pastCenter = false;
    nextDirection = homeDirection;
    nextLocation = calculateNextCellLocation();

    // Reset anti-stuck mechanism
    stuckFrameCount = 0;
    previousStuckLocation = null;
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

    // Ghost house escape logic - force upward movement when near home and exiting
    if (nearHome && getMode() != Mode.INACTIVE) {
      // Check if up is a valid direction
      if (row > 0 && !maze.isWall(row - 1, col)) {
        newRow = row - 1;
        currentDirection = Direction.UP;
        return new Location(newRow, col);
      // If up is blocked or we're already at the top of ghost house area,
      // try left or right to move out of the house
      } else if (col > 0 && !maze.isWall(row, col - 1)) {
        newCol = col - 1;
        currentDirection = Direction.LEFT;
        return new Location(row, newCol);
      } else if (col < numCols - 1 && !maze.isWall(row, col + 1)) {
        newCol = col + 1;
        currentDirection = Direction.RIGHT;
        return new Location(row, newCol);
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
    return new Location(newRow, newCol);
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
    // Skip calculation if we're past the center and still in the same cell
    // (This prevents oscillation/jittering)
    if (pastCenter && nextLocation != null) {
      return;
    }

    // Check for INACTIVE mode - in which case we do nothing
    if (getMode() == Mode.INACTIVE) {
      return;
    }

    // Special handling for FRIGHTENED mode
    if (getMode() == Mode.FRIGHTENED) {
      handleFrightenedMode();
      return;
    }

    // Define direction priority array (UP has highest priority for ties)
    Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    // Get current location and validate
    Location currentLoc = getCurrentLocation();
    if (currentLoc == null) {
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

    // --- STEP 3: Get target based on mode ---
    Location targetLocation = getTargetLocation(d);

    // Fallback to scatter target if no target is available
    if (targetLocation == null) {
      targetLocation = getScatterTarget();
    }

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
        return;
      }
    }

    // For other modes, choose the direction that minimizes distance to target
    int bestDirectionIndex = -1;
    double shortestDistance = Double.MAX_VALUE;

    for (int i = 0; i < 4; i++) {
      if (!canMove[i] || neighbors[i] == null) {
        continue;
      }

      double distance = calculateDistanceTween(neighbors[i], targetLocation);

      // Take this direction if it's better, or equal but higher priority
      if (distance < shortestDistance
          || (Math.abs(distance - shortestDistance) < ERR && i < bestDirectionIndex)) {
        shortestDistance = distance;
        bestDirectionIndex = i;
      }
    }

    // --- STEP 5: Choose final direction and next location ---

    // If we found a valid direction, use it
    if (bestDirectionIndex >= 0 && bestDirectionIndex < 4) {
      nextDirection = directions[bestDirectionIndex];
      nextLocation = neighbors[bestDirectionIndex];
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
      return;
    }

    // Last resort: find any valid direction
    for (int i = 0; i < 4; i++) {
      if (canMove[i]) {
        nextDirection = directions[i];
        nextLocation = neighbors[i];
        return;
      }
    }

    // If we get here, we're completely stuck - just stay in place
    nextLocation = currentLoc;
  }

  /**
   * Helper method to handle ghost movement in frightened mode.
   * In this mode, ghosts move randomly at intersections.
   */
  private void handleFrightenedMode() {
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
        } else {
          // Otherwise random choice
          randomIndex = rand.nextInt(validMoveCount);
        }
      } else {
        // Normal random choice
        randomIndex = rand.nextInt(validMoveCount);
      }

      // Set the next direction and location
      nextDirection = validDirections[randomIndex];
      nextLocation = validLocations[randomIndex];
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
      } else {
        // Nowhere to go, stay in place
        nextLocation = currentLoc;
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
    // If in INACTIVE mode, don't move
    if (getMode() == Mode.INACTIVE) {
      return;
    }

    // Store previous location for change detection
    previousLocation = getCurrentLocation();

    // Calculate next cell if not already done
    if (nextLocation == null) {
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
    }

    // PHASE 1: ANTI-STUCK MECHANISM
    // Check if the ghost is stuck in the same location for multiple frames
    if (previousStuckLocation != null && previousStuckLocation.equals(getCurrentLocation())) {
      stuckFrameCount++;

      // If stuck for too many frames, force a direction change
      if (stuckFrameCount >= STUCK_THRESHOLD) {
        // Force ghost to choose a new direction (anything except the current one)
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        Direction originalDirection = currentDirection;
        
        // Shuffle the directions array to introduce randomness
        for (int i = 0; i < directions.length; i++) {
          int j = rand.nextInt(directions.length);
          Direction temp = directions[i];
          directions[i] = directions[j];
          directions[j] = temp;
        }

        // First try directions that aren't opposite to current, to avoid ping-ponging
        for (Direction dir : directions) {
          // Skip opposite direction as the first choice
          if ((dir == Direction.UP && originalDirection == Direction.DOWN) ||
              (dir == Direction.DOWN && originalDirection == Direction.UP) ||
              (dir == Direction.LEFT && originalDirection == Direction.RIGHT) ||
              (dir == Direction.RIGHT && originalDirection == Direction.LEFT)) {
            continue;
          }
          
          boolean canMove = false;
          switch (dir) {
            case UP:
              canMove = currentRow > 0 && !maze.isWall(currentRow - 1, currentCol);
              break;
            case DOWN:
              canMove = currentRow < numRows - 1 && !maze.isWall(currentRow + 1, currentCol);
              break;
            case LEFT:
              canMove = currentCol > 0 && !maze.isWall(currentRow, currentCol - 1);
              break;
            case RIGHT:
              canMove = currentCol < numCols - 1 && !maze.isWall(currentRow, currentCol + 1);
              break;
            default:
              // No action needed for default case
              break;
          }

          if (canMove) {
            // Force new direction
            currentDirection = dir;

            // Reset stuck counter and pastCenter flag
            stuckFrameCount = 0;
            pastCenter = false;

            // Recalculate next cell with new direction
            calculateNextCell(description);
            break;
          }
        }
        
        // If we couldn't find a non-opposite direction, try any direction including opposite
        if (stuckFrameCount >= STUCK_THRESHOLD) {
          for (Direction dir : directions) {
            boolean canMove = false;
            switch (dir) {
              case UP:
                canMove = currentRow > 0 && !maze.isWall(currentRow - 1, currentCol);
                break;
              case DOWN:
                canMove = currentRow < numRows - 1 && !maze.isWall(currentRow + 1, currentCol);
                break;
              case LEFT:
                canMove = currentCol > 0 && !maze.isWall(currentRow, currentCol - 1);
                break;
              case RIGHT:
                canMove = currentCol < numCols - 1 && !maze.isWall(currentRow, currentCol + 1);
                break;
              default:
                // No action needed for default case
                break;
            }

            if (canMove) {
              // Force new direction
              currentDirection = dir;

              // Reset stuck counter and pastCenter flag
              stuckFrameCount = 0;
              pastCenter = false;

              // Recalculate next cell with new direction
              calculateNextCell(description);
              break;
            }
          }
        }
      }
    } else {
      // Reset stuck counter if we've moved
      stuckFrameCount = 0;
      previousStuckLocation = getCurrentLocation();
    }

    // PHASE 2: CALCULATE MOVEMENT
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
        }

        // Apply movement
        currentColumnExact -= increment;

        // Handle tunnel wraparound
        if (currentColumnExact < 0) {
          currentColumnExact += numCols;
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
        }

        // Apply movement
        currentColumnExact += increment;

        // Handle tunnel wraparound
        if (currentColumnExact >= numCols) {
          currentColumnExact -= numCols;
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
        }

        // Apply movement
        currentRowExact -= increment;

        // Handle edge case
        if (currentRowExact < 0) {
          currentRowExact = 0;
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
        }

        // Apply movement
        currentRowExact += increment;

        // Handle edge case
        if (currentRowExact >= numRows) {
          currentRowExact = numRows - 1;
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

    // PHASE 3: DIRECTION DECISIONS & RECALCULATION
    // We should make a direction change if:
    // 1. We're at a cell center point (or very close)
    // 2. We have a next direction already calculated
    // 3. The next direction isn't blocked by a wall
    // 4. We're not in the middle of an oscillation
    if (atCenterPoint && nextDirection != null) {
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
        // In maze intersections or when exiting the ghost house,
        // we should always allow direction changes even if they seem like oscillations
        // Only apply oscillation prevention in corridors
        boolean isInCorridor = false;
        boolean wouldOscillate = false;
        
        // Count available directions to determine if we're in a corridor
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
        
        // If we only have 2 directions available (forward and backward), we're in a corridor
        isInCorridor = (availableDirections <= 2);
        
        // Check for oscillation only if we're in a corridor
        if (isInCorridor) {
          final int oscillationThreshold = 3;
          
          // Opposite directions check
          if ((currentDirection == Direction.UP && nextDirection == Direction.DOWN)
              || (currentDirection == Direction.DOWN && nextDirection == Direction.UP)
              || (currentDirection == Direction.LEFT && nextDirection == Direction.RIGHT)
              || (currentDirection == Direction.RIGHT && nextDirection == Direction.LEFT)) {
            // Only consider it oscillation if we just changed direction recently
            if (stuckFrameCount < oscillationThreshold) {
              wouldOscillate = true;
            }
          }
        }
        
        if (!wouldOscillate) {
          currentDirection = nextDirection;
          pastCenter = false;
        }
      }
    }

    // Update pastCenter - only set to true if we've passed center
    // but reset to false if at center or a new cell
    if (atCenterPoint || movedToNewCell) {
      pastCenter = false;
    } else {
      // Check if we've passed the center
      boolean passedCenter = distanceToCenter < 0;

      // Only update pastCenter to true if we weren't already past center
      // This prevents recalculating after we've committed to a direction
      if (!pastCenter && passedCenter) {
        pastCenter = true;
      }
    }

    // Recalculate next cell when:
    // 1. We've moved to a new cell
    // 2. We've hit a wall
    // 3. We're at the center of a cell and not currently past the center
    boolean needsRecalculation = movedToNewCell || hitWall || (atCenterPoint && !pastCenter);

    if (needsRecalculation) {
      calculateNextCell(description);
    }
  }

  /**
   * Helper method to ensure that the location is updated in the same way every time.
   * 
   * @param curRowExact Exact row position
   * @param curColExact Exact column position
   */
  private void updateLocation(final double curRowExact, final double curColExact) {
    // Update the exact coordinates
    setRowExact(curRowExact);
    setColExact(curColExact);

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
      }

      // Recalculate next cell with new direction/mode
      pastCenter = false;
      calculateNextCell(description);
    }

    // Mode based speed adjustments
    final double frightenedSpeedFactor = 2.0 / 3.0;
    final double deadSpeedFactor = 2.0;
    
    if (gMode == Mode.FRIGHTENED) {
      currentIncrement = baseIncrement * frightenedSpeedFactor;
    } else if (gMode == Mode.DEAD) {
      currentIncrement = baseIncrement * deadSpeedFactor;
    } else {
      currentIncrement = baseIncrement;
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
}