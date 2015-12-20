package pt.utl.ist.online.learning.utils;

import java.util.HashMap;

public class EmptyStatistics extends Statistics {
	
	private static final long serialVersionUID = -5413553790524803038L;
		
	public EmptyStatistics() {
		super(new HashMap<String, Integer>());
	}
}
