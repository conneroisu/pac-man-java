package ui;

import api.PacmanGame;
import api.Actor;
import api.Location;
import api.Mode;
import api.Direction;
import api.Descriptor;

/**
 * Test ghost activation and initial movement.
 */
public class GhostActivationTest {
  
  public static void main(String[] args) {
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
    
    System.out.println("=== Initial State ===");
    printGhostStates(ghosts);
    
    // Simulate some frames to see when ghosts activate
    System.out.println("\n=== Simulating frames to observe ghost activation ===");
    
    // Run for 250 frames (5 seconds at 50 fps) to see Blinky activate
    for (int frame = 1; frame <= 250; frame++) {
      game.updateAll();
      
      // Check for mode changes
      boolean anyChange = false;
      for (int i = 0; i < ghosts.length; i++) {
        if (ghosts[i].getMode() != Mode.INACTIVE) {
          anyChange = true;
        }
      }
      
      if (anyChange || frame % 50 == 0) {
        System.out.println("\nFrame " + frame + " (t=" + (frame/50.0) + "s):");
        printGhostStates(ghosts);
      }
    }
    
    // Now let's manually activate a ghost and see what happens
    System.out.println("\n=== Manually activating Blinky to test movement ===");
    Actor blinky = ghosts[0];
    
    // Create a descriptor for the game state
    Location playerLoc = game.getPlayer().getCurrentLocation();
    Direction playerDir = game.getPlayer().getCurrentDirection();
    Descriptor desc = new Descriptor(playerLoc, playerDir, blinky.getCurrentLocation());
    
    // Force Blinky to SCATTER mode
    blinky.setMode(Mode.SCATTER, desc);
    
    System.out.println("\nAfter setting Blinky to SCATTER mode:");
    System.out.println("  Mode: " + blinky.getMode());
    System.out.println("  Location: " + blinky.getCurrentLocation());
    System.out.println("  Exact: (" + blinky.getRowExact() + ", " + blinky.getColExact() + ")");
    System.out.println("  Direction: " + blinky.getCurrentDirection());
    
    // Try to update Blinky a few times
    System.out.println("\nUpdating Blinky 10 times:");
    for (int i = 0; i < 10; i++) {
      blinky.update(desc);
      System.out.println("  Update " + i + ": Location=" + blinky.getCurrentLocation() + 
                         ", Exact=(" + String.format("%.2f", blinky.getRowExact()) + ", " + 
                         String.format("%.2f", blinky.getColExact()) + "), Dir=" + 
                         blinky.getCurrentDirection());
    }
  }
  
  private static void printGhostStates(Actor[] ghosts) {
    for (int i = 0; i < ghosts.length; i++) {
      Actor ghost = ghosts[i];
      System.out.println("  Ghost " + i + " (" + ghost.getClass().getSimpleName() + "): " +
                         "Mode=" + ghost.getMode() + ", " +
                         "Loc=" + ghost.getCurrentLocation() + ", " +
                         "Dir=" + ghost.getCurrentDirection());
    }
  }
}