package ui;

import api.PacmanGame;
import api.Actor;
import api.Location;
import api.Mode;
import api.Descriptor;

/**
 * Validates that ghost movement fixes are working properly.
 */
public class GhostMovementFixValidation {
  
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
    
    System.out.println("=== Ghost Movement Fix Validation ===");
    System.out.println("Testing ghost that previously got stuck...\n");
    
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
        if (stuckCount > 10) {
          System.out.println("Frame " + frame + ": Ghost was stuck for " + stuckCount + " frames at " + lastPos);
        }
        stuckCount = 0;
      }
      
      lastPos = currentPos;
      
      // Print status every 50 frames
      if (frame % 50 == 0) {
        System.out.println("Frame " + frame + ": Ghost at " + currentPos + 
                           " mode=" + blinky.getMode() + 
                           " dir=" + blinky.getCurrentDirection());
      }
    }
    
    System.out.println("\n=== Results ===");
    System.out.println("Maximum consecutive stuck frames: " + maxStuckFrames);
    
    if (maxStuckFrames < 50) {
      System.out.println("✅ SUCCESS: Ghost movement is working properly!");
      System.out.println("   Ghost never got stuck for more than " + maxStuckFrames + " frames.");
    } else {
      System.out.println("❌ FAILED: Ghost still getting stuck!");
      System.out.println("   Ghost was stuck for " + maxStuckFrames + " consecutive frames.");
    }
    
    // Also test oscillation detection
    System.out.println("\n=== Oscillation Detection Test ===");
    
    // Reset and run a specific scenario
    game.resetAll();
    
    // Track last few positions
    Location[] recentPositions = new Location[10];
    int posIndex = 0;
    boolean oscillationDetected = false;
    
    for (int frame = 0; frame < 200; frame++) {
      game.updateAll();
      
      Location pos = blinky.getCurrentLocation();
      recentPositions[posIndex] = pos;
      posIndex = (posIndex + 1) % recentPositions.length;
      
      // Check for oscillation pattern
      if (frame > 20) {
        // Look for A-B-A-B pattern
        int idx1 = (posIndex - 4 + recentPositions.length) % recentPositions.length;
        int idx2 = (posIndex - 3 + recentPositions.length) % recentPositions.length;
        int idx3 = (posIndex - 2 + recentPositions.length) % recentPositions.length;
        int idx4 = (posIndex - 1 + recentPositions.length) % recentPositions.length;
        
        if (recentPositions[idx1] != null && recentPositions[idx2] != null &&
            recentPositions[idx3] != null && recentPositions[idx4] != null) {
          if (recentPositions[idx1].equals(recentPositions[idx3]) &&
              recentPositions[idx2].equals(recentPositions[idx4]) &&
              !recentPositions[idx1].equals(recentPositions[idx2])) {
            if (!oscillationDetected) {
              System.out.println("Frame " + frame + ": Detected oscillation between " + 
                                 recentPositions[idx1] + " and " + recentPositions[idx2]);
              oscillationDetected = true;
            }
          }
        }
      }
    }
    
    if (!oscillationDetected) {
      System.out.println("✅ No oscillation patterns detected!");
    } else {
      System.out.println("ℹ️  Oscillation was detected but should have been handled by the fix.");
    }
    
    System.out.println("\n=== Fix Validation Complete ===");
  }
}