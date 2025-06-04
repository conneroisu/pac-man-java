package ui;

// Ghost mode imports

import api.Descriptor;
import api.Direction;
import api.Location;
import api.PacmanGame;
import com.pacman.ghost.Blinky;

// NOTE: The tests below assume that you have a bogus, temporary implementation
// calculateNextCell in your Blinky class, like this:
//
// public void calculateNextCell(Descriptor d)
// {
//  Location currentLoc = getCurrentLocation();
//
//  if (currentLoc.row() > 2)
//  {
//    nextLoc = new Location(currentLoc.row() - 1, currentLoc.col());
//    nextDir = UP;
//  }
//  else
//  {
//    nextLoc = new Location(currentLoc.row(), currentLoc.col() + 1);
//    nextDir = RIGHT;
//  }
// }
//

/** Simple test class to verify Blinky movement. */
public final class SimpleTest {

  /** Common maze border pattern. */
  private static final String MAZE_BORDER = "#######";
  /** Common open area line. */
  private static final String OPEN_AREA = "#.....#";

  /** Private constructor to prevent instantiation of utility class. */
  private SimpleTest() {
    // Utility class should not be instantiated
  }

  /** Test maze with simple layout. */
  public static final String[] SIMPLE1 = {
    MAZE_BORDER, OPEN_AREA, OPEN_AREA, OPEN_AREA, OPEN_AREA, "#..B..#", "#S....#", MAZE_BORDER
  };

  /**
   * Main method to run the test.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(final String[] args) {
    // using a frame rate of 10, the speed increment will be 0.4
    final int frameRate = 10;
    PacmanGame game = new PacmanGame(SIMPLE1, frameRate);

    // Blinky is always at index 0 in the enemies array
    Blinky b = (Blinky) game.getEnemies()[0];

    if (Logger.isInfoEnabled()) {
      Logger.info(b.getCurrentLocation() + ""); // expected (5, 3)
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getCurrentIncrement() + ""); // expected 0.4
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getCurrentDirection() + ""); // expected UP
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getRowExact() + ", " + b.getColExact()); // expected (5.5, 3.5)
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getNextCell() + ""); // expected null
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("");
    }

    // this should update value of getNextCell()
    b.calculateNextCell(makeDescriptor(game));
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getNextCell() + ""); // expected (4, 3)
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("");
    }

    // now some updates
    b.update(makeDescriptor(game));
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getRowExact() + ", " + b.getColExact()); // ~5.1, 3.5
    }
    b.update(makeDescriptor(game));
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getRowExact() + ", " + b.getColExact()); // ~4.7, 3.5
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("");
    }

    // lots of updates!  See the pdf for expected output
    final int numUpdates = 10;
    for (int i = 0; i < numUpdates; ++i) {
      b.update(makeDescriptor(game));
      if (Logger.isInfoEnabled()) {
        Logger.info(b.getRowExact() + ", " + b.getColExact());
      }
      if (Logger.isInfoEnabled()) {
        Logger.info(b.getCurrentDirection() + "");
      }
      if (Logger.isInfoEnabled()) {
        Logger.info(b.getNextCell() + "");
      }
    }
  }

  /**
   * Creates a game descriptor for use in testing.
   *
   * @param game The game to create a descriptor for
   * @return A new descriptor with player and enemy information
   */
  public static Descriptor makeDescriptor(final PacmanGame game) {
    Location enemyLoc = game.getEnemies()[0].getCurrentLocation();
    Location playerLoc = game.getPlayer().getCurrentLocation();
    Direction playerDir = game.getPlayer().getCurrentDirection();
    return new Descriptor(playerLoc, playerDir, enemyLoc);
  }
}
