package com.pacman.ghost;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import java.util.Random;

/**
 * Pinky (Pink Ghost) implementation.
 * Personality: Ambusher
 * Target: 4 tiles ahead of Pac-Man's current position and direction
 * Strategy: Attempts to position ahead of Pac-Man to trap him from the front
 * Note: Due to a bug in the original game, when Pac-Man faces upward, 
 * Pinky targets 4 tiles up and 4 tiles left of Pac-Man
 */
public final class Pinky extends ActorImpl {

  /** Maximum number of attempts to find a random location. */
  private static final int MAX_ATTEMPTS = 20;
  
  /** Number of tiles Pinky targets ahead of Pacman. */
  private static final int TARGET_DISTANCE = 4;
  
  /**
   * Constructor for Pinky ghost.
   *
   * @param maze The maze map
   * @param home The home/starting location
   * @param baseSpeed The base movement speed
   * @param homeDirection The initial direction
   * @param scatterTarget The target location during scatter mode
   * @param rand Random number generator for movement
   */
  public Pinky(
      final MazeMap maze,
      final Location home,
      final double baseSpeed,
      final Direction homeDirection,
      final Location scatterTarget,
      final Random rand) {
    super(maze, home, baseSpeed, homeDirection, scatterTarget, rand);
  }

  /**
   * Gets the target location based on the current mode.
   * When in CHASE mode, targets 4 tiles ahead of Pac-Man.
   *
   * @param desc The game descriptor containing game state
   * @return The target location
   */
  @Override
  protected Location getTargetLocation(final Descriptor desc) {
    if (super.getMode() == Mode.CHASE) {
      Location playerLocation = desc.getPlayerLocation();
      Direction playerDirection = desc.getPlayerDirection();

      // Target 4 tiles ahead of Pac-Man
      int targetRow = playerLocation.row();
      int targetCol = playerLocation.col();

      // Determine the target location based on Pac-Man's direction
      // Note the "bug" when Pac-Man is facing UP
      if (playerDirection == Direction.UP) {
        // Classic bug in the original game - when facing up,
        // target 4 tiles up and 4 tiles left
        targetRow -= TARGET_DISTANCE;
        targetCol -= TARGET_DISTANCE;
      } else if (playerDirection == Direction.DOWN) {
        targetRow += TARGET_DISTANCE;
      } else if (playerDirection == Direction.LEFT) {
        targetCol -= TARGET_DISTANCE;
      } else if (playerDirection == Direction.RIGHT) {
        targetCol += TARGET_DISTANCE;
      }

      // Create a new location for the target
      return new Location(targetRow, targetCol);
    } else if (super.getMode() == Mode.SCATTER) {
      return super.getScatterTarget();
    } else if (super.getMode() == Mode.FRIGHTENED) {
      // In FRIGHTENED mode, generate a valid random target location
      // This is just a target for decision making at intersections
      // Actual movement will use the handler in ActorImpl

      // Get maze dimensions
      int width = maze.getNumColumns();
      int height = maze.getNumRows();

      // Try to find a non-wall location
      Location randomLocation = null;
      // Limit attempts to prevent infinite loop
      final int maxAttempts = MAX_ATTEMPTS;
      int attempts = 0;

      while (randomLocation == null && attempts < maxAttempts) {
        // Generate random coordinates
        final int randomCol = rand.nextInt(width);
        final int randomRow = rand.nextInt(height);

        // Check if valid (not a wall)
        if (!maze.isWall(randomRow, randomCol)) {
          randomLocation = new Location(randomRow, randomCol);
        }

        attempts++;
      }

      // If we couldn't find a valid location, use current location
      // This is just a fallback - the frightened handler will still work
      if (randomLocation == null) {
        return getCurrentLocation();
      }

      return randomLocation;
    } else if (super.getMode() == Mode.INACTIVE) {
      return super.getCurrentLocation();
    } else { // DEAD mode
      // Return home location when in DEAD mode
      return super.getHomeLocation();
    }
  }
}
