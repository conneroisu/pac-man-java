package hw4;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import java.util.Random;

/**
 * Inky (Blue Ghost) implementation.
 * Target: Complex calculation using both Pac-Man and Blinky
 * Strategy: 1. Identify a point 2 tiles ahead of Pac-Man
 *           2. Draw a vector from Blinky's position to this point
 *           3. Double the length of this vector to find Inky's target tile
 * Result: Creates unpredictable flanking movements that can surprise players
 */
public class Inky extends ActorImpl {

  public Inky(
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
      Location blinkyLocation = desc.getBlinkyLocation();
      
      // First, find the point 2 tiles ahead of Pac-Man
      int intermediateRow = playerLocation.row();
      int intermediateCol = playerLocation.col();
      
      // Calculate intermediate point based on Pac-Man's direction
      // With the same "bug" as Pinky when facing UP
      if (playerDirection == Direction.UP) {
        // Apply the same bug as Pinky but with only 2 tiles
        intermediateRow -= 2;
        intermediateCol -= 2;
      } else if (playerDirection == Direction.DOWN) {
        intermediateRow += 2;
      } else if (playerDirection == Direction.LEFT) {
        intermediateCol -= 2;
      } else if (playerDirection == Direction.RIGHT) {
        intermediateCol += 2;
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