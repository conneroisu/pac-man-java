package com.pacman.ghost;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import java.util.Random;

/**
 * Blinky (Red Ghost) implementation.
 * Personality: Aggressive, direct pursuer
 * Target: Pac-Man's current position
 * Strategy: Targets Pac-Man's exact location, making him the most direct pursuer
 */
public final class Blinky extends ActorImpl {
  
  /** Maximum number of attempts to find a random location. */
  private static final int MAX_ATTEMPTS = 20;

  /**
   * Constructor for Blinky ghost.
   *
   * @param maze The maze map
   * @param home The home/starting location
   * @param baseSpeed The base movement speed
   * @param homeDirection The initial direction
   * @param scatterTarget The target location during scatter mode
   * @param rand Random number generator for movement
   */
  public Blinky(
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
   *
   * @param desc The game descriptor containing game state
   * @return The target location
   */
  @Override
  protected Location getTargetLocation(final Descriptor desc) {
    if (super.getMode() == Mode.CHASE) {
      return desc.getPlayerLocation();
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
      // When dead, head back to home
      return super.getHomeLocation();
    }
  }
}