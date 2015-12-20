package edu.columbia.cs.ltrie.active.learning.instances.cleaner.impl;

import weka.core.Instances;
import edu.columbia.cs.ltrie.active.learning.data.Data;
import edu.columbia.cs.ltrie.active.learning.instances.cleaner.InstancesCleaner;

public class DummyCleaner<T,I> extends InstancesCleaner<T,I> {

	@Override
	public void cleanData(Data<T,I> data) throws Exception {
		;
	}


}
