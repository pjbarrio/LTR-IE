package edu.columbia.cs.ltrie.utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DocumentLoader {
	public static String loadDocument(String path) throws IOException{
		File file = new File(path);
		BufferedReader in = new BufferedReader(new FileReader(file));
        
        String input;
        StringBuilder reply = new StringBuilder();
        
        while ((input = in.readLine()) != null) {
        	reply.append(input + "\n");
        }
	        
        in.close();
        return reply.toString();
	}
}
