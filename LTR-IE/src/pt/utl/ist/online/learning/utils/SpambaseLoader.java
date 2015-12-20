package pt.utl.ist.online.learning.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class SpambaseLoader {
	public static Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> load(String file){
		Map<Integer,DataObject<Map<Integer,Double>>> data = new HashMap<Integer, DataObject<Map<Integer,Double>>>();
		Map<Integer,Boolean> labels = new HashMap<Integer, Boolean>();
		int spam=0;
		int nonSpam=0;
		try{
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int index=1;
			while ((strLine = br.readLine()) != null)   {
				String[] attributes = strLine.split(",");
				Map<Integer, Double> atts = new HashMap<Integer, Double>();
				boolean label;
				for(int i=0; i<attributes.length-1; i++){
					Double value = Double.parseDouble(attributes[i]);
					if(value!=0){
						atts.put(i, value);
					}
				}
				if(attributes[attributes.length-1].equals("1")){
					label=true;
					spam++;
				}else{
					label=false;
					nonSpam++;
				}
				
				data.put(index, new DataObject<Map<Integer,Double>>(atts, index));
				labels.put(index, label);
				
				index++;
			}
			in.close();
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
		System.out.println("Loaded " + spam + " spam instances and " + nonSpam + " non-spam instances.");
		
		return new Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>>(data, labels);
	}
}
