package ui;

import api.PacmanGame;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class RunGame {
  
  /** Common maze border pattern. */
  private static final String MAZE_BORDER = "#######";
  /** Common maze layout line pattern. */
  private static final String MAZE_LAYOUT_LINE = "#.####.#####.##.#####.####.#";
  /** Common corridor pattern. */
  private static final String CORRIDOR_PATTERN = "#.#.#.#";
  /** Common maze line for main game. */
  private static final String OPEN_AREA_LINE = "#.....#";

  /** Private constructor to prevent instantiation. */
  private RunGame() {
    // Utility class
  }
  // no ghost
  public static final String[] TEST0 = {
    MAZE_BORDER, "#*...*#", "#.###.#", OPEN_AREA_LINE, CORRIDOR_PATTERN, CORRIDOR_PATTERN, "#..S..#", MAZE_BORDER,
  };

  // one ghost
  public static final String[] TEST1 = {
    MAZE_BORDER, "#*...*#", "#.#B#.#", OPEN_AREA_LINE, CORRIDOR_PATTERN, CORRIDOR_PATTERN, "#..S..#", MAZE_BORDER,
  };

  // one ghost, and a tunnel
  public static final String[] TEST2 = {
    MAZE_BORDER, "#*...*#", "#.###.#", " ..B.. ", CORRIDOR_PATTERN, CORRIDOR_PATTERN, "#..S..#", MAZE_BORDER,
  };

  // the classic Pacman maze
  public static final String[] MAIN1 = {
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

  public static void main(String[] args) {
    final PacmanGame maze = new PacmanGame(MAIN1, 50);
    Runnable r =
        new Runnable() {
          @Override
          public void run() {
            createAndShow(maze);
          }
        };
    SwingUtilities.invokeLater(r);
  }

  private static void createAndShow(final PacmanGame maze) {

    // create the frame
    JFrame frame = new JFrame("Nonbinary Pac-person");

    // create an instance of our JPanel subclass and
    // add it to the frame
    PacmanPanel panel = new PacmanPanel(maze);
    frame.getContentPane().add(panel);
    panel.setPreferredSize(
        new Dimension(
            maze.getNumColumns() * PacmanPanel.CELL_SIZE,
            maze.getNumRows() * PacmanPanel.CELL_SIZE));

    // give it a nonzero size
    frame.pack();

    // we want to shut down the application if the
    // "close" button is pressed on the frame
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // make the frame visible and start the UI machinery
    frame.setVisible(true);

    // make sure panel gets key events
    panel.grabFocus();
  }
}
