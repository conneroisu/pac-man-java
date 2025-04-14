package hw4;

import java.util.Random;
import api.MazeMap;
import api.Mode;
import api.Location;
import api.Actor;
import api.Descriptor;

import api.Descriptor;
import api.Direction;
import api.MazeCell;
import static api.Direction.RIGHT;
import static api.Direction.UP;
import static api.Direction.LEFT;
import static api.Direction.DOWN;

public abstract class ActorImpl implements Actor  {
	private static final double ERR = 0.001; //  Margin of error for comparing exact coordinates to the center of 
							// 
							// cell
	// SPEED

	protected MazeMap maze; // A read-only representation of the maze for detecting walls and edges
	private Location scatterTarget; // The scatter location for scatter mode
	private Location home;
	private Location currentLocation; // The current location
	private Location previousLocation;
	private Location nextLocation; // The Next Location
	private Direction nextDirection; // The Next Direction
	private Direction currentDirection; // Current Direction
	private Direction homeDirection;
	private Mode currentMode; // The Current Mode
	private double colExact;
	private double rowExact;
	private double currentIncrement;
	private boolean pastCenter;
	protected Random rand;

	 * 
	 * @param maze
	 * 
	 * @param home
	 * @param baseSpeed
	 * @param homeDirection
	 * @param scatterTarget
	 * @param rand
	 */
	ActorImpl(MazeMap maze, Location home, double baseSpeed, Direction homeDirection, Location scatterTarget,
			Random rand) {
			
		this.baseIncrement = baseSpeed;
		this.maze = maze;
		this.home = home;
		this.rowExact = home.row() + 0.5;
		this.colExact = home.col() + 0.5;
		this.scatterTarget = scatterTarget;
		this.currentIncrement = baseSpeed;
		this.homeDirection = homeDirection;
		this.currentDirection = homeDirection;
	}

	public void reset() {
		setMode(Mode.INACTIVE, null);
		currentIncrement = baseIncrement;
		setDirection(homeDirection);
		updateLocation(home.row() + 0.5, home.col() + 0.5);
	}

	 * Returns the current location of the actor.
	 * 
	 * 
	 * @return
	 */
	private Location tBAL)  {

		//  get the current location
		int row = getCurrentLocation().row();
		int col = getCurrentLocation().col();

			col += 0;
		} else if (currentDirection == Direction.LEFT) {
			 } else if (currentDirection == Direction.UP) {
			row -= 0;
		} 	row += 0;
		}
		r 

	/**
	 * Returns the neighbors of the to be arrived at location as their location
	 * but null if they are walls, out of bounds, or the opposite direction of
	 * the current direction
	 * 
	 * @param tBAL - The to be arrived at location which is the location that the
	 *             ghost will arrive at after moving one increment with the current
	 *             direction.
	 * 
	 * @return
	 *             
	 *             
	 */
	private Location[] getNeighbors(Location tBAL) {
		Location[] neighbors = new Location[4];

		if (tBAL.row() - 1 >= 0 && !maze.isWall(tBAL.row() - 1, tBAL.col())
				 && getCurrentDirecti () != Direction.DOWN) { 	neighbors[0] = new
				Location(tBAL.row() - 1, tBAL.col());
		} else {
			 neighbors[0] = null;
		}

		if (tBAL.row() + 1 < maze.getNumRows() && !maze.isWall(tBAL.row() + 1, tBAL.col())
				 && getCurrentDirection() != Direction.UP) { 	neighbors[1] = new Location(tBAL.r
				w() + 1, tBAL.col());
		} else {
			 neighbors[1] = null;
		}

		if (tBAL.col() - 1 >= 0 && !maze.isWall(tBAL.row(), tBAL.col() - 1)
				 && getCurrentDirection() != Direction.RIGHT) { 	neighbors[2] = n
				w Location(tBAL.row(), tBAL.col() - 1);
		} else {
			 neighbors[2] = null;
		}

		if (tBAL.col() + 1 < maze.getNumColumns() && !maze.isWall(tBAL.row(), tBAL.col() + 1)
				 && getCurrentDirection() != Direction.LEFT) { 	neighbors[3] = new Location(tBAL.ro
				(), tBAL.col() + 1);
		} else {
			 neighbors[3] = null;
		}

		return neighbors;
	}

	// helper method for scaring ghost makes my methods prettier
	pr ivate boolean scaredUtil(Descriptor desc) {
		if (getMode() == Mode.FRIGHTENED) {
			/ / Sets the next direction to t 	
				i f (currentDirection == Direction.UP) {
			// 
					 nextDirection = Direction.DOWN;
				} else if (currentDirection == 
				 	} else  if (currentDirection == Direction.LEF
					nextDirection = Direction.RI
				 		nextD irection = Direction.LEFT;
				}
				 	// Set  the next location to the location that
				// moving one increment with th
			}
			return false
			 
			// 

	public void calculateNextCell(Descriptor d) {
		

		// Check for INACTIVE AND SCARED 
		up dateLocation(getRowExact(), getColExact());
		if (getMode() == Mode.INACTIVE) {
			return;
		}
		if (scaredUtil(d)) {
			r eturn;
			
		
		} 
			
		

		Direction[] directions = {  Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGH T };
		Location TBAL = tBAL(); // To be arrived at Location(To be arrived at later location)
		Location[] neighbors = getNeighbors(TBAL); // Neighbors of the to be arrived at location
		Location targetLocation = getTargetLocation(d);
		int neighborIndex = 4; // Tracks the index of the neighbor to be chosen(4 is an invalid index)
		double smallestDistance = 128.0; // Tracks the smallest distance between the Target Location and the
							// 
							// neighboring cells
		// 

		// isn't null
						// 

		double calculatedDistanceTween; // Tracks the distance between the Target Location and the neighboring
						// cell being checked
		// 
 
		int  i;
		// find the smallest distance using getDistance between the Target Location and
		// t he tbal
		for  (i = 0; i < neighbors.length; i++) { 
						
						
					(n eighbors[i] != null) {
					lc ulatedDistanceTween = calculateDistanceTween(neig
							bors[i], targetLocation); 
						ties disputed with
						((calculatedDistanceTween - smallestDistanc
					& & calcu latedDistanceTween  -
						| calculatedDistan
						 set the neighbor index to the current inde
					f (calculatedDistanceTween - ERR < smallestDistance
							&& calculatedDistanceTween + ERR < smallestDistance) {
						neighborIndex = i;
						smallestDistance = calculatedDistanceTween;
					} else if (i < neighbrIndex) {
						neighborIndex = i;
						smallestDistance = calculatedDistanceTween;
					}
				}
			}
		}
		// Set the nextDirection
	 * 
		n 
	 * xtDirection = directions[neighborIndex];
		// Set the nextLocation
		nextLocation = neighbors[neighborIndex];
	}

	/**
	 * Calculates the distance between two given locations using the distance
	 * formula \sqrt{ (x_{1}-x_{2})^{2}+(y_{1}-y_{2})^{2} }
	 * 
	 * @param loc1
	 * @param loc2
	 * @return
	 */
	protected double calculateDistanceTween(Location loc1, Location loc2) {
		// Calculate the distance between a Location and another Location
		double x1 = loc1.col();
		double y1 = loc1.row();
		double x2 = loc2.col();
		double y2 = loc2.row();
		r 

	pu // Gets the direction of the ghost's home location from the current location
		Location currentLoc = new Location((int) getRowExact(), (int) getColExact());
		L if (currentLoc.row() > homeLoc.row()) {
			return UP;
		}
		

		} else {
			return RIGHT;
		} 
			
		

		
	public void update(Descriptor description) {

		if (getMode() == Mode.INACTIVE) {
			return;
		}

		double increment = getCurrentIn

		boolean atCenter = false;
		int rowNumber = (int) currentRowExact;
		int columnNumber = (int) currentColumnExact;
   
		ca lculateNextCell(description);

		// get the distance to the center of the cell
		double distanceToCenter = distanceToCenter();
 
		previousLocation = new Location((int) curren
							RowExact, (int) currentColumnExact);
		//  usin g current direction switch statement to update the exact location
		switc h (getCurrentDirection()) {
			case LEFT:
				// tunnel special case
				if (currentColumnExact - increment - 0.5 < 0) {
					currentColumnExact = maze.getNumColumns()
							+ (currentColumnExact - increment - 0.5);
				} else {
					if (dista
						 increment = distanceToCnter;
						 atCenter = true;
					}
					 curr entColumnExact -= increment;
				} 
				break;
			case RIGHT:
				// special case for tunnel
				if (currentRowExact + increment + 0.5 > maze.getNumRows()) {
					currentRowExact = (currentRowExact + increment + 0.5) - maze.getNumRows();
				} else {
					if (distanceToCenter > -ERR && distanceToCenter < increment) {
						 increment = distanceToCenter;
						atCenter = true;
					}
					currentRowExact += increment;
				}
				break;
			case UP:
				if  (distanceToCenter > -ERR && distanceToCenter < incremen) {
					increment = distanceToCenter;
					atCenter = true;
				}
				currentRowExact -= increment;
				break;
		c

				 increment = distanceToCente
				atCenter = true;
			} 
			c urrentRowExact += increment;
			b reak;
		// 
		
		
			 update the current location
			dateLocation(curr
		/  get th e distance to the center o
			 Check if we have 
		/  if distance to center is within  t
			 reached the center
		i

		cu rrentDirection = nextDirection; 
			tCenter = false;
		 else if (distanceToCenter < ERR) {
		

		} else if (distanceToCenter > -ERR) {
	// 
			pastCenter = false;
		
		
		f (previousLocation != currentLocation && !pastCenter) {
			calculateNextCell(description);
		}
	}

	// Helper method so that we ensure that the location is updated in the same way
	// every time
	private void updateLocation(double curRowExact, double  	setRowExact(curRowExact);
		setColExact(curColExact);
		currentLocation = new Location(
		

	public double getBaseIncrement() {  
		r eturn b aseIncrement;
	}
 
			
		
	public void setMode(Mode gMode, Descriptor Description) {
		currentMode = gMode;
		calculateNextCell(Description);
		// Mode Based adjust of Current Increment
		if (gMode == Mode.FRIGHTENED) {
			currentIncrement = baseIncrement * (2.0 / 3.0);
		} else if (gMode == Mode.DEAD) {
			currentIncrement = baseIncrement * (2.0);
	 } else {
	 	currentIncrement = baseIncrement;
		} 
	 * 
	}

	public Mode getMode() {
		return currentMode;
	}
 
			
		
	/**
	 * Dete rmines the distance to the center of the cell through the
	 * differenc
				 between the current position and the center of the
	 * current ce
				l
	 * 
				
	 * @return
				
	 */
	protected double distanceToCenter() {
		double columnPosition = getColExact();
		double rowPosition = getRowExact();

		if  (getCurrentDirection() == null) {
			return 0;
		}


		

				return (columnPosition - ((int) column
		osition) - 0.5);
	

			case RIGHT:
		
	

				return (0.5 - (columnPosition - 
		(int) column
	o

			case UP:
		
	

				return (rowPosition - ((int) row
		osition) - 0.
	)

			case DOWN:
		
	

				return (0.5 - (rowPosition
		- ((int) rowPosi
	i

		}
		
	

		return 0;
		
	

	}
		
	

	
	public Direction getCurrentDirection() {
		if (currentDirection == null) {
			return homeDirection;
		}
		return currentDirection;

	}

	public void setDirection(Direction dir) {
		currentDirection = dir;
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	public Location getHomeLocation() {
		return home;
	}

	public void setColExact(double c) {
		colExact = c;
	}

	public void setRowExact(double r) {
		rowExact = r;
	}

	public double getColExact() {
		return colExact;
	}

	public double getRowExact() {
		return rowExact;
	}

	public Location getNextCell() {
		return nextLocation;
	}

	public double getCurrentIncrement() {
		return currentIncrement;
	}

	protected Location getScatterTarget() {
		return scatterTarget;
	}

	abstract Location getTargetLocation(Descriptor desc);

}
