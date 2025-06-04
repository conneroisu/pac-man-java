package ui;

import api.Actor;
import api.Descriptor;
import api.Direction;
import api.Location;
import api.Mode;
import api.PacmanGame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Analyze ghost movement to detect stuck patterns. */
public final class GhostStuckAnalysis {

  /** Minimum data points for analysis. */
  private static final int MINIMUM_DATA_POINTS = 50;

  /** Pattern repeat threshold. */
  private static final int PATTERN_REPEAT_THRESHOLD = 5;

  /** Movement variability divisor. */
  private static final int MOVEMENT_VARIABILITY_DIVISOR = 10;

  /** Stuck counter threshold. */
  private static final int STUCK_COUNTER_THRESHOLD = 10;

  /** Status report interval. */
  private static final int STATUS_REPORT_INTERVAL = 10;

  /** Common maze layout line pattern. */
  private static final String MAZE_LAYOUT_LINE = "#.####.#####.##.#####.####.#";

  /** Private constructor to prevent instantiation. */
  private GhostStuckAnalysis() {
    // Utility class
  }

  public static void main(String[] args) {
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

    // Track ghost positions over time
    Map<Actor, List<Location>> positionHistory = new HashMap<>();
    Map<Actor, List<Direction>> directionHistory = new HashMap<>();

    for (Actor ghost : ghosts) {
      positionHistory.put(ghost, new ArrayList<>());
      directionHistory.put(ghost, new ArrayList<>());
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("=== Running simulation to detect stuck ghosts ===");
    }

    // Run for 1000 frames (20 seconds)
    for (int frame = 0; frame < 1000; frame++) {
      game.updateAll();

      // Record positions and directions
      for (Actor ghost : ghosts) {
        if (ghost.getMode() != Mode.INACTIVE) {
          positionHistory.get(ghost).add(ghost.getCurrentLocation());
          directionHistory.get(ghost).add(ghost.getCurrentDirection());
        }
      }
    }

    // Analyze for stuck patterns
    if (Logger.isInfoEnabled()) {
      Logger.info("\n=== Stuck Pattern Analysis ===");
    }

    for (int i = 0; i < ghosts.length; i++) {
      Actor ghost = ghosts[i];
      List<Location> positions = positionHistory.get(ghost);
      List<Direction> directions = directionHistory.get(ghost);

      if (positions.size() < MINIMUM_DATA_POINTS) continue; // Skip if not enough data

      if (Logger.isInfoEnabled()) {
        Logger.info("\nGhost " + i + " (" + ghost.getClass().getSimpleName() + "):");
      }

      // Check for oscillation patterns
      Map<String, Integer> patternCount = new HashMap<>();

      // Look for 2-cell oscillations
      for (int j = 0; j < positions.size() - 3; j++) {
        Location loc1 = positions.get(j);
        Location loc2 = positions.get(j + 1);
        Location loc3 = positions.get(j + 2);
        Location loc4 = positions.get(j + 3);

        if (loc1.equals(loc3) && loc2.equals(loc4) && !loc1.equals(loc2)) {
          String pattern = loc1 + "->" + loc2;
          patternCount.put(pattern, patternCount.getOrDefault(pattern, 0) + 1);
        }
      }

      // Look for 3-cell cycles
      for (int j = 0; j < positions.size() - 5; j++) {
        Location loc1 = positions.get(j);
        Location loc2 = positions.get(j + 1);
        Location loc3 = positions.get(j + 2);
        Location loc4 = positions.get(j + 3);
        Location loc5 = positions.get(j + 4);
        Location loc6 = positions.get(j + 5);

        if (loc1.equals(loc4) && loc2.equals(loc5) && loc3.equals(loc6)) {
          String pattern = loc1 + "->" + loc2 + "->" + loc3;
          patternCount.put(pattern, patternCount.getOrDefault(pattern, 0) + 1);
        }
      }

      // Report patterns found
      boolean isStuck = false;
      for (Map.Entry<String, Integer> entry : patternCount.entrySet()) {
        if (entry.getValue() > PATTERN_REPEAT_THRESHOLD) { // Pattern repeated more than threshold times
          if (Logger.isInfoEnabled()) {
            Logger.info(
                "  STUCK PATTERN: " + entry.getKey() + " (repeated " + entry.getValue() + " times)");
          }
          isStuck = true;
        }
      }

      if (!isStuck) {
        // Check if ghost hasn't moved much
        int uniquePositions = (int) positions.stream().distinct().count();
        if (Logger.isInfoEnabled()) {
          Logger.info(
              "  Visited "
                  + uniquePositions
                  + " unique positions in "
                  + positions.size()
                  + " frames");
        }

        if (uniquePositions < positions.size() / MOVEMENT_VARIABILITY_DIVISOR && Logger.isInfoEnabled()) {
          Logger.info("  PROBLEM: Ghost is not exploring maze effectively!");
        }
      }

      // Show last few positions
      if (Logger.isInfoEnabled()) {
        Logger.info("  Last 10 positions:");
      }
      int start = Math.max(0, positions.size() - 10);
      for (int j = start; j < positions.size(); j++) {
        if (Logger.isInfoEnabled()) {
          Logger.info("    " + positions.get(j) + " (dir: " + directions.get(j) + ")");
        }
      }
    }

    // Let's also manually check a specific ghost's movement
    if (Logger.isInfoEnabled()) {
      Logger.info("\n=== Manual Movement Test for Blinky ===");
    }
    Actor blinky = ghosts[0];

    // Reset the game
    game.resetAll();

    // Manually activate Blinky
    Descriptor desc =
        new Descriptor(
            game.getPlayer().getCurrentLocation(),
            game.getPlayer().getCurrentDirection(),
            blinky.getCurrentLocation());
    blinky.setMode(Mode.SCATTER, desc);

    if (Logger.isInfoEnabled()) {
      Logger.info("Initial state:");
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("  Location: " + blinky.getCurrentLocation());
    }
    if (Logger.isInfoEnabled()) {
      Logger.info("  Direction: " + blinky.getCurrentDirection());
    }

    // Track movement for a few updates
    Location lastLoc = blinky.getCurrentLocation();
    int stuckCounter = 0;

    for (int i = 0; i < 100; i++) {
      blinky.update(desc);
      Location currentLoc = blinky.getCurrentLocation();

      if (currentLoc.equals(lastLoc)) {
        stuckCounter++;
      } else {
        if (stuckCounter > STUCK_COUNTER_THRESHOLD && Logger.isInfoEnabled()) {
          Logger.info(
              "  Ghost was stuck at " + lastLoc + " for " + stuckCounter + " frames!");
        }
        stuckCounter = 0;
      }

      if ((i % STATUS_REPORT_INTERVAL == 0 || !currentLoc.equals(lastLoc)) && Logger.isInfoEnabled()) {
        Logger.info(
            "  Frame " + i + ": " + currentLoc + " (dir: " + blinky.getCurrentDirection() + ")");
      }

      lastLoc = currentLoc;
    }

    if (stuckCounter > 10 && Logger.isInfoEnabled()) {
      Logger.info(
          "  Ghost is currently stuck at " + lastLoc + " for " + stuckCounter + " frames!");
    }
  }
}
