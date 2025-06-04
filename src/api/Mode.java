package api;

/** Modes for the enemy behavior in a Pacman game. */
public enum Mode {
  /** Ghost is not active, typically at the start of the game or when respawning. */
  INACTIVE,

  /** Ghost has been eaten and is returning to the ghost house. */
  DEAD,

  /** Ghost is in a vulnerable state after Pacman eats an energizer. */
  FRIGHTENED,

  /** Ghost retreats to its designated corner of the maze. */
  SCATTER,

  /** Ghost actively pursues Pacman according to its individual algorithm. */
  CHASE
}
