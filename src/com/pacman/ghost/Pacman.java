package com.pacman.ghost;

import static api.Direction.DOWN;
import static api.Direction.LEFT;
import static api.Direction.RIGHT;
import static api.Direction.UP;

import api.Actor;
import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;

/**
 * Implements the player-controlled Pac-Man character. Handles movement, turning, and wall collision
 * detection.
 */
public final class Pacman implements Actor {
  /** Margin of error for comparing exact position to centerline of cell. */
  private static final double ERR = 0.001;

  /** Maze configuration. */
  private final MazeMap maze;

  /** Initial location on reset(). */
  private final Location home;

  /** Initial direction on reset(). */
  private final Direction homeDirection;

  /** Current direction of travel. */
  private Direction currentDirection;

  /** Basic speed increment, used to determine currentIncrement. */
  private final double baseIncrement;

  /** Current speed increment, added in direction of travel each frame. */
  private double currentIncrement;

  /**
   * Row (y) coordinate, in units of cells. The row number for the currently occupied cell is always
   * the int portion of this value.
   */
  private double rowExact;

  /**
   * Column (x) coordinate, in units of cells. The column number for the currently occupied cell is
   * always the int portion of this value.
   */
  private double colExact;

  /**
   * Flag indicating that the player is in "turning" mode, that is, moving on a diagonal in a new
   * direction of travel and simultaneously in the previous direction of travel.
   */
  private boolean turning;

  /** When in turning mode, records the previous direction of travel. */
  private Direction previousDirection;

  /** When in turning mode, records the centerline of the new row or column. */
  private double turnTarget;

  /**
   * Constructs a new Pacman with the given maze, home location, base speed, and initial direction.
   *
   * @param maze maze configuration
   * @param home initial location
   * @param baseSpeed base speed increment
   * @param homeDirection initial direction
   */
  public Pacman(
      final MazeMap maze,
      final Location home,
      final double baseSpeed,
      final Direction homeDirection) {
    this.maze = maze;
    this.home = home;
    this.baseIncrement = baseSpeed;
    this.currentIncrement = baseSpeed;
    this.homeDirection = homeDirection;
  }

  @Override
  public double getBaseIncrement() {
    return baseIncrement;
  }

  @Override
  public double getColExact() {
    return colExact;
  }

  @Override
  public Direction getCurrentDirection() {
    return currentDirection;
  }

  @Override
  public double getCurrentIncrement() {
    return currentIncrement;
  }

  @Override
  public Location getCurrentLocation() {
    return new Location((int) rowExact, (int) colExact);
  }

  @Override
  public Direction getHomeDirection() {
    return homeDirection;
  }

  @Override
  public Location getHomeLocation() {
    return home;
  }

  @Override
  public Mode getMode() {
    // Pacman doesn't have modes like ghosts
    return null;
  }

  @Override
  public double getRowExact() {
    return rowExact;
  }

  @Override
  public void reset() {
    Location homeLoc = getHomeLocation();
    setRowExact(homeLoc.row() + 0.5);
    setColExact(homeLoc.col() + 0.5);
    setDirection(getHomeDirection());
    currentIncrement = getBaseIncrement();
    turning = false;
  }

  @Override
  public void setColExact(final double c) {
    colExact = c;
  }

  @Override
  public void setDirection(final Direction dir) {
    currentDirection = dir;
  }

  @Override
  public void setMode(final Mode mode, final Descriptor desc) {
    // Pacman doesn't have modes like ghosts
  }

  @Override
  public void setRowExact(final double r) {
    rowExact = r;
  }

  /**
   * Attempts to set the direction to the given new direction. This may occur slightly before
   * reaching the new row or column, allowing the player to "cut" the corner; in that case, we enter
   * "turning" mode: this sets the new direction, but also remembers the previous direction in order
   * to keep moving along the previous direction as well as the new direction until aligned with the
   * new row or column.
   *
   * @param newDir desired direction of travel for the player
   */
  public void tryTurn(final Direction newDir) {
    if (turning) {
      // can't change direction in the middle of a turn
      return;
    }

    // easy cases first: not actually changing direction, just maybe reversing
    Direction currentDir = getCurrentDirection();
    if (((newDir == LEFT || newDir == RIGHT)
            && (currentDir == LEFT || currentDir == RIGHT || currentDir == null))
        || ((newDir == UP || newDir == DOWN)
            && (currentDir == UP || currentDir == DOWN || currentDir == null))) {
      currentDirection = newDir;
      return;
    }

    double rowPos = getRowExact();
    double colPos = getColExact();
    int colNum = (int) getColExact();
    int rowNum = (int) getRowExact();

    // max distance before new row/column that we can start a turn
    final double tolerance = 1.0;

    int newColNum = colNum;
    int newRowNum = rowNum;
    double diff = 0;

    if (newDir == LEFT || newDir == RIGHT) {
      // Idea - figure out whether a turn is possible within the next 'tolerance'
      // cell units, which could include the current cell or one cell ahead.
      // So, if we are before the center of current cell, possible turn direction
      // is the cell to left or right. But if we are past the center of current
      // cell, then possible turn is at the *next* row left or right.

      // Note: if we are currently in the tunnel, that is ok because
      // we can't be currently moving up or down
      newColNum = newDir == Direction.LEFT ? colNum - 1 : colNum + 1;
      if (currentDir == Direction.UP) {
        // distance to center
        diff = rowPos - ((int) rowPos) - 0.5;
        if (diff < 0) {
          // past the center, we must be trying to turn at next cell up
          diff = diff + 1;
          newRowNum -= 1;
        }
      } else if (currentDir == Direction.DOWN) {
        diff = 0.5 - (rowPos - ((int) rowPos));
        if (diff < 0) {
          // past the center, try next cell up
          diff = diff + 1;
          newRowNum += 1;
        }
      }
    } else {
      // Note: if we are currently in the tunnel, this is still ok because
      // we are only checking walls based on the current column number
      newRowNum = newDir == Direction.UP ? rowNum - 1 : rowNum + 1;
      if (currentDir == Direction.LEFT) {
        diff = colPos - ((int) colPos) - 0.5;
        if (diff < 0) {
          diff += 1;
          newColNum -= 1;
        }
      } else if (currentDir == Direction.RIGHT) {
        diff = 0.5 - (colPos - colNum);
        if (diff < 0) {
          diff += 1;
          newColNum += 1;
        }
      }
    }

    if (diff >= 0 && diff < tolerance && !maze.isWall(newRowNum, newColNum)) {
      // after all that, we can actually decide to turn!
      setDirection(newDir);

      // if we are not exactly aligned with new direction, then
      // go into 'turning' mode and continue to adjust along previous
      // direction with the next few update() calls. For this, we
      // need the previous direction, and it is also helpful to
      // record the centerline for the desired new row or column
      // (the 'turnTarget').
      if (diff > 0) {
        turning = true;
        previousDirection = currentDir;
        if (currentDir == UP || currentDir == DOWN) {
          // centerline of new row
          turnTarget = newRowNum + 0.5;
        } else {
          // centerline of new column
          turnTarget = newColNum + 0.5;
        }
      }
    }
  }

  @Override
  public void update(final Descriptor d) {

    if (getCurrentDirection() == null) {
      return;
    }

    // if turning, may have to continue moving along the previous direction first.
    // do this first before the forward motion, so we don't mistakenly think we're
    // hitting a wall when cutting a corner
    if (turning) {
      handleTurn();
    }

    double increment = getCurrentIncrement();
    double curRowExact = getRowExact();
    double curColExact = getColExact();
    int rowNum = (int) curRowExact;
    int colNum = (int) curColExact;

    // distance to center of cell we are in, in the direction of travel, may be
    // negative
    double diff = distanceToCenter();

    switch (getCurrentDirection()) {
      // tricky bit: if we are approaching a wall, adjust increment if needed,
      // so we end up in the center of the cell

      case LEFT:
        // special case: check whether we are in the tunnel and need to wrap around
        if (curColExact - increment - 0.5 < 0) {
          curColExact = maze.getNumColumns() + (curColExact - increment - 0.5);
        } else {
          // if we are approaching a wall, be sure we stop moving
          // at the center of current cell. This only applies when
          // 'diff' is positive but small enough that we can't move a full
          // increment
          if (diff > -ERR && diff < increment && maze.isWall(rowNum, colNum - 1)) {
            increment = diff;
          }
          curColExact -= increment;
        }
        break;
      case RIGHT:
        // special case: check whether we are in the tunnel and need to wrap around
        if (curColExact + increment + 0.5 >= maze.getNumColumns()) {
          curColExact = curColExact + increment + 0.5 - maze.getNumColumns();
        } else {
          if (diff > -ERR && diff < increment && maze.isWall(rowNum, colNum + 1)) {
            increment = diff;
          }
          curColExact += increment;
        }
        break;
      case UP:
        if (diff > -ERR && diff < increment && maze.isWall(rowNum - 1, colNum)) {
          increment = diff;
        }
        curRowExact -= increment;
        break;
      case DOWN:
        if (diff > -ERR && diff < increment && maze.isWall(rowNum + 1, colNum)) {
          increment = diff;
        }
        curRowExact += increment;
        break;
      default:
        // No action needed for default case
        break;
    }

    // finally, update instance vars
    setRowExact(curRowExact);
    setColExact(curColExact);
  }

  /**
   * When in "turning" mode, we need to update along the previous direction of travel until lined up
   * with the new row or column.
   */
  private void handleTurn() {
    double increment = getCurrentIncrement();
    double curRowExact = getRowExact();
    double curColExact = getColExact();

    if (previousDirection == UP) {
      // when up or down, turnTarget is center of target row
      double distanceToGo = curRowExact - turnTarget;
      if (increment >= distanceToGo - ERR) {
        increment = distanceToGo;

        // we've reached the center, so done with turning mode
        turning = false;
      }
      curRowExact -= increment;
    } else if (previousDirection == DOWN) {
      double distanceToGo = turnTarget - curRowExact;
      if (increment >= distanceToGo - ERR) {
        increment = distanceToGo;
        turning = false;
      }
      curRowExact += increment;
    } else if (previousDirection == LEFT) {
      // when left or right, turnTarget is center of target column
      double distanceToGo = curColExact - turnTarget;
      if (increment >= distanceToGo - ERR) {
        increment = distanceToGo;
        turning = false;
      }
      curColExact -= increment;
    } else if (previousDirection == RIGHT) {
      double distanceToGo = turnTarget - curColExact;
      if (increment >= distanceToGo - ERR) {
        increment = distanceToGo;
        turning = false;
      }
      curColExact += increment;
    }

    // finally, update instance vars
    setRowExact(curRowExact);
    setColExact(curColExact);
  }

  /**
   * Determines the difference between current position and center of current cell, in the direction
   * of travel.
   *
   * @return Distance to the center of the current cell
   */
  private double distanceToCenter() {
    double colPos = getColExact();
    double rowPos = getRowExact();
    switch (getCurrentDirection()) {
      case LEFT:
        return colPos - ((int) colPos) - 0.5;
      case RIGHT:
        return 0.5 - (colPos - ((int) colPos));
      case UP:
        return rowPos - ((int) rowPos) - 0.5;
      case DOWN:
        return 0.5 - (rowPos - ((int) rowPos));
      default:
        return 0;
    }
  }
}
