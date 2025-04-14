package api;

/** Indication of direction for actor motion. */
public enum Direction {
  /** Movement to the left on the grid (decreasing x coordinate). */
  LEFT,
  
  /** Movement to the right on the grid (increasing x coordinate). */
  RIGHT,
  
  /** Movement upward on the grid (decreasing y coordinate). */
  UP,
  
  /** Movement downward on the grid (increasing y coordinate). */
  DOWN
}