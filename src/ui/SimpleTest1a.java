package ui;

import static api.Mode.*;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.PacmanGame;
import com.pacman.ghost.Blinky;

/** Simple test for Blinky behavior in different modes. */
public final class SimpleTest1a {

  /** Common maze border pattern. */
  private static final String MAZE_BORDER = "#######";
  /** Common open area line. */
  private static final String OPEN_AREA = "#.....#";
  /** Separator line for output formatting. */
  private static final String SEPARATOR_LINE = "----------";

  /** Private constructor to prevent instantiation of utility class. */
  private SimpleTest1a() {
    // Utility class should not be instantiated
  }

  public static final String[] SIMPLE1a = {
    MAZE_BORDER, OPEN_AREA, "#....S#", OPEN_AREA, OPEN_AREA, "#..B..#", OPEN_AREA, MAZE_BORDER,
  };

  /**
   * Main method to run tests.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(final String[] args) {
    // using a frame rate of 10, the speed increment will be 0.4
    PacmanGame game = new PacmanGame(SIMPLE1a, 10);

    // Blinky is always at index 0 in the enemies array
    Blinky b = (Blinky) game.getEnemies()[0];

    // verify initial state is INACTIVE
    if (Logger.isInfoEnabled()) {
      Logger.info("Check initial state");
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getMode() + ""); // INACTIVE
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getRowExact() + ", " + b.getColExact()); // 5.5, 3.5
    }

    // calculateNextCell does nothing when in INACTIVE mode
    b.calculateNextCell(makeDescriptor(game));
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getNextCell() + ""); // still null
    }

    // update does nothing when in INACTIVE mode
    b.update(makeDescriptor(game));
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getRowExact() + ", " + b.getColExact()); // still 5.5, 3.5
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(SEPARATOR_LINE);
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("");
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("Setting SCATTER mode");
    }
    // this should invoke calculateNextCell after setting mode
    b.setMode(SCATTER, null);
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getMode() + ""); // SCATTER
    }
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getNextCell() + ""); // expected (4, 3)
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("");
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("Do six calls to update():");
    }
    for (int i = 0; i < 6; ++i) {
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
    // Expected: should be at 3.1, 3.5 with next cell (2, 3) and direction UP
    if (Logger.isInfoEnabled()) {
      Logger.info(SEPARATOR_LINE);
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("");
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("Setting CHASE mode");
    }
    b.setMode(CHASE, makeDescriptor(game));
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getRowExact() + ", " + b.getColExact());
      Logger.info(b.getCurrentDirection() + "");
      Logger.info(b.getNextCell() + "");
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("");
    }
    // Expected: still at 3.1, 3.5 with next cell (2, 3) and direction UP
    // Our target in CHASE mode is now Pacman, at (2, 5), so going right to (3, 4) would
    // take us closer.  But since we are already past the center of cell (3, 3),
    // the implicit call to calculateNextCell from setMode does nothing (i.e, since
    // we are already past the center of our current cell, we can't change direction here)
    if (Logger.isInfoEnabled()) {
      Logger.info(SEPARATOR_LINE);
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("");
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("Do one call to update():");
    }
    b.update(makeDescriptor(game));
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getRowExact() + ", " + b.getColExact());
      Logger.info(b.getCurrentDirection() + "");
      Logger.info(b.getNextCell() + "");
    }
    // Expected: crossing into our next cell (2, 3) triggers a call to calculateNextCell.
    // Now the closest neighboring cell to target is to the right at (2, 4).
    // Current direction is still UP
    if (Logger.isInfoEnabled()) {
      Logger.info(SEPARATOR_LINE);
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("");
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("Do one call to update():");
    }
    b.update(makeDescriptor(game));
    if (Logger.isInfoEnabled()) {
      Logger.info(b.getRowExact() + ", " + b.getColExact());
      Logger.info(b.getCurrentDirection() + "");
      Logger.info(b.getNextCell() + "");
    }
    // Expected: since we have to change direction to go to next cell (2, 4), we
    // adjust the increment so as not to go past the center of the current cell,
    // so we end up at 2.5, 3.5 and the current direction is now RIGHT.
    if (Logger.isInfoEnabled()) {
      Logger.info(SEPARATOR_LINE);
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("");
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("Do three more calls to update():");
    }
    for (int i = 0; i < 3; ++i) {
      b.update(makeDescriptor(game));
      if (Logger.isInfoEnabled()) {
        Logger.info(b.getRowExact() + ", " + b.getColExact());
        Logger.info(b.getCurrentDirection() + "");
        Logger.info(b.getNextCell() + "");
      }
    }
    // Expected: should end up at 2.5, 4.7 with next cell (2, 5)
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
