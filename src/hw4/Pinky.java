package hw4;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import java.util.Random;

/**
 * Pinky (Pink Ghost) implementation.
 * Target: 4 tiles ahead of Pac-Man's current position and direction
 * Strategy: Attempts to position ahead of Pac-Man to trap him from the front
 * Note: Due to a bug in the original game, when Pac-Man faces upward, Pinky targets 
 * 4 tiles up and 4 tiles left of Pac-Man
 */
public class Pinky extends ActorImpl {

  public Pinky(
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
      Direction playerDirection = desc.getPlayerDirection();
      
      // Target 4 tiles ahead of Pac-Man
      int targetRow = playerLocation.row();
      int targetCol = playerLocation.col();
      
      // Determine the target location based on Pac-Man's direction
      // Note the "bug" when Pac-Man is facing UP
      if (playerDirection == Direction.UP) {
        // Classic bug in the original game - when facing up, 
        // target 4 tiles up and 4 tiles left
        targetRow -= 4;
        targetCol -= 4;
      } else if (playerDirection == Direction.DOWN) {
        targetRow += 4;
      } else if (playerDirection == Direction.LEFT) {
        targetCol -= 4;
      } else if (playerDirection == Direction.RIGHT) {
        targetCol += 4;
      }
      
      // Create a new location for the target
      return new Location(targetRow, targetCol);
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