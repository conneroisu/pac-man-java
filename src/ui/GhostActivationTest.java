package ui;

import api.Actor;
import api.Descriptor;
import api.Direction;
import api.Location;
import api.Mode;
import api.PacmanGame;

/** Test ghost activation and initial movement. */
public final class GhostActivationTest {

  /** Common maze layout line pattern. */
  private static final String MAZE_LAYOUT_LINE = "#.####.#####.##.#####.####.#";

  /** Status report interval. */
  private static final int STATUS_REPORT_INTERVAL = 50;

  /** Zero comparison. */
  private static final int ZERO_COMPARISON = 0;

  /** Private constructor to prevent instantiation. */
  private GhostActivationTest() {
    // Utility class
  }

  public static void main(String[] args) {
    PacmanGame game = getPacmanGame();
    Actor[] ghosts = game.getEnemies();

    if (Logger.isInfoEnabled()) {
      Logger.info("=== Initial State ===");
    }
    printGhostStates(ghosts);

    // Simulate some frames to see when ghosts activate
    if (Logger.isInfoEnabled()) {
      Logger.info("\n=== Simulating frames to observe ghost activation ===");
    }

    // Run for 250 frames (5 seconds at 50 fps) to see Blinky activate
    for (int frame = 1; frame <= 250; frame++) {
      game.updateAll();

      // Check for mode changes
      boolean anyChange = false;
      for (Actor ghost : ghosts) {
        if (ghost.getMode() != Mode.INACTIVE) {
          anyChange = true;
        }
      }

      if (anyChange || frame % STATUS_REPORT_INTERVAL == ZERO_COMPARISON) {
        if (Logger.isInfoEnabled()) {
          Logger.info("\nFrame " + frame + " (t=" + (frame / 50.0) + "s):");
        }
        printGhostStates(ghosts);
      }
    }

    // Now let's manually activate a ghost and see what happens
    if (Logger.isInfoEnabled()) {
      Logger.info("\n=== Manually activating Blinky to test movement ===");
    }
    Actor blinky = ghosts[0];

    // Create a descriptor for the game state
    Location playerLoc = game.getPlayer().getCurrentLocation();
    Direction playerDir = game.getPlayer().getCurrentDirection();
    Descriptor desc = new Descriptor(playerLoc, playerDir, blinky.getCurrentLocation());

    // Force Blinky to SCATTER mode
    blinky.setMode(Mode.SCATTER, desc);

    if (Logger.isInfoEnabled()) {
      Logger.info("\nAfter setting Blinky to SCATTER mode:");
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("  Mode: " + blinky.getMode());
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("  Location: " + blinky.getCurrentLocation());
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("  Exact: (" + blinky.getRowExact() + ", " + blinky.getColExact() + ")");
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("  Direction: " + blinky.getCurrentDirection());
    }

    // Try to update Blinky a few times
    if (Logger.isInfoEnabled()) {
      Logger.info("\nUpdating Blinky 10 times:");
    }
    for (int i = 0; i < 10; i++) {
      blinky.update(desc);
      if (Logger.isInfoEnabled()) {
        Logger.info(
            "  Update "
                + i
                + ": Location="
                + blinky.getCurrentLocation()
                + ", Exact=("
                + String.format("%.2f", blinky.getRowExact())
                + ", "
                + String.format("%.2f", blinky.getColExact())
                + "), Dir="
                + blinky.getCurrentDirection());
      }
    }
  }

  private static PacmanGame getPacmanGame() {
    String[] maze = {
      "############################",
      "#............##............#",
      MAZE_LAYOUT_LINE,
      "#*####.#####.##.#####.####*#",
      MAZE_LAYOUT_LINE,
      "#..........................#",
      "#.####.##.########.##.####.#",
      "#.####.##.########.##.####.#",
      "#......##....##....##......#",
      "######.##### ## #####.######",
      "     #.##### ## #####.#     ",
      "     #.##          ##.#     ",
      "     #.## ##BPIC## ##.#     ",
      "######.## ######## ##.######",
      "      .   ##    ##   .      ",
      "######.## ######## ##.######",
      "     #.## ######## ##.#     ",
      "     #.##          ##.#     ",
      "     #.## ######## ##.#     ",
      "######.## ######## ##.######",
      "#............##............#",
      MAZE_LAYOUT_LINE,
      MAZE_LAYOUT_LINE,
      "#*..##................##..*#",
      "###.##.##.########.##.##.###",
      "###.##.##.########.##.##.###",
      "#......##...S##....##......#",
      "#.##########.##.##########.#",
      "#.##########.##.##########.#",
      "#..........................#",
      "############################",
    };

    return new PacmanGame(maze, 50);
  }

  private static void printGhostStates(Actor... ghosts) {
    for (int i = 0; i < ghosts.length; i++) {
      Actor ghost = ghosts[i];
      if (Logger.isInfoEnabled()) {
        Logger.info(
            "  Ghost "
                + i
                + " ("
                + ghost.getClass().getSimpleName()
                + "): "
                + "Mode="
                + ghost.getMode()
                + ", "
                + "Loc="
                + ghost.getCurrentLocation()
                + ", "
                + "Dir="
                + ghost.getCurrentDirection());
      }
    }
  }
}
