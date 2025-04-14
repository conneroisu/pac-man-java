package ui;

import static api.Mode.*;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.PacmanGame;
import com.pacman.ghost.Blinky;

/** Some ideas for initially testing FRIGHTENED mode */
/**
 * Test class for ghost behavior in frightened mode.
 */
public final class SimpleTestFrightened {
  
  /**
   * Private constructor to prevent instantiation of utility class.
   */
  private SimpleTestFrightened() {
    // Utility class should not be instantiated
  }

  public static final String[] SIMPLE_FRIGHTENED = {
    "######", "#.####", "#B..S#", "#....#", "#....#", "#....#", "######",
  };

  /**
   * Main method to run the test.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(final String[] args) {
    // using a frame rate of 10, the speed increment will be 0.4
    PacmanGame game = new PacmanGame(SIMPLE_FRIGHTENED, 10);

    // Blinky is always at index 0 in the enemies array
    Blinky b = (Blinky) game.getEnemies()[0];

    System.out.println("Setting SCATTER mode");
    // this should invoke calculateNextCell after setting mode
    b.setMode(SCATTER, null);
    System.out.println(b.getMode()); // SCATTER
    System.out.println(b.getNextCell()); // expected (1, 1)
    System.out.println();

    System.out.println("Do one call to update():");
    b.update(makeDescriptor(game));
    System.out.println(b.getRowExact() + ", " + b.getColExact());
    System.out.println(b.getCurrentDirection());
    System.out.println(b.getNextCell());
    // Expected: coordinates 2.1, 1.5 and next cell (1, 1)
    System.out.println("----------");
    System.out.println();

    System.out.println("Setting FRIGHTENED mode");
    // since we are past the center of current cell, implicit call to calculateNextCell does nothing
    b.setMode(FRIGHTENED, makeDescriptor(game));
    System.out.println("Increment: " + b.getCurrentIncrement()); // expected ~0.267 (2/3 speed)
    System.out.println("Next cell: " + b.getNextCell()); // still (1, 1)
    System.out.println();

    System.out.println("Call update():");
    // crossing into next cell (1, 1) should trigger a calculateNextCell, but only the possible
    // next direction is below, so the "randomly" chosen next cell has to be (2, 1)
    b.update(makeDescriptor(game));
    System.out.println(b.getRowExact() + ", " + b.getColExact()); // expected ~1.83, 1.5
    System.out.println(b.getCurrentDirection()); // still UP
    System.out.println(b.getNextCell()); // now (2, 1)
    System.out.println("----------");

    // just as with a turn, we don't actually change direction until reaching the center
    // of the current cell
    System.out.println("Call update():");
    b.update(makeDescriptor(game));
    System.out.println(b.getRowExact() + ", " + b.getColExact()); // expected ~1.57, 1.5
    System.out.println(b.getCurrentDirection()); // still UP
    System.out.println(b.getNextCell()); // (2, 1)
    System.out.println("----------");

    System.out.println("Call update():");
    b.update(makeDescriptor(game));
    System.out.println(b.getRowExact() + ", " + b.getColExact()); // expected 1.5, 1.5
    System.out.println(b.getCurrentDirection()); // DOWN
    System.out.println(b.getNextCell()); // (2, 1)
    System.out.println("----------");

    System.out.println("Call update():");
    b.update(makeDescriptor(game));
    System.out.println(b.getRowExact() + ", " + b.getColExact()); // expected 1.77, 1.5
    System.out.println(b.getCurrentDirection()); // DOWN
    System.out.println(b.getNextCell()); // (2, 1)
    System.out.println("----------");

    System.out.println("Setting CHASE mode");
    b.setMode(CHASE, makeDescriptor(game));
    System.out.println("Increment: " + b.getCurrentIncrement()); // expected 0.4 (base speed)
    System.out.println();

    System.out.println("Call update():");
    // crossing into next cell triggers calculateNextCell, now minimizes distance
    // to Pacman by going right to (2, 2)
    b.update(makeDescriptor(game));
    System.out.println(b.getRowExact() + ", " + b.getColExact()); // expected ~2.17
    System.out.println(b.getCurrentDirection()); // DOWN
    System.out.println(b.getNextCell()); // (2, 2)
    System.out.println("----------");

    System.out.println("3 more calls to update():");
    for (int i = 0; i < 3; ++i) {
      b.update(makeDescriptor(game));
      System.out.println(b.getRowExact() + ", " + b.getColExact());
      System.out.println(b.getCurrentDirection());
      System.out.println(b.getNextCell());
    }
    // Expected: should be at 2.5, 2.3 with next cell (2, 3) and direction RIGHT
    System.out.println("----------");
    System.out.println();
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
