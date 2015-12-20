package edu.columbia.cs.ltrie.sampling;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExplicitSamplingTechnique implements SamplingTechnique {
	private List<String> sample;
	
	public ExplicitSamplingTechnique(String parentPath, String[] path){
		sample = new ArrayList<String>();
		for(String p : path){
			File fDir = new File(parentPath+p);
			for(File f : fDir.listFiles()){
				sample.add(f.getName());
			}
		}
	}

	@Override
	public List<String> getSample() {
		return sample;
	}

}
