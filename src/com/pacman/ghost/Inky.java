package com.pacman.ghost;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import java.util.Random;

/**
 * Inky (Blue Ghost) implementation. Personality: Unpredictable, wild card Target: Complex
 * calculation using both Pac-Man and Blinky Strategy: 1. Identify a point 2 tiles ahead of Pac-Man
 * 2. Draw a vector from Blinky's position to this point 3. Double the length of this vector to find
 * Inky's target tile Result: Creates unpredictable flanking movements that can surprise players
 */
public final class Inky extends ActorImpl {

  /** Maximum number of attempts to find a random location. */
  private static final int MAX_ATTEMPTS = 20;

  /** Number of tiles Inky targets ahead of Pacman for intermediate point. */
  private static final int INTERMEDIATE_DISTANCE = 2;

  /**
   * Constructor for Inky ghost.
   *
   * @param maze The maze map
   * @param home The home/starting location
   * @param baseSpeed The base movement speed
   * @param homeDirection The initial direction
   * @param scatterTarget The target location during scatter mode
   * @param rand Random number generator for movement
   */
  public Inky(
      final MazeMap maze,
      final Location home,
      final double baseSpeed,
      final Direction homeDirection,
      final Location scatterTarget,
      final Random rand) {
    super(maze, home, baseSpeed, homeDirection, scatterTarget, rand);
  }

  /**
   * Gets the target location based on the current mode. When in CHASE mode, uses a complex vector
   * calculation based on both Pac-Man's and Blinky's positions.
   *
   * @param desc The game descriptor containing game state
   * @return The target location
   */
  @Override
  protected Location getTargetLocation(final Descriptor desc) {
    if (super.getMode() == Mode.CHASE) {
      Location playerLocation = desc.getPlayerLocation();
      Direction playerDirection = desc.getPlayerDirection();
      Location blinkyLocation = desc.getBlinkyLocation();

      // First, find the point 2 tiles ahead of Pac-Man
      int intermediateRow = playerLocation.row();
      int intermediateCol = playerLocation.col();

      // Calculate intermediate point based on Pac-Man's direction
      // With the same "bug" as Pinky when facing UP
      if (playerDirection == Direction.UP) {
        // Apply the same bug as Pinky but with only 2 tiles
        intermediateRow -= INTERMEDIATE_DISTANCE;
        intermediateCol -= INTERMEDIATE_DISTANCE;
      } else if (playerDirection == Direction.DOWN) {
        intermediateRow += INTERMEDIATE_DISTANCE;
      } else if (playerDirection == Direction.LEFT) {
        intermediateCol -= INTERMEDIATE_DISTANCE;
      } else if (playerDirection == Direction.RIGHT) {
        intermediateCol += INTERMEDIATE_DISTANCE;
      }

      // Calculate the vector from Blinky to the intermediate point
      int vectorRow = intermediateRow - blinkyLocation.row();
      int vectorCol = intermediateCol - blinkyLocation.col();

      // Double the vector to get Inky's target
      int targetRow = intermediateRow + vectorRow;
      int targetCol = intermediateCol + vectorCol;

      return new Location(targetRow, targetCol);
    } else if (super.getMode() == Mode.SCATTER) {
      return super.getScatterTarget();
    } else if (super.getMode() == Mode.FRIGHTENED) {
      // In FRIGHTENED mode, generate a valid random target location
      // This is just a target for decision making at intersections
      // Actual movement will use the handler in ActorImpl

      // Get maze dimensions
      final int width = maze.getNumColumns();
      final int height = maze.getNumRows();

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
