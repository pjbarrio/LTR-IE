package edu.columbia.cs.ltrie.sampling.queries.generation;

import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.SimpleBatchFilter;

public class In_FrequentRemovalFilter extends SimpleBatchFilter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double x;
	private double y;

	@Override
	protected Instances determineOutputFormat(Instances inputFormat)
			throws Exception {

		Instances result = new Instances(inputFormat,0);
		
		return result;
		
	}

	@Override
	public String globalInfo() {
		return "Removes Attributes that appear (set to 1) in less than X% and in more than Y% instances of the dataset";
	}

	public void setMinFrequencyvalue(double minFrequency){
		this.x = minFrequency;
	}
	
	public void setMaxFrequencyvalue(double maxFrequency){
		this.y = maxFrequency;
	}
	
	@Override
	public Instances process(Instances instances) throws Exception {
		
		double numInstances = (double)instances.numInstances();
		
		ArrayList<Attribute> toRemove = new ArrayList<Attribute>();
				
		double val;
		
		for (int i=1;i<instances.numAttributes();i++){
			
			if (i % 1000 == 0)
				System.out.println("Attribute: " + i + " Out of: " + instances.numAttributes());
			
			int[] nC = instances.attributeStats(i).nominalCounts;
						
			if (nC == null){ //numeric
				
				double[] aux = instances.attributeToDoubleArray(i);
				
				val = 0;
				
				for (int j = 0; j < aux.length; j++) {
					
					if (aux[j] == 0.0)
						val++;
				}
				
				val /= numInstances;
				
			}else {
			
				if (nC.length>1){
					val = (numInstances - (double)nC[0]) / numInstances;
				}else
					val = 0.0;
				
			}
			
			if (val<x || val>y){
				
				toRemove.add(instances.attribute(i));

			}
			
		}
		
		instances.setRelationName("TailoredRelation");
		
		int i = 0;
		
		for (Attribute attribute : toRemove) {
			i++;
			
			if (i % 1000 == 0)
				System.out.println("Removing: " + i + "-Attribute: " + attribute.name() + " Out of: " + toRemove.size());
			
			int position = getPosition(instances,attribute);
			
			instances.deleteAttributeAt(position);
		
		}
		
		return instances;
	}
	
	public static int getPosition(Instances instances, Attribute attribute) {
		
		int position = instances.attribute(attribute.name()).index();
		
		if (position>0) //Class is 0 or last index.
			return position;

		for (int i = 1; i < instances.numAttributes(); i++) {
			
			Attribute att = instances.attribute(i);
			
			if (attribute.equals(att))
				return i;
			
		}
		
		return -1;
	}
	
}
