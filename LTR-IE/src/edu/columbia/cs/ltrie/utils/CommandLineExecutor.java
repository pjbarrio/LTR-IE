package edu.columbia.cs.ltrie.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandLineExecutor {
	
	public synchronized String getOutput(String commandLine){
		
		String output = "";
		
		try {  
			
			Process p = Runtime.getRuntime().exec(commandLine);  
			
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));  
			
			String line = null;
			
			line = in.readLine();
			
			while (line != null) {  
				
				output = output + " \n" + line;
			
				line = in.readLine();
			}  
			
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			p.getErrorStream().close();
			p.getInputStream().close();
			p.getOutputStream().close();
			
			p.destroy();
			
			in.close();
			
		} catch (IOException e) {  
			e.printStackTrace();  
		}
		
		return output;
	}

}
