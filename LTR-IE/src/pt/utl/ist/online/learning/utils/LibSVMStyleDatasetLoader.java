package pt.utl.ist.online.learning.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class LibSVMStyleDatasetLoader {
	public static Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> load(String file){
		Map<Integer,DataObject<Map<Integer,Double>>> data = new HashMap<Integer, DataObject<Map<Integer,Double>>>();
		Map<Integer,Boolean> labels = new HashMap<Integer, Boolean>();
		
		int pos = 0;
		int neg = 0;
		try{
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int index=1;
			while ((strLine = br.readLine()) != null)   {
				String[] attributes = strLine.split(" ");
				Map<Integer, Double> atts = new HashMap<Integer, Double>();
				
				boolean label;
				if(attributes[0].equals("1")){
					label=true;
					pos++;
				}else if(attributes[0].equals("-1")){
					label=false;
					neg++;
				}else{
					label=false;
					System.err.println("Strange label in line " + strLine);
					System.exit(1);
				}
				
				for(int i=1; i<attributes.length; i++){
					String[] attributeValue = attributes[i].split(":");
					atts.put(Integer.parseInt(attributeValue[0]), Double.parseDouble(attributeValue[1]));
				}
				
				data.put(index, new DataObject<Map<Integer,Double>>(atts, index));
				labels.put(index, label);
								
				index++;
			}
			in.close();
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
		System.out.println("Loaded " + pos + " positive instances and " + neg + " negative instances.");
		
		return new Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>>(data, labels);
	}
	
	private static double normsq(Map<Integer, Double> atts){
		double sum=0;
		
		for(Entry<Integer,Double> entry : atts.entrySet()){
			sum+=entry.getValue()*entry.getValue();
		}
		
		return sum;
	}
}
