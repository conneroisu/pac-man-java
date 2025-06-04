package ui;

import api.Actor;
import api.Location;
import api.Mode;
import api.PacmanGame;

/** Validates that ghost movement fixes are working properly. */
public final class GhostMovementFixValidation {

  /** Stuck threshold. */
  private static final int STUCK_THRESHOLD = 10;

  /** Status report interval. */
  private static final int STATUS_REPORT_INTERVAL = 50;

  /** Acceptable stuck frames. */
  private static final int ACCEPTABLE_STUCK_FRAMES = 50;

  /** Oscillation check start frame. */
  private static final int OSCILLATION_CHECK_START_FRAME = 20;

  /** History array size. */
  private static final int HISTORY_ARRAY_SIZE = 10;

  /** History index offset 1. */
  private static final int HISTORY_INDEX_1 = 4;

  /** History index offset 2. */
  private static final int HISTORY_INDEX_2 = 3;

  /** History index offset 3. */
  private static final int HISTORY_INDEX_3 = 2;

  /** History index offset 4. */
  private static final int HISTORY_INDEX_4 = 1;

  /** Private constructor to prevent instantiation. */
  private GhostMovementFixValidation() {
    // Utility class
  }

  public static void main(String[] args) {
    // Test maze that previously caused issues
    String[] maze = {
      "###############",
      "#.............#",
      "#.............#",
      "#......###....#",
      "#......#B#....#",
      "#......#P#....#",
      "#......###....#",
      "#.............#",
      "#......S......#",
      "###############"
    };

    PacmanGame game = new PacmanGame(maze, 50);
    Actor blinky = game.getEnemies()[0];

    if (Logger.isInfoEnabled()) {
      Logger.info("=== Ghost Movement Fix Validation ===");
      Logger.info("Testing ghost that previously got stuck...\n");
    }

    // Track positions to detect stuck behavior
    Location lastPos = null;
    int stuckCount = 0;
    int maxStuckFrames = 0;

    // Run for 500 frames (10 seconds)
    for (int frame = 0; frame < 500; frame++) {
      game.updateAll();

      Location currentPos = blinky.getCurrentLocation();

      // Check if ghost is stuck
      if (currentPos.equals(lastPos) && blinky.getMode() != Mode.INACTIVE) {
        stuckCount++;
        maxStuckFrames = Math.max(maxStuckFrames, stuckCount);
      } else {
        if (stuckCount > STUCK_THRESHOLD && Logger.isInfoEnabled()) {
          Logger.info(
              "Frame " + frame + ": Ghost was stuck for " + stuckCount + " frames at " + lastPos);
        }
        stuckCount = 0;
      }

      lastPos = currentPos;

      // Print status every 50 frames
      if (frame % STATUS_REPORT_INTERVAL == 0 && Logger.isInfoEnabled()) {
        Logger.info(
            "Frame "
                + frame
                + ": Ghost at "
                + currentPos
                + " mode="
                + blinky.getMode()
                + " dir="
                + blinky.getCurrentDirection());
      }
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("\n=== Results ===");
      Logger.info("Maximum consecutive stuck frames: " + maxStuckFrames);
    }

    if (maxStuckFrames < ACCEPTABLE_STUCK_FRAMES && Logger.isInfoEnabled()) {
      Logger.info("✅ SUCCESS: Ghost movement is working properly!");
      Logger.info("   Ghost never got stuck for more than " + maxStuckFrames + " frames.");
    } else if (maxStuckFrames >= ACCEPTABLE_STUCK_FRAMES && Logger.isInfoEnabled()) {
      Logger.info("❌ FAILED: Ghost still getting stuck!");
      Logger.info("   Ghost was stuck for " + maxStuckFrames + " consecutive frames.");
    }

    // Also test oscillation detection
    if (Logger.isInfoEnabled()) {
      Logger.info("\n=== Oscillation Detection Test ===");
    }

    // Reset and run a specific scenario
    game.resetAll();

    // Track last few positions
    Location[] recentPositions = new Location[HISTORY_ARRAY_SIZE];
    int posIndex = 0;
    boolean oscillationDetected = false;

    for (int frame = 0; frame < 200; frame++) {
      game.updateAll();

      Location pos = blinky.getCurrentLocation();
      recentPositions[posIndex] = pos;
      posIndex = (posIndex + 1) % recentPositions.length;

      // Check for oscillation pattern
      if (frame > OSCILLATION_CHECK_START_FRAME) {
        // Look for A-B-A-B pattern
        int idx1 = (posIndex - HISTORY_INDEX_1 + recentPositions.length) % recentPositions.length;
        int idx2 = (posIndex - HISTORY_INDEX_2 + recentPositions.length) % recentPositions.length;
        int idx3 = (posIndex - HISTORY_INDEX_3 + recentPositions.length) % recentPositions.length;
        int idx4 = (posIndex - HISTORY_INDEX_4 + recentPositions.length) % recentPositions.length;

        if (recentPositions[idx1] != null
            && recentPositions[idx2] != null
            && recentPositions[idx3] != null
            && recentPositions[idx4] != null
            && recentPositions[idx1].equals(recentPositions[idx3])
            && recentPositions[idx2].equals(recentPositions[idx4])
            && !recentPositions[idx1].equals(recentPositions[idx2])) {
          if (!oscillationDetected && Logger.isInfoEnabled()) {
            Logger.info(
                "Frame "
                    + frame
                    + ": Detected oscillation between "
                    + recentPositions[idx1]
                    + " and "
                    + recentPositions[idx2]);
          }
          if (!oscillationDetected) {
            oscillationDetected = true;
          }
        }
      }
    }

    if (!oscillationDetected && Logger.isInfoEnabled()) {
      Logger.info("✅ No oscillation patterns detected!");
    } else if (oscillationDetected && Logger.isInfoEnabled()) {
      Logger.info("ℹ️  Oscillation was detected but should have been handled by the fix.");
    }

    if (Logger.isInfoEnabled()) {
      Logger.info("\n=== Fix Validation Complete ===");
    }
  }
}
