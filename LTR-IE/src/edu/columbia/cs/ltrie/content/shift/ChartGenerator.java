package edu.columbia.cs.ltrie.content.shift;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class ChartGenerator {

	public static void main(String[] args) throws IOException {
		
		File folder = new File("featureShift");
		
		File[] files = folder.listFiles();
		
		Arrays.sort(files);
		
		List<List<String>> lines = new ArrayList<List<String>>();
		
		for (int i = 0; i < files.length; i++) {
			
			lines.add(FileUtils.readLines(files[i]));
			
		}
		
		System.setOut(new PrintStream(new File("featureShift/chartgamma.csv")));
		
		//First Line
		
		System.out.print(files[0].getName().replace(".csv", ""));
		
		for (int i = 1; i < files.length; i++) {
			System.out.print("," + files[i].getName().replace(".csv", ""));
		}
		
		int size = lines.get(0).size();
		
		for (int i = 0; i < size; i++) {
		
			System.out.println();
			
			System.out.print(lines.get(0).get(i).split(",")[1]);
			
			for (int j = 1; j < files.length; j++) {
				
				System.out.print("," + lines.get(j).get(i).split(",")[1]);
				
			}
			
		}
		
	}

}
