package ui;

import api.Actor;
import api.Location;
import api.PacmanGame;

/** Test to verify ghost spawn positions and initial movement. */
public final class GhostSpawnTest {

  /** Common maze layout line pattern. */
  private static final String MAZE_LAYOUT_LINE = "#.####.#####.##.#####.####.#";
  /** Cell type constants. */
  private static final String CELL_OPEN = "OPEN";
  private static final String CELL_WALL = "WALL";
  /** Wall character constant. */
  private static final char WALL_CHAR = '#';

  /** Minimum bounds check. */
  private static final int MINIMUM_BOUNDS = 0;

  /** Single decrement offset. */
  private static final int SINGLE_DECREMENT = 1;

  /** Ghost house start row. */
  private static final int GHOST_HOUSE_START_ROW = 10;

  /** Ghost house end row. */
  private static final int GHOST_HOUSE_END_ROW = 15;

  /** Private constructor to prevent instantiation. */
  private GhostSpawnTest() {
    // Utility class
  }

  public static void main(String[] args) {
    // Use the main maze from RunGame
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

    PacmanGame game = new PacmanGame(maze, 50);
    Actor[] ghosts = game.getEnemies();

    if (Logger.isInfoEnabled()) {
      Logger.info("=== Ghost Spawn Analysis ===");
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("Number of ghosts: " + ghosts.length);
    }

    // Check each ghost's spawn position
    for (int i = 0; i < ghosts.length; i++) {
      Actor ghost = ghosts[i];
      Location home = ghost.getHomeLocation();
      Location current = ghost.getCurrentLocation();

      if (Logger.isInfoEnabled()) {
        Logger.info("\nGhost " + i + " (" + ghost.getClass().getSimpleName() + "):");
      }
      if (Logger.isInfoEnabled()) {
        Logger.info("  Home location: " + home);
      }
      if (Logger.isInfoEnabled()) {
        Logger.info("  Current location: " + current);
      }
      if (Logger.isInfoEnabled()) {
        Logger.info("  Mode: " + ghost.getMode());
      }
      if (Logger.isInfoEnabled()) {
        Logger.info("  Direction: " + ghost.getCurrentDirection());
      }

      // Check surrounding cells at spawn position
      int row = home.row();
      int col = home.col();
      if (Logger.isInfoEnabled()) {
        Logger.info("  Surrounding cells at spawn:");
      }

      // Check all four directions
      boolean canMoveUp = row > MINIMUM_BOUNDS && !game.getCell(row - SINGLE_DECREMENT, col).isWall();
      boolean canMoveDown = row < game.getNumRows() - SINGLE_DECREMENT && !game.getCell(row + SINGLE_DECREMENT, col).isWall();
      boolean canMoveLeft = col > MINIMUM_BOUNDS && !game.getCell(row, col - SINGLE_DECREMENT).isWall();
      boolean canMoveRight = col < game.getNumColumns() - SINGLE_DECREMENT && !game.getCell(row, col + SINGLE_DECREMENT).isWall();

      if (Logger.isInfoEnabled()) {
        Logger.info("    UP: " + (canMoveUp ? CELL_OPEN : CELL_WALL));
      }
      if (Logger.isInfoEnabled()) {
        Logger.info("    DOWN: " + (canMoveDown ? CELL_OPEN : CELL_WALL));
      }
      if (Logger.isInfoEnabled()) {
        Logger.info("    LEFT: " + (canMoveLeft ? CELL_OPEN : CELL_WALL));
      }
      if (Logger.isInfoEnabled()) {
        Logger.info("    RIGHT: " + (canMoveRight ? CELL_OPEN : CELL_WALL));
      }

      if (!canMoveUp && !canMoveDown && !canMoveLeft && !canMoveRight) {
        Logger.error("  *** PROBLEM: Ghost is completely surrounded by walls! ***");
      }
    }

    // Visualize the ghost house area
    if (Logger.isInfoEnabled()) {
      Logger.info("\n=== Ghost House Area (rows 10-14) ===");
    }
    for (int row = GHOST_HOUSE_START_ROW; row < GHOST_HOUSE_END_ROW; row++) {
      StringBuilder rowOutput = new StringBuilder("Row " + row + ": ");
      for (int col = 0; col < game.getNumColumns(); col++) {
        char c = maze[row].charAt(col);
        if (c == WALL_CHAR) {
          rowOutput.append(WALL_CHAR);
        } else if (c == 'B' || c == 'P' || c == 'I' || c == 'C') {
          rowOutput.append(c);
        } else if (game.getCell(row, col).isWall()) {
          rowOutput.append('W'); // Wall that wasn't marked as '#'
        } else {
          rowOutput.append('.');
        }
      }
      if (Logger.isInfoEnabled()) {
        Logger.info(rowOutput.toString());
      }
    }

    // Check the actual cell types after parsing
    if (Logger.isInfoEnabled()) {
      Logger.info("\n=== Cell Types After Parsing (rows 10-14) ===");
    }
    for (int row = GHOST_HOUSE_START_ROW; row < GHOST_HOUSE_END_ROW; row++) {
      StringBuilder cellRowOutput = new StringBuilder("Row " + row + ": ");
      for (int col = 0; col < game.getNumColumns(); col++) {
        if (game.getCell(row, col).isWall()) {
          cellRowOutput.append('#');
        } else {
          cellRowOutput.append('.');
        }
      }
      if (Logger.isInfoEnabled()) {
        Logger.info(cellRowOutput.toString());
      }
    }
  }
}
