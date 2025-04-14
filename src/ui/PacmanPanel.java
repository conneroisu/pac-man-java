package ui;

import api.Actor;
import api.CellType;
import api.Direction;
import api.Location;
import api.MazeCell;
import api.Mode;
import api.PacmanGame;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * UI for a Pacman game. This UI does very little besides a) send periodic update commands to the
 * game, b) send turn instructions to the game when one of the arrow keys is pressed, and c) draw
 * the game state
 */
public final class PacmanPanel extends JPanel {
  /** Size in pixels of the cells for the grid. */
  public static final int CELL_SIZE = 30;

  /** Size of wall squares and energizers. */
  public static final int BORDER_SIZE = CELL_SIZE / 2;

  /** Size of dots. */
  public static final int PELLET_SIZE = CELL_SIZE / 4;

  /** Slightly brighter blue for wall squares. */
  public static final Color WALL_COLOR = new Color(100, 100, 255);

  /** Pale green to draw grid, if desired. */
  public static final Color GRID_COLOR = new Color(0, 150, 0);
  
  /** Angle in degrees for direction LEFT. */
  private static final int LEFT_ANGLE = 180;
  
  /** Angle in degrees for direction RIGHT. */
  private static final int RIGHT_ANGLE = 0;

  /** Angle in degrees for direction UP. */
  private static final int UP_ANGLE = 90;
  
  /** Angle in degrees for direction DOWN. */
  private static final int DOWN_ANGLE = 270;
  
  /** Full circle in degrees. */
  private static final int FULL_CIRCLE = 360;
  
  /** Pacman mouth open angle. */
  private static final int MOUTH_OPEN_ANGLE = 50;
  
  /** Milliseconds in a second. */
  private static final int MILLISECONDS_PER_SECOND = 1000;
  
  /** Offset factor for centering. */
  private static final double CENTER_OFFSET = 0.5;
  
  /** Quarter second count for ghost flashing. */
  private static final int QUARTER_SECOND = 250;
  
  /** Value for alternating flash pattern. */
  private static final int FLASH_CYCLES = 16;
  
  /** Eye size as fraction of cell size. */
  private static final int EYE_SIZE_FACTOR = 4;
  
  /** Eye separation factor. */
  private static final int EYE_SEPARATION_FACTOR = 2;
  
  /** Vertical offset for eyes. */
  private static final int EYE_VERTICAL_OFFSET_FACTOR = 10;
  
  /** Eyeball movement factor. */
  private static final int EYEBALL_MOVEMENT_FACTOR = 4;
  
  /** Half size factor. */
  private static final int HALF = 2;

  /** The grid to be displayed by this panel. */
  private PacmanGame game;

  /** Timer for game updates. */
  private Timer timer;

  /** Timer interval, determined by game's preferred frame rate. */
  private int interval;

  /** Previous location of the player, used for animation. */
  private Location prev;
  
  /** Current angle for Pacman's mouth animation. */
  private double angle;
  
  /** Increment for changing the angle in Pacman's mouth animation. */
  private double arcIncrement;

  /**
   * Constructs a panel to display the given game.
   *
   * @param game the grid to be displayed
   */
  public PacmanPanel(final PacmanGame game) {
    this.game = game;
    prev = game.getPlayer().getCurrentLocation();
    interval = MILLISECONDS_PER_SECOND / game.getFrameRate();
    timer = new Timer(interval, new TimerCallback());
    timer.start();
    this.addKeyListener(new MyKeyListener());
  }

  /**
   * Paints the game panel, including walls, dots, energizers, Pacman, and ghosts.
   *
   * @param g the graphics context to use for painting
   */
  @Override
  public void paintComponent(final Graphics g) {
    // clear background
    g.clearRect(0, 0, getWidth(), getHeight());

    for (int row = 0; row < game.getNumRows(); ++row) {
      for (int col = 0; col < game.getNumColumns(); ++col) {
        g.setColor(Color.BLACK);
        g.fillRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);

        // Color color = getColor(row, col);
        MazeCell c = game.getCell(row, col);
        if (c.isWall()) {
          g.setColor(WALL_COLOR);
          g.drawRect(
              col * CELL_SIZE + BORDER_SIZE / 2,
              row * CELL_SIZE + BORDER_SIZE / 2,
              BORDER_SIZE,
              BORDER_SIZE);
        } else {
          if (c.getType() == CellType.DOT && c.canEat()) {
            g.setColor(Color.WHITE);
            g.fillOval(
                col * CELL_SIZE + (CELL_SIZE - PELLET_SIZE) / 2,
                row * CELL_SIZE + (CELL_SIZE - PELLET_SIZE) / 2,
                PELLET_SIZE,
                PELLET_SIZE);
          } else if (c.getType() == CellType.ENERGIZER && c.canEat()) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillOval(
                col * CELL_SIZE + BORDER_SIZE / 2,
                row * CELL_SIZE + BORDER_SIZE / 2,
                BORDER_SIZE,
                BORDER_SIZE);
          }
        }
      }
    }

    drawPacman(g);
    drawGhosts(g);
  }

  /**
   * Draws Pacman with mouth animation based on the current direction and animation state.
   *
   * @param g the graphics context to use for painting
   */
  private void drawPacman(final Graphics g) {
    // upper left corner
    Actor pacman = game.getPlayer();
    Direction dir = pacman.getCurrentDirection();

    int dirDegrees = RIGHT_ANGLE;
    switch (dir) {
      case LEFT:
        dirDegrees = LEFT_ANGLE;
        break;
      case RIGHT:
        dirDegrees = RIGHT_ANGLE;
        break;
      case UP:
        dirDegrees = UP_ANGLE;
        break;
      case DOWN:
        dirDegrees = DOWN_ANGLE;
        break;
      default:
        // No action needed, using RIGHT_ANGLE as default
    }

    int currAngle = (int) Math.round(angle);
    int start = dirDegrees + currAngle;
    int sweep = FULL_CIRCLE - currAngle * 2;

    double pmRow = pacman.getRowExact() - CENTER_OFFSET;
    int rowPixel = (int) Math.round(pmRow * CELL_SIZE);
    double pmCol = pacman.getColExact() - CENTER_OFFSET;
    int colPixel = (int) Math.round(pmCol * CELL_SIZE);
    g.setColor(Color.YELLOW);
    // g.fillOval(colPixel, rowPixel, CELL_SIZE, CELL_SIZE);
    g.fillArc(colPixel, rowPixel, CELL_SIZE, CELL_SIZE, start, sweep);
  }

  /**
   * Draws all ghosts with appropriate colors and animations based on their modes.
   *
   * @param g the graphics context to use for painting
   */
  private void drawGhosts(final Graphics g) {
    Actor[] enemies = game.getEnemies();
    Color[] colorHints = game.getColorHints();
    for (int i = 0; i < enemies.length; ++i) {
      Actor ghost = enemies[i];
      if (ghost.getMode() == Mode.FRIGHTENED) {
        g.setColor(Color.BLUE);

        // flash every QUARTER second = 8 flashes in last 4 seconds
        int count = game.getFrightenedCount();
        int quarterSecondCount = QUARTER_SECOND / interval;
        if (quarterSecondCount * FLASH_CYCLES >= count) {
          int flag = count / quarterSecondCount;
          if (flag % 2 == 1) {
            g.setColor(Color.WHITE);
          }
        }
      } else if (ghost.getMode() == Mode.DEAD) {
        g.setColor(Color.DARK_GRAY);
      } else {
        g.setColor(colorHints[i]);
      }

      double pmRow = ghost.getRowExact() - CENTER_OFFSET;
      int rowPixel = (int) Math.round(pmRow * CELL_SIZE);
      double pmCol = ghost.getColExact() - CENTER_OFFSET;
      int colPixel = (int) Math.round(pmCol * CELL_SIZE);
      g.fillOval(colPixel, rowPixel, CELL_SIZE, CELL_SIZE);
      g.fillRect(colPixel, rowPixel + CELL_SIZE / HALF, CELL_SIZE, CELL_SIZE / HALF);

      int eyeSize = CELL_SIZE / EYE_SIZE_FACTOR;
      int eyeSep = eyeSize / EYE_SEPARATION_FACTOR + 1;
      int left = CELL_SIZE / HALF - eyeSep - eyeSize / HALF;
      int right = CELL_SIZE / HALF + eyeSep - eyeSize / HALF;
      int vertOffset = CELL_SIZE / EYE_VERTICAL_OFFSET_FACTOR;
      g.setColor(Color.WHITE);

      g.fillOval(colPixel + left, rowPixel + vertOffset, eyeSize, eyeSize + 1);
      g.fillOval(colPixel + right, rowPixel + vertOffset, eyeSize, eyeSize + 1);

      int eyeballX = eyeSize / HALF;
      int eyeballY = eyeSize / HALF;
      int shift = eyeSize / EYEBALL_MOVEMENT_FACTOR;
      Direction dir = ghost.getCurrentDirection();

      switch (dir) {
        case LEFT:
          eyeballX -= shift;
          break;
        case RIGHT:
          eyeballX += shift;
          break;
        case UP:
          eyeballY -= shift;
          break;
        case DOWN:
          eyeballY += shift + 1;
          break;
        default:
          // No action needed
      }

      int xPos = left + eyeballX - eyeSize / EYEBALL_MOVEMENT_FACTOR;
      int yPos = vertOffset + eyeballY - eyeSize / EYEBALL_MOVEMENT_FACTOR;

      g.setColor(Color.BLACK);

      g.fillOval(colPixel + xPos, rowPixel + yPos, eyeSize / HALF, eyeSize / HALF);
      xPos = right + eyeballX - eyeSize / EYEBALL_MOVEMENT_FACTOR;
      g.fillOval(colPixel + xPos, rowPixel + yPos, eyeSize / HALF, eyeSize / HALF);
    }
  }

  /**
   * Listens for keyboard input to control Pacman's direction.
   */
  private class MyKeyListener implements KeyListener {
    /**
     * Handles key press events to change Pacman's direction.
     *
     * @param event the key event to be processed
     */
    @Override
    public void keyPressed(final KeyEvent event) {
      int key = event.getKeyCode();
      Direction dir = null;
      switch (key) {
        case KeyEvent.VK_UP:
          dir = Direction.UP;
          break;
        case KeyEvent.VK_DOWN:
          dir = Direction.DOWN;
          break;
        case KeyEvent.VK_LEFT:
          dir = Direction.LEFT;
          break;
        case KeyEvent.VK_RIGHT:
          dir = Direction.RIGHT;
          break;
        default:
          return;
      }

      game.turnPlayer(dir);
    }

    /**
     * Not used in this implementation.
     *
     * @param e the key event to be processed
     */
    @Override
    public void keyTyped(final KeyEvent e) {
      // do nothing
    }

    /**
     * Not used in this implementation.
     *
     * @param e the key event to be processed
     */
    @Override
    public void keyReleased(final KeyEvent e) {
      // do nothing
    }
  }

  /**
   * Timer callback for animation updates.
   */
  private class TimerCallback implements ActionListener {
    /** Full unit for speed calculations. */
    private static final double FULL_UNIT = 1.0;
    
    /** Division factor for update count. */
    private static final int UPDATE_DIVISION_FACTOR = 2;
    
    /**
     * Handles timer events to update the game state and animations.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
      game.updateAll();

      // calculation for eating animation...
      Actor pacman = game.getPlayer();
      Location curr = pacman.getCurrentLocation();
      if (!curr.equals(prev)) {
        // transitioned to new cell, start closing pacman's mouth
        prev = curr;
        double currSpeed = pacman.getCurrentIncrement();
        int numUpdates = (int) (FULL_UNIT / currSpeed / UPDATE_DIVISION_FACTOR);
        arcIncrement = -MOUTH_OPEN_ANGLE / numUpdates;
        angle = MOUTH_OPEN_ANGLE + arcIncrement;
      } else if (angle <= 0) {
        arcIncrement = -arcIncrement;
        angle = arcIncrement;
      } else if (angle < MOUTH_OPEN_ANGLE) {
        angle += arcIncrement;
      }

      repaint();
    }
  }
}
