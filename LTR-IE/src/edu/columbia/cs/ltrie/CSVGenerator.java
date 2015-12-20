package edu.columbia.cs.ltrie;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.opencsv.CSVWriter;

import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.utils.Pair;

public class CSVGenerator {

	private String data;
	private List<String[]> curves;
	private List<String[]> novcurves;
	
	public CSVGenerator(String data) {
		this.data = data;
		curves = new ArrayList<String[]>();
		novcurves = new ArrayList<String[]>();
	}

	public void printCSV(String string) throws IOException {
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(string + "splits")));
				
		bw.write(createLine(new String[]{"Technique","data","relation","extractor","position","split","useful"}));
		for (String[] line : curves) {
			bw.write("\n" + createLine(line));
		}
		
		bw.close();
		
		BufferedWriter csvn = new BufferedWriter(new FileWriter(new File(string + "novel.splits")));
		csvn.write(createLine(new String[]{"Technique","data","relation","extractor","position","split","attribute","total"}));
		for (String[] line : novcurves) {
			csvn.write("\n" + createLine(line));
		}
		
		csvn.close();
	}

	public void printTimeCSV(String string) throws IOException {
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(string + "time")));
				
		bw.write(createLine(new String[]{"Technique","data","relation","extractor","recall","split","time"}));
		for (String[] line : curves) {
			bw.write("\n" + createLine(line));
		}
		
		bw.close();
		
	}
	
	private String createLine(String[] line) {
		String ret = "\"" + line[0];
		
		for (int i = 1; i < line.length; i++) {
			ret += "\",\"" +  line[i];
		}
		
		return ret + "\"";
	}

	public void addCurve(String name, String relationship, String extractor,
			int split, Pair<int[], int[]> pair) {
		
		int[] datx = pair.first();
		int[] daty = pair.second();
		
		for (int i = 0; i < datx.length; i++) {
			
			String[] line = new String[7];
			line[0] = name; //technique
			line[1] = data; //data
			line[2] = relationship; //relation
			line[3] = extractor; //extractor
			line[4] = Integer.toString(datx[i]);
			line[5] = Integer.toString(split);
			line[6] = Integer.toString(daty[i]);
				
			curves.add(line);
			
		}
		
	}

	public void addNoveltyCurve(String name, String relation, String extr,
			int split, Map<String,Pair<int[],int[]>> slimNoveltyCurve) {
		
		for (Entry<String,Pair<int[],int[]>> ent : slimNoveltyCurve.entrySet()) {
			
			int[] datx = ent.getValue().first();
			int[] daty = ent.getValue().second();
			
			for (int i = 0; i < datx.length; i++) {
				
				String[] line = new String[8];
				line[0] = name; //technique
				line[1] = data; //data
				line[2] = relation; //relation
				line[3] = extr; //extractor
				line[4] = Integer.toString(datx[i]);
				line[5] = Integer.toString(split);
				line[6] = ent.getKey();
				line[7] = Integer.toString(daty[i]);
					
				novcurves.add(line);
				
			}
			
		}
		
		
	}

	public void addTimeCurve(String name, String relation, String extr,
			int split, Pair<double[], double[]> pair) {
		
		double[] datx = pair.first();
		double[] daty = pair.second();
		
		for (int i = 0; i < datx.length; i++) {
			
			String[] line = new String[7];
			line[0] = name; //technique
			line[1] = data; //data
			line[2] = relation; //relation
			line[3] = extr; //extractor
			line[4] = Double.toString(datx[i]);
			line[5] = Integer.toString(split);
			line[6] = Double.toString(daty[i]);
				
			curves.add(line);
			
		}
		
	}

}
