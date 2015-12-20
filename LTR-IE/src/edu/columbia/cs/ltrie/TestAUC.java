package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.columbia.cs.ltrie.excel.curves.DataPoint;

import auc.Confusion;
import auc.ReadList;

public class TestAUC {
	public static void main(String[] args) throws IOException {
	
		List<DataPoint> points = new ArrayList<DataPoint>();
		points.add(new DataPoint(0.9, true));
		points.add(new DataPoint(0.8, true));
		points.add(new DataPoint(0.7, false));
		points.add(new DataPoint(0.6, true));
		points.add(new DataPoint(0.55, true));
		points.add(new DataPoint(0.54, true));
		points.add(new DataPoint(0.53, false));
		points.add(new DataPoint(0.52, false));
		points.add(new DataPoint(0.51, true));
		points.add(new DataPoint(0.505, false));
		
		Collections.sort(points);
		
		Confusion localConfusion = ReadList.readFile(points);
		System.out.println(localConfusion.calculateAUCROC());
	}
	
}
