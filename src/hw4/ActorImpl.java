package hw4;

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

public abstract class ActorImpl implements Actor {
  private static final double ERR =
      0.001; // Margin of error for comparing exact coordinates to the center of a cell
  // SPEED
  private double baseIncrement;

  protected MazeMap maze; // A read-only representation of the maze for detecting walls and edges
  private Location scatterTarget; // The scatter location for scatter mode
  private Location home;
  private Location currentLocation; // The current location
  private Location previousLocation;
  private Location nextLocation; // The Next Location
  private Direction nextDirection; // The Next Direction
  private Direction currentDirection; // Current Direction
  private Direction homeDirection;
  private Mode currentMode; // The Current Mode
  private double colExact;
  private double rowExact;
  private double currentIncrement;
  private boolean pastCenter;
  protected Random rand;
  
  // Anti-stuck mechanism
  private int stuckFrameCount = 0;
  private Location previousStuckLocation = null;
  private static final int STUCK_THRESHOLD = 10; // Number of frames to consider "stuck"

  /**
   * Constructor for SuperClass for Ghost Initialization Parameters
   *
   * @param maze
   * @param home
   * @param baseSpeed
   * @param homeDirection
   * @param scatterTarget
   * @param rand
   */
  ActorImpl(
      MazeMap maze,
      Location home,
      double baseSpeed,
      Direction homeDirection,
      Location scatterTarget,
      Random rand) {
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
    this.currentLocation = new Location((int)rowExact, (int)colExact);
    
    // Initialize next direction and location
    this.nextDirection = homeDirection;
    
    // Calculate initial next location based on home direction
    Location tbal = tBAL();
    this.nextLocation = tbal;
  }

  public void reset() {
    setMode(Mode.INACTIVE, null);
    currentIncrement = baseIncrement;
    setDirection(homeDirection);
    updateLocation(home.row() + 0.5, home.col() + 0.5);
    pastCenter = false;
    nextDirection = homeDirection;
    nextLocation = tBAL();
    
    // Reset anti-stuck mechanism
    stuckFrameCount = 0;
    previousStuckLocation = null;
  }

  /**
   * Returns the next cell location based on current direction.
   * Checks for walls and only returns a valid next location.
   *
   * @return The next valid cell location
   */
  private Location tBAL() {
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
      nearHome = (row == homeLoc.row() && Math.abs(col - homeLoc.col()) <= 2) ||
                 (col == homeLoc.col() && Math.abs(row - homeLoc.row()) <= 2);
    }
    
    // Ghost house escape logic - force upward movement when near home and exiting
    if (nearHome && getMode() != Mode.INACTIVE) {
      // Check if up is a valid direction
      if (row > 0 && !maze.isWall(row - 1, col)) {
        newRow = row - 1;
        currentDirection = Direction.UP;
        return new Location(newRow, col);
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
    }
    
    // Return the next location
    return new Location(newRow, newCol);
  }

  /**
   * Returns the neighbors of the to be arrived at location as their location but null if they are
   * walls, out of bounds, or the opposite direction of the current direction
   *
   * @param tBAL - The to be arrived at location which is the location that the ghost will arrive at
   *     after moving one increment with the current direction.
   * @return
   */
  private Location[] getNeighbors(Location tBAL) {
    Location[] neighbors = new Location[4];
    int numRows = maze.getNumRows();
    int numCols = maze.getNumColumns();
    
    // Safety check for invalid location
    if (tBAL == null || tBAL.row() < 0 || tBAL.row() >= numRows || 
        tBAL.col() < 0 || tBAL.col() >= numCols) {
      return neighbors; // Return array of nulls if tBAL is invalid
    }

    // Check UP neighbor
    if (tBAL.row() - 1 >= 0 && 
        !maze.isWall(tBAL.row() - 1, tBAL.col()) && 
        getCurrentDirection() != Direction.DOWN) {
      neighbors[0] = new Location(tBAL.row() - 1, tBAL.col());
    } else {
      neighbors[0] = null;
    }

    // Check DOWN neighbor
    if (tBAL.row() + 1 < numRows && 
        !maze.isWall(tBAL.row() + 1, tBAL.col()) && 
        getCurrentDirection() != Direction.UP) {
      neighbors[1] = new Location(tBAL.row() + 1, tBAL.col());
    } else {
      neighbors[1] = null;
    }

    // Check LEFT neighbor
    if (tBAL.col() - 1 >= 0 && 
        !maze.isWall(tBAL.row(), tBAL.col() - 1) && 
        getCurrentDirection() != Direction.RIGHT) {
      neighbors[2] = new Location(tBAL.row(), tBAL.col() - 1);
    } else {
      neighbors[2] = null;
    }

    // Check RIGHT neighbor
    if (tBAL.col() + 1 < numCols && 
        !maze.isWall(tBAL.row(), tBAL.col() + 1) && 
        getCurrentDirection() != Direction.LEFT) {
      neighbors[3] = new Location(tBAL.row(), tBAL.col() + 1);
    } else {
      neighbors[3] = null;
    }

    return neighbors;
  }


  public void calculateNextCell(Descriptor d) {
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
    canMove[0] = row > 0 && 
                !maze.isWall(row - 1, col) && 
                currentDirection != Direction.DOWN;
    
    // DOWN direction              
    canMove[1] = row < numRows - 1 && 
                !maze.isWall(row + 1, col) && 
                currentDirection != Direction.UP;
    
    // LEFT direction              
    canMove[2] = col > 0 && 
                !maze.isWall(row, col - 1) && 
                currentDirection != Direction.RIGHT;
    
    // RIGHT direction              
    canMove[3] = col < numCols - 1 && 
                !maze.isWall(row, col + 1) && 
                currentDirection != Direction.LEFT;
    
    // --- STEP 2: Create neighbor locations for valid moves ---
    Location[] neighbors = new Location[4];
    int validMoveCount = 0;
    
    for (int i = 0; i < 4; i++) {
      if (canMove[i]) {
        int newRow = row;
        int newCol = col;
        
        switch (i) {
          case 0: newRow--; break; // UP
          case 1: newRow++; break; // DOWN
          case 2: newCol--; break; // LEFT
          case 3: newCol++; break; // RIGHT
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
      if (!canMove[i] || neighbors[i] == null) continue;
      
      double distance = calculateDistanceTween(neighbors[i], targetLocation);
      
      // Take this direction if it's better, or equal but higher priority
      if (distance < shortestDistance || 
          (Math.abs(distance - shortestDistance) < ERR && i < bestDirectionIndex)) {
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
   * Helper method to handle ghost movement in frightened mode
   * In this mode, ghosts move randomly at intersections
   */
  private void handleFrightenedMode() {
    Location currentLoc = getCurrentLocation();
    if (currentLoc == null) return;
    
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
    canMove[1] = row < numRows - 1 && !maze.isWall(row + 1, col) && currentDirection != Direction.UP;
    canMove[2] = col > 0 && !maze.isWall(row, col - 1) && currentDirection != Direction.RIGHT;
    canMove[3] = col < numCols - 1 && !maze.isWall(row, col + 1) && currentDirection != Direction.LEFT;
    
    // Count valid moves
    for (boolean move : canMove) {
      if (move) validMoveCount++;
    }
    
    // If we're at an intersection (>1 possible direction)
    if (validMoveCount > 1) {
      // Pick a random valid direction
      int randomIndex = rand.nextInt(validMoveCount);
      int currentCount = 0;
      
      for (int i = 0; i < 4; i++) {
        if (canMove[i]) {
          if (currentCount == randomIndex) {
            // This is our randomly chosen direction
            nextDirection = directions[i];
            int newRow = row;
            int newCol = col;
            
            switch (i) {
              case 0: newRow--; break; // UP
              case 1: newRow++; break; // DOWN
              case 2: newCol--; break; // LEFT
              case 3: newCol++; break; // RIGHT
            }
            
            nextLocation = new Location(newRow, newCol);
            return;
          }
          currentCount++;
        }
      }
    } 
    else if (validMoveCount == 1) {
      // Only one possible direction, take it
      for (int i = 0; i < 4; i++) {
        if (canMove[i]) {
          nextDirection = directions[i];
          int newRow = row;
          int newCol = col;
          
          switch (i) {
            case 0: newRow--; break; // UP
            case 1: newRow++; break; // DOWN
            case 2: newCol--; break; // LEFT
            case 3: newCol++; break; // RIGHT
          }
          
          nextLocation = new Location(newRow, newCol);
          return;
        }
      }
    } 
    else {
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
   * Helper method to determine the direction toward a target
   */
  private Direction getDirectionToTarget(Location current, Location target) {
    if (current == null || target == null) return currentDirection;
    
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
   * Calculates the distance between two given locations using the distance formula \sqrt{
   * (x_{1}-x_{2})^{2}+(y_{1}-y_{2})^{2} }
   *
   * @param loc1
   * @param loc2
   * @return
   */
  protected double calculateDistanceTween(Location loc1, Location loc2) {
    // Calculate the distance between a Location and another Location
    double x1 = loc1.col();
    double y1 = loc1.row();
    double x2 = loc2.col();
    double y2 = loc2.row();
    return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
  }

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

  public void update(Descriptor description) {
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
    int currentRow = (int)currentRowExact;
    int currentCol = (int)currentColumnExact;
    
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
    if (previousStuckLocation != null && 
        previousStuckLocation.equals(getCurrentLocation())) {
      stuckFrameCount++;
      
      // If stuck for too many frames, force a direction change
      if (stuckFrameCount >= STUCK_THRESHOLD) {
        // Force ghost to choose a new direction (anything except the current one)
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        Direction originalDirection = currentDirection;
        
        // Try to find a valid direction that isn't the current one
        for (Direction dir : directions) {
          if (dir != originalDirection) {
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
            }
            
            if (canMove) {
              // Force new direction
              currentDirection = dir;
              
              // Reset stuck counter
              stuckFrameCount = 0;
              
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
        int nextCol = (int)(currentColumnExact - increment);
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
        nextCol = (int)(currentColumnExact + increment);
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
        int nextRow = (int)(currentRowExact - increment);
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
        nextRow = (int)(currentRowExact + increment);
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
    if (atCenterPoint && nextDirection != null) {
      // Verify the next direction isn't immediately blocked
      boolean canChangeDirection = false;
      
      switch (nextDirection) {
        case UP:
          canChangeDirection = newLocation.row() > 0 && 
                              !maze.isWall(newLocation.row() - 1, newLocation.col());
          break;
        case DOWN:
          canChangeDirection = newLocation.row() < numRows - 1 && 
                              !maze.isWall(newLocation.row() + 1, newLocation.col());
          break;
        case LEFT:
          canChangeDirection = newLocation.col() > 0 && 
                              !maze.isWall(newLocation.row(), newLocation.col() - 1);
          break;
        case RIGHT:
          canChangeDirection = newLocation.col() < numCols - 1 && 
                              !maze.isWall(newLocation.row(), newLocation.col() + 1);
          break;
      }
      
      // Make the change if it's valid
      if (canChangeDirection) {
        currentDirection = nextDirection;
        pastCenter = false;
      }
    }
    
    // Update pastCenter - only set to true if we've passed center
    // but reset to false if at center or a new cell
    if (atCenterPoint || movedToNewCell) {
      pastCenter = false; 
    } else {
      pastCenter = distanceToCenter < 0;
    }
    
    // Recalculate next cell when:
    // 1. We've moved to a new cell
    // 2. We've hit a wall
    // 3. We're at the center of a cell (for choosing a direction at intersections)
    // 4. We've changed direction
    boolean needsRecalculation = movedToNewCell || hitWall || atCenterPoint;
    
    if (needsRecalculation) {
      calculateNextCell(description);
    }
  }

  // Helper method so that we ensure that the location is updated in the same way every time
  private void updateLocation(double curRowExact, double curColExact) {
    // Update the exact coordinates
    setRowExact(curRowExact);
    setColExact(curColExact);
    
    // Get the current discrete cell coordinates
    int row = (int) getRowExact();
    int col = (int) getColExact();
    
    // Update the current location
    currentLocation = new Location(row, col);
  }

  public double getBaseIncrement() {
    return baseIncrement;
  }

  public void setMode(Mode gMode, Descriptor Description) {
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
      calculateNextCell(Description);
    }
    
    // Mode based speed adjustments
    if (gMode == Mode.FRIGHTENED) {
      currentIncrement = baseIncrement * (2.0 / 3.0);
    } else if (gMode == Mode.DEAD) {
      currentIncrement = baseIncrement * (2.0);
    } else {
      currentIncrement = baseIncrement;
    }
  }

  public Mode getMode() {
    return currentMode;
  }

  /**
   * Calculates the distance from the current position to the center of the current cell.
   * 
   * @return A value that indicates:
   *         - Positive: we are approaching the center
   *         - Negative: we have passed the center
   *         - Near zero: we are at the center
   */
  protected double distanceToCenter() {
    // Get exact position
    double colPosition = getColExact();
    double rowPosition = getRowExact();
    
    // Default to 0 if no direction
    if (getCurrentDirection() == null) {
      return 0;
    }
    
    // Calculate offsets from the center of the current cell
    // The center of a cell is at (row+0.5, col+0.5)
    int cellRow = (int)rowPosition;
    int cellCol = (int)colPosition;
    double centerRow = cellRow + 0.5;
    double centerCol = cellCol + 0.5;
    
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
    }
    
    // Default return if we somehow get here
    return 0;
  }

  public Direction getCurrentDirection() {
    if (currentDirection == null) {
      return homeDirection;
    }
    return currentDirection;
  }

  public void setDirection(Direction dir) {
    currentDirection = dir;
  }

  public Location getCurrentLocation() {
    return currentLocation;
  }

  public Location getHomeLocation() {
    return home;
  }

  public void setColExact(double c) {
    colExact = c;
  }

  public void setRowExact(double r) {
    rowExact = r;
  }

  public double getColExact() {
    return colExact;
  }

  public double getRowExact() {
    return rowExact;
  }

  public Location getNextCell() {
    return nextLocation;
  }

  public double getCurrentIncrement() {
    return currentIncrement;
  }

  protected Location getScatterTarget() {
    return scatterTarget;
  }

  abstract Location getTargetLocation(Descriptor desc);
}
