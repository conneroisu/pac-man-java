package hw4;

import java.util.Random;

import api.Descriptor;
import api.Direction;
import api.Location;
import api.MazeMap;
import api.Mode;

public class Blunky extends SuperClass{

	Blunky(MazeMap maze, Location home, double baseSpeed, Direction homeDirection, Location scatterTarget,
			Random rand) {
		super(maze, home, baseSpeed, homeDirection, scatterTarget, rand);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void reset() {
		super.setMode(Mode.INACTIVE, currentDesc);
		currentSpeed = this.baseIncrement;
		this.setDirection(getHomeDirection());
		this.currentLocation = this.home;
	}

	@Override
	public void calculateNextCell(Descriptor d) {
		// TODO Auto-generated method stub
		
	}
	
	

}
