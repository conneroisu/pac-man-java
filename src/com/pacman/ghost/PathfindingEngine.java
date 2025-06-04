package com.pacman.ghost;

import api.Descriptor;
import api.Direction;
import api.Location;

/**
 * Interface for ghost pathfinding algorithms. Handles navigation and direction choosing logic.
 */
public interface PathfindingEngine {
  
  /**
   * Calculates the best path to a target location.
   *
   * @param currentLoc The current location
   * @param targetLoc The target location to navigate to
   * @return A PathResult containing the next direction and location
   */
  PathResult calculatePathToTarget(Location currentLoc, Location targetLoc);
  
  /**
   * Handles frightened mode movement logic with random direction choosing.
   *
   * @param currentLoc The current location
   * @param currentDirection The current direction of movement
   * @return A PathResult containing the next direction and location
   */
  PathResult handleFrightenedMode(Location currentLoc, Direction currentDirection);
  
  /**
   * Calculates the next cell location based on current direction and obstacles.
   *
   * @param currentLoc The current location
   * @param currentDirection The current direction
   * @return A PathResult containing the next direction and location
   */
  PathResult calculateNextCellLocation(Location currentLoc, Direction currentDirection);
  
  /**
   * Evaluates all possible directions and finds the best option.
   *
   * @param currentLoc The current location
   * @param targetLoc The target location
   * @param currentDirection The current direction
   * @return A PathResult containing the best direction and location
   */
  PathResult findBestDirection(Location currentLoc, Location targetLoc, Direction currentDirection);
  
  /**
   * Simple record to hold pathfinding results.
   */
  record PathResult(Direction direction, Location location, String debugMessage) {
    public PathResult(Direction direction, Location location) {
      this(direction, location, "");
    }
  }
}