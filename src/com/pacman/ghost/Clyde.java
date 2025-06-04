package com.pacman.ghost;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import java.util.Random;

/**
 * Clyde (Orange Ghost) implementation. Personality: Shy, keeps distance Target: Alternates between
 * targeting Pac-Man and his scatter corner Strategy: - If Clyde is more than 8 tiles away from
 * Pac-Man, he targets Pac-Man directly (like Blinky) - If Clyde is within 8 tiles of Pac-Man, he
 * retreats to his scatter corner Result: Clyde tends to approach and then flee, creating an
 * on-again-off-again pursuit
 */
public final class Clyde extends ActorImpl {

  /** Distance threshold for Clyde's behavior (8 tiles). */
  private static final double DISTANCE_THRESHOLD = 8.0;

  /** Maximum number of attempts to find a random location. */
  private static final int MAX_ATTEMPTS = 20;

  /**
   * Constructor for Clyde ghost.
   *
   * @param maze The maze map
   * @param home The home/starting location
   * @param baseSpeed The base movement speed
   * @param homeDirection The initial direction
   * @param scatterTarget The target location during scatter mode
   * @param rand Random number generator for movement
   */
  public Clyde(
      final MazeMap maze,
      final Location home,
      final double baseSpeed,
      final Direction homeDirection,
      final Location scatterTarget,
      final Random rand) {
    super(maze, home, baseSpeed, homeDirection, scatterTarget, rand);
  }

  /**
   * Gets the target location based on the current mode. When in CHASE mode, Clyde targets Pac-Man
   * directly if more than 8 tiles away, otherwise retreats to scatter target.
   *
   * @param desc The game descriptor containing game state
   * @return The target location
   */
  @Override
  protected Location getTargetLocation(final Descriptor desc) {
    if (super.getMode() == Mode.CHASE) {
      Location playerLocation = desc.getPlayerLocation();
      Location currentLocation = super.getCurrentLocation();

      // Calculate distance to Pac-Man
      double distanceToPacman = calculateDistanceTween(currentLocation, playerLocation);

      // If distance is greater than threshold, target Pac-Man directly like Blinky
      if (distanceToPacman > DISTANCE_THRESHOLD) {
        return playerLocation;
      } else {
        // If too close to Pac-Man, retreat to scatter target
        return super.getScatterTarget();
      }
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
        int attempts = 0;

      while (randomLocation == null && attempts < MAX_ATTEMPTS) {
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
