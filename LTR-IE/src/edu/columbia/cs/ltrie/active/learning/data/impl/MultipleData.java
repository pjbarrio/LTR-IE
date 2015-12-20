package edu.columbia.cs.ltrie.active.learning.data.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.columbia.cs.ltrie.active.learning.data.Data;

public class MultipleData<D, I> extends Data<List<Data<D,I>>, I> {

	public MultipleData(List<Data<D, I>> data) {
		super(data);
	}
	
	@Override
	public Data<List<Data<D, I>>, I> select(int attributes) throws Exception {
		
		List<Data<D,I>> ret = new ArrayList<Data<D,I>>();
		
		for (int i = 0; i < this.getInstances().size(); i++) {
			ret.add(this.getInstances().get(i).select(attributes));
		}
		
		return new MultipleData<D, I>(ret);
		
	}

	@Override
	public Data<List<Data<D, I>>, I> createNewInstance() {
		
		List<Data<D,I>> ret = new ArrayList<Data<D,I>>();
		
		for (int i = 0; i < this.getInstances().size(); i++) {
			ret.add(this.getInstances().get(i).createNewInstance());
		}
		
		return new MultipleData<D, I>(ret);
		
	}

	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public I get(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getClassValue(I instance) {
		throw new UnsupportedOperationException();	}

	@Override
	public void addInstance(I instance) {
		throw new UnsupportedOperationException();
	}



}
