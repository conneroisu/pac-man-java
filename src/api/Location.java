package api;

/** Immutable container for a pair of two-dimensional coordinates. */
public class Location {
  /** The row. */
  private final int rowValue;

  /** The columnn. */
  private final int colValue;

  /**
   * Constructs a Location with the given row and column.
   *
   * @param row given row
   * @param col given column
   */
  public Location(final int row, final int col) {
    this.rowValue = row;
    this.colValue = col;
  }

  /**
   * Returns the row value.
   *
   * @return the row
   */
  public int row() {
    return rowValue;
  }

  /**
   * Returns the column value.
   *
   * @return the column
   */
  public int col() {
    return colValue;
  }

  /**
   * Determines whether this Location is equal to the given object.
   *
   * @param obj the object to compare this location with
   * @return true if the given object is a Location with the same row and column
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    Location other = (Location) obj;
    return rowValue == other.rowValue && colValue == other.colValue;
  }

  /**
   * Returns a hash code value for this location.
   *
   * @return a hash code value for this location
   */
  @Override
  public int hashCode() {
    return 31 * rowValue + colValue;
  }

  /**
   * Returns a string representation of this object in the form (row, column).
   *
   * @return string representation of the location
   */
  @Override
  public String toString() {
    return "(" + rowValue + ", " + colValue + ")";
  }
}
