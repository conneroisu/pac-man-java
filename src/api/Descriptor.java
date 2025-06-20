package api;

/**
 * Data container for aspects of the Pacman game state that are relevant for enemy move calculation.
 */
public class Descriptor {
  /** Current player location. */
  private final Location playerLocation;

  /** Current player direction. */
  private final Direction playerDirection;

  /** Current location of the 0th enemy ("Blinky"). */
  private final Location blinkyLocation;

  /**
   * Constructs a descriptor with the given parameters.
   *
   * @param playerLocation the location of the player
   * @param playerDirection the direction the player is facing
   * @param blinkyLocation the location of the 0th enemy
   */
  public Descriptor(
      final Location playerLocation,
      final Direction playerDirection,
      final Location blinkyLocation) {
    this.playerLocation = playerLocation;
    this.playerDirection = playerDirection;
    this.blinkyLocation = blinkyLocation;
  }

  /**
   * Returns the player location.
   *
   * @return player location
   */
  public Location getPlayerLocation() {
    return playerLocation;
  }

  /**
   * Returns the player direction.
   *
   * @return player direction
   */
  public Direction getPlayerDirection() {
    return playerDirection;
  }

  /**
   * Returns the location of the 0th ghost ("Blinky").
   *
   * @return blinky's location
   */
  public Location getBlinkyLocation() {
    return blinkyLocation;
  }
}
