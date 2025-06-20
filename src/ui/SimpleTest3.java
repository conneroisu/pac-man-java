package ui;

import api.Mode;
import api.PacmanGame;
import com.pacman.ghost.Blinky;

/** More complete test for Blinky behavior with different modes. */
public final class SimpleTest3 {

  /** Private constructor to prevent instantiation of utility class. */
  private SimpleTest3() {
    // Utility class should not be instantiated
  }

  // one ghost
  /** Simple test maze with one ghost. */
  public static final String[] SIMPLE3 = {
    "######", "#....#", "#.B..#", "#....#", "#S...#", "######",
  };

  /**
   * Main method to run the test.
   *
   * @param args Command line arguments (not used)
   */
  @SuppressWarnings("PMD.GuardLogStatement")
  public static void main(final String[] args) {

    // using a frame rate of 10, the speed increment will be 0.4
    final int frameRate = 10;
    PacmanGame game = new PacmanGame(SIMPLE3, frameRate);

    // Blinky is always at index 0 in the enemies array
    Blinky b = (Blinky) game.getEnemies()[0];

    // Blinky is at (2, 2) and his scatter target is
    // at -3, 3, so moving up to (1, 2) will minimize
    // the straight-line distance to the target
    b.setMode(Mode.SCATTER, SimpleTest.makeDescriptor(game));
    Logger.info(b.getNextCell() + ""); // expected (1, 2)
    Logger.info("");

    // update should move up 0.4 units
    b.update(SimpleTest.makeDescriptor(game));
    Logger.info(b.getCurrentDirection() + ""); // UP
    Logger.info(b.getRowExact() + ", " + b.getColExact()); // now 2.1, 2.5
    Logger.info("");

    // now, if we change the mode to CHASE, since we are already past the midpoint of
    // the current cell in direction UP, the implicit call to calculateNextCell
    // should not do anything
    b.setMode(Mode.CHASE, SimpleTest.makeDescriptor(game));
    Logger.info(b.getNextCell() + ""); // still (1, 2)
    Logger.info(b.getCurrentDirection() + ""); // still UP
    Logger.info(b.getRowExact() + ", " + b.getColExact()); // still 2.1, 2.5
    Logger.info("");

    // but when we update again, we cross the boundary into (1, 2), which
    // triggers a call to calculateNextCell.  Since the mode is now
    // CHASE, the target is now at (4, 1).  We can't go down, which would be a
    // reversal of direction, so the neighboring cell that is closest to the
    // target is to the left at (1, 1)
    b.update(SimpleTest.makeDescriptor(game));
    Logger.info(b.getNextCell() + ""); // expected (1, 1)
    Logger.info(b.getCurrentDirection() + ""); // still UP
    Logger.info(b.getRowExact() + ", " + b.getColExact()); // 1.7, 2.5
    Logger.info("");

    // next update should continue up to center of cell at 1.5 and then change direction
    b.update(SimpleTest.makeDescriptor(game));
    Logger.info(b.getNextCell() + ""); // expected 1, 1
    Logger.info(b.getCurrentDirection() + ""); // expected LEFT
    Logger.info(b.getRowExact() + ", " + b.getColExact()); // 1.5, 2.5
    Logger.info("");
  }
}
