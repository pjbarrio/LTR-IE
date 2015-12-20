package pt.utl.ist.online.learning.utils;

import java.util.ArrayList;
import java.util.List;

import prototype.CIMPLE.utils.CPUTimeMeasure;

public class TimeMeasurer {
	private long startTime;
	private List<Integer> checkPoints = new ArrayList<Integer>();
	
	public TimeMeasurer(){
		startTime=CPUTimeMeasure.getCpuTime()/1000000;
	}
	
	public void addCheckPoint(){
		long checkTime = CPUTimeMeasure.getCpuTime()/1000000;
		checkPoints.add((int) (checkTime-startTime));
	}
	
	public List<Integer> getCheckPoints(){
		return checkPoints;
	}
}
