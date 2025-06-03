package ui;

import api.PacmanGame;
import api.Actor;
import api.Location;
import api.Mode;

/**
 * Test to verify ghost spawn positions and initial movement.
 */
public class GhostSpawnTest {
  
  public static void main(String[] args) {
    // Use the main maze from RunGame
    String[] maze = {
      "############################",
      "#............##............#",
      "#.####.#####.##.#####.####.#",
      "#*####.#####.##.#####.####*#",
      "#.####.#####.##.#####.####.#",
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
      "#.####.#####.##.#####.####.#",
      "#.####.#####.##.#####.####.#",
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
    
    System.out.println("=== Ghost Spawn Analysis ===");
    System.out.println("Number of ghosts: " + ghosts.length);
    
    // Check each ghost's spawn position
    for (int i = 0; i < ghosts.length; i++) {
      Actor ghost = ghosts[i];
      Location home = ghost.getHomeLocation();
      Location current = ghost.getCurrentLocation();
      
      System.out.println("\nGhost " + i + " (" + ghost.getClass().getSimpleName() + "):");
      System.out.println("  Home location: " + home);
      System.out.println("  Current location: " + current);
      System.out.println("  Mode: " + ghost.getMode());
      System.out.println("  Direction: " + ghost.getCurrentDirection());
      
      // Check surrounding cells at spawn position
      int row = home.row();
      int col = home.col();
      System.out.println("  Surrounding cells at spawn:");
      
      // Check all four directions
      boolean canMoveUp = row > 0 && !game.getCell(row - 1, col).isWall();
      boolean canMoveDown = row < game.getNumRows() - 1 && !game.getCell(row + 1, col).isWall();
      boolean canMoveLeft = col > 0 && !game.getCell(row, col - 1).isWall();
      boolean canMoveRight = col < game.getNumColumns() - 1 && !game.getCell(row, col + 1).isWall();
      
      System.out.println("    UP: " + (canMoveUp ? "OPEN" : "WALL"));
      System.out.println("    DOWN: " + (canMoveDown ? "OPEN" : "WALL"));
      System.out.println("    LEFT: " + (canMoveLeft ? "OPEN" : "WALL"));
      System.out.println("    RIGHT: " + (canMoveRight ? "OPEN" : "WALL"));
      
      if (!canMoveUp && !canMoveDown && !canMoveLeft && !canMoveRight) {
        System.out.println("  *** PROBLEM: Ghost is completely surrounded by walls! ***");
      }
    }
    
    // Visualize the ghost house area
    System.out.println("\n=== Ghost House Area (rows 10-14) ===");
    for (int row = 10; row < 15; row++) {
      System.out.print("Row " + row + ": ");
      for (int col = 0; col < game.getNumColumns(); col++) {
        char c = maze[row].charAt(col);
        if (c == '#') {
          System.out.print('#');
        } else if (c == 'B' || c == 'P' || c == 'I' || c == 'C') {
          System.out.print(c);
        } else if (game.getCell(row, col).isWall()) {
          System.out.print('W'); // Wall that wasn't marked as '#'
        } else {
          System.out.print('.');
        }
      }
      System.out.println();
    }
    
    // Check the actual cell types after parsing
    System.out.println("\n=== Cell Types After Parsing (rows 10-14) ===");
    for (int row = 10; row < 15; row++) {
      System.out.print("Row " + row + ": ");
      for (int col = 0; col < game.getNumColumns(); col++) {
        if (game.getCell(row, col).isWall()) {
          System.out.print('#');
        } else {
          System.out.print('.');
        }
      }
      System.out.println();
    }
  }
}