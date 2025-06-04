package api;

/** Constants representing possible states of a maze cell. */
public enum CellType {
  /** An empty cell with no collectible item. */
  EMPTY,

  /** A wall cell that cannot be traversed. */
  WALL,

  /** A cell containing a regular dot collectible. */
  DOT,

  /** A cell containing an energizer collectible that triggers ghost vulnerability. */
  ENERGIZER
}
