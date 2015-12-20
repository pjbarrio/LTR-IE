package edu.columbia.cs.ltrie.utils;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DatasetLoader {
	public static List<String> loadDataset(String path){
		List<String> entries = new ArrayList<String>();

		try {
			BufferedReader in = new BufferedReader(new FileReader(path));
			String input;
			while ((input = in.readLine()) != null) {
				entries.add(input);
			}
		} catch (IOException e) {
		}

		return entries;
	}
	
	public static void main(String[] args){
		List<String> entries = loadDataset("/home/goncalo/homepagesDatasetValidationFold0.txt");
		System.out.println(entries.size());
		System.out.println(entries);
	}
}
