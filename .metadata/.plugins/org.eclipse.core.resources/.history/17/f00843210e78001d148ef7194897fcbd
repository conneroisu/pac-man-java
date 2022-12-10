package ui;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.PacmanGame;
import hw4.Blinky;

import static api.Direction.RIGHT;
import static api.Direction.UP;
import static api.Mode.*;

/**
 * Some ideas for initially testing the update() method.
 */

// NOTE: The tests below assume that you have a bogus, temporary implementation
// calculateNextCell in your Blinky class, like this:
//
//public void calculateNextCell(Descriptor d)
//{
//  Location currentLoc = getCurrentLocation();
//
//  if (currentLoc.row() > 2)
//  {
//    nextLoc = new Location(currentLoc.row() - 1, currentLoc.col());
//    nextDir = UP;
//  }
//  else
//  {
//    nextLoc = new Location(currentLoc.row(), currentLoc.col() + 1);
//    nextDir = RIGHT;
//  }
//} 
//

public class SimpleTest
{
  
  public static final String[] SIMPLE1 = {
    "#######",
    "#.....#",
    "#.....#",
    "#.....#",
    "#.....#",
    "#..B..#",
    "#S....#",
    "#######",     
  };
  
  private void updateMessage(int num) {
	  
  }
  
  public static void main(String[] args)
  {
    // using a frame rate of 10, the speed increment will be 0.4
    PacmanGame game = new PacmanGame(SIMPLE1, 10);
    
    // Blinky is always at index 0 in the enemies array
    Blinky b = (Blinky) game.getEnemies()[0];
    System.out.println(b.getCurrentLocation()); // expected (5, 3)
    System.out.println(b.getCurrentIncrement()); // expected 0.4
    System.out.println(b.getCurrentDirection()); // expected UP
    System.out.println();
    System.out.println();
    System.out.println();
    
    System.out.println("Update 1"); 
    // print the exact location of blinky with a preface
    System.out.println("Exact location: " + b.getRowExact() + ", " + b.getColExact()); // expected (5.5, 3.5)
    // print the value of next cell with a preface
    System.out.println("Next cell: " + b.getNextCell()); // expected null
    System.out.println();
    // print out the current location of blinky with a preface
    System.out.println("Current location: " + b.getCurrentLocation());
    System.out.println();
    System.out.println();
    System.out.println();
    
    System.out.println("Update 2"); 
    // print the exact location of blinky with a preface
    System.out.println("Exact location: " + b.getRowExact() + ", " + b.getColExact());    
    // this should update value of getNextCell()
    b.calculateNextCell(makeDescriptor(game));
    System.out.println("Next cell: " + b.getNextCell()); // expected (4, 3)
    System.out.println();
    System.out.println("Current location: " + b.getCurrentLocation());
    System.out.println();
    System.out.println();
    System.out.println();
    
    System.out.println("MOde:" + b.getMode());
    System.out.println("MOde:" + b.getMode());
    System.out.println("MOde:" + b.getMode());
    System.out.println("MOde:" + b.getMode());
    System.out.println("MOde:" + b.getMode());
    System.out.println("Update 3"); 
    // print the exact location of blinky with a preface
    System.out.println("Exact location: " + b.getRowExact() + ", " + b.getColExact());    
    b.calculateNextCell(makeDescriptor(game));
    System.out.println("Next cell: " + b.getNextCell());
    // now some updates
    b.update(makeDescriptor(game));
    System.out.println(b.getRowExact() + ", " + b.getColExact()); // ~5.1, 3.5
    b.update(makeDescriptor(game));
    System.out.println(b.getRowExact() + ", " + b.getColExact()); // ~4.7, 3.5
    System.out.println();
    System.out.println("Current location: " + b.getCurrentLocation());
    System.out.println();
    System.out.println();
    System.out.println();
    
    // lots of updates!  See the pdf for expected output
    for (int i = 0; i < 10; ++i)
    {
      // print the exact location of blinky with a preface
      System.out.println("Exact location: " + b.getRowExact() + ", " + b.getColExact());    
      System.out.println("Update " + (i + 4)); 
      b.update(makeDescriptor(game));
      System.out.println(b.getRowExact() + ", " + b.getColExact());
      System.out.println(b.getCurrentDirection());
      b.calculateNextCell(makeDescriptor(game));
      System.out.println("Next cell: " + b.getNextCell());
      System.out.println("Current location: " + b.getCurrentLocation());
      //repreat a print 5 times
      for (int j = 0; j < 5; ++j)
      {
        System.out.print("Update " + (i + 4));
      }
      System.out.println();
      System.out.println();
      System.out.println();

    }
  }

  
  
  
  public static Descriptor makeDescriptor(PacmanGame game)
  {
    Location enemyLoc = game.getEnemies()[0].getCurrentLocation();
    Location playerLoc = game.getPlayer().getCurrentLocation();
    Direction playerDir = game.getPlayer().getCurrentDirection();
    return new Descriptor(playerLoc, playerDir, enemyLoc);
  }
}
