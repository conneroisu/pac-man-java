package hw4;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;
import java.util.Random;

public class Blinky extends ActorImpl {

  public Blinky(
      MazeMap maze,
      Location home,
      double baseSpeed,
      Direction homeDirection,
      Location scatterTarget,
      Random rand) {
    super(maze, home, baseSpeed, homeDirection, scatterTarget, rand);
  }

  Location getTargetLocation(Descriptor desc) {
    if (super.getMode() == Mode.CHASE) {
      return desc.getPlayerLocation();
    } else if (super.getMode() == Mode.SCATTER) {
      return super.getScatterTarget();
    } else if (super.getMode() == Mode.FRIGHTENED) {
      // select a random location within the bounds of the play area
      Location randomLocation = null;
      int randomX = -1;
      int randomY = -1;
      while (randomLocation == null || maze.isWall(randomX, randomY)) {
        // get the width and height of the maze
        int width = maze.getNumColumns();
        int height = maze.getNumRows();
        // generate a random number between 0 and width of the maze
        randomX = rand.nextInt(width);
        // generate a random number between 0 and height of the maze
        randomY = rand.nextInt(height);
      }
      // create a new location with the random numbers
      randomLocation = new Location(randomX, randomY);
      // return the location
      return randomLocation;
    } else if (super.getMode() == Mode.INACTIVE) {
      return super.getCurrentLocation();
    } else {
      // get the location of the player from the current descriptor
      Location playerLocation = desc.getPlayerLocation();
      // RETURN the location of the player as the Target Location
      return playerLocation;
    }
  }
}
