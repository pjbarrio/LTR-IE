package edu.columbia.cs.ltrie.active.learning.instances.cleaner;

import edu.columbia.cs.ltrie.active.learning.data.Data;

public abstract class InstancesCleaner<T,I> {

	public void clean(Data<T,I> data) throws Exception{
		cleanData(data);
		data.informProcessing("r"); 
	}

	protected abstract void cleanData(Data<T,I> data) throws Exception;

}
