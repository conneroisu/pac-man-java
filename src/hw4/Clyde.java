package hw4;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import java.util.Random;

/**
 * Clyde (Orange Ghost) implementation.
 * Personality: Shy, keeps distance
 * Target: Alternates between targeting Pac-Man and his scatter corner
 * Strategy: 
 *   - If Clyde is more than 8 tiles away from Pac-Man, he targets Pac-Man directly (like Blinky)
 *   - If Clyde is within 8 tiles of Pac-Man, he retreats to his scatter corner
 * Result: Clyde tends to approach and then flee, creating an on-again-off-again pursuit
 */
public class Clyde extends ActorImpl {

  // Distance threshold for Clyde's behavior (8 tiles)
  private static final double DISTANCE_THRESHOLD = 8.0;

  public Clyde(
      MazeMap maze,
      Location home,
      double baseSpeed,
      Direction homeDirection,
      Location scatterTarget,
      Random rand) {
    super(maze, home, baseSpeed, homeDirection, scatterTarget, rand);
  }

  @Override
  Location getTargetLocation(Descriptor desc) {
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
      // Select a random location within the bounds of the play area
      Location randomLocation = null;
      int randomRow = -1;
      int randomCol = -1;
      while (randomLocation == null || maze.isWall(randomRow, randomCol)) {
        // Get the width and height of the maze
        int width = maze.getNumColumns();
        int height = maze.getNumRows();
        // Generate a random number between 0 and width of the maze
        randomCol = rand.nextInt(width);
        // Generate a random number between 0 and height of the maze
        randomRow = rand.nextInt(height);
      }
      // Create a new location with the random numbers
      randomLocation = new Location(randomRow, randomCol);
      // Return the location
      return randomLocation;
    } else if (super.getMode() == Mode.INACTIVE) {
      return super.getCurrentLocation();
    } else { // DEAD mode
      // Return home location when in DEAD mode
      return super.getHomeLocation();
    }
  }
}