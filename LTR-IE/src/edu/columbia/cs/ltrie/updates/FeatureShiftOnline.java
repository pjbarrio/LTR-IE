package edu.columbia.cs.ltrie.updates;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activity.InvalidActivityException;

import org.apache.lucene.queryparser.classic.ParseException;

import prototype.CIMPLE.utils.CPUTimeMeasure;
import pt.utl.ist.online.learning.engines.oneclass.OneClassEngine;
import pt.utl.ist.online.learning.engines.oneclass.OneClassKernelPegasosEngine;
import pt.utl.ist.online.learning.engines.oneclass.OneClassOnlineAlgorithm;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.kernels.GaussianKernel;
import pt.utl.ist.online.learning.utils.DataObject;

import edu.columbia.cs.ltrie.features.FeaturesCoordinator;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class FeatureShiftOnline implements UpdatePrediction {

	private double fracP = 0.1;
	private double fracN = 0.1;
	private double gammaP = 0.01;
	private double gammaN = 0.01;
	private int epochsP = 100;
	private int epochsN = 100;
	private OneClassOnlineAlgorithm<Map<Long,Double>> algoP;
	private OneClassOnlineAlgorithm<Map<Long, Double>> algoN;
	private FeaturesCoordinator coord;
	private IndexConnector conn;
	private double[] valsP;
	private double[] valsN;
	private int instancesToLook;
	private double[] Ss;
	private int firstPos; //inclusive
	private double[] dcsP;
	private double[] dcsN;
	private double k = 6.0;
	private double SumS;
	private double threshold = 0.55;
	private double startAfter = 0.7;
	private long totalTime = 0;
	private int numDetections = 0;
	
	
	public FeatureShiftOnline(List<String> docs, List<String> relevantDocs, IndexConnector conn, FeaturesCoordinator coord, int instacesToLook) throws InvalidVectorIndexException{
		
		docs = new ArrayList<String>(docs);
		
		docs.removeAll(relevantDocs);
		
		SumS = 0;
		Ss = new double[instacesToLook];
		firstPos = -1;
		
		this.instancesToLook = instacesToLook;
		
		this.coord = coord;
		this.conn = conn;
		
		OneClassEngine<Map<Long,Double>> engineP = new OneClassKernelPegasosEngine<Map<Long,Double>>(fracP, new GaussianKernel(gammaP));
		
		algoP = new OneClassOnlineAlgorithm<Map<Long,Double>>(generateInstances(relevantDocs), epochsP, engineP);
		
		OneClassEngine<Map<Long,Double>> engineN = new OneClassKernelPegasosEngine<Map<Long,Double>>(fracN, new GaussianKernel(gammaN));
		
		algoN = new OneClassOnlineAlgorithm<Map<Long,Double>>(generateInstances(docs), epochsN, engineN);

		dcsP = obtainDCs(relevantDocs,algoP); 
		
		valsP = calculateValues(dcsP);
		
		dcsN = obtainDCs(docs, algoN);
		
		valsN = calculateValues(dcsN);
		
	}
	
	private double[] calculateValues(double[] dis) {
		
		double[] vals = new double[3];
		
		vals[0] = Double.MAX_VALUE;
		vals[1] = 0.0;
		vals[2] = 0.0;

		for (int i = 0; i < dis.length; i++) {
			if (dis[i] < vals[0])
				vals[0] = dis[i];
			
			vals[1]+=dis[i]; //For the mean
		
		}

		vals[1] /= (double)dis.length;

		for (int i = 0; i < dis.length; i++) {
			vals[2] += ((dis[i]-vals[1])*(dis[i]-vals[2]));
		}

		vals[2] /= (double)dis.length;
		
		return vals;
		
	}

	private double[] obtainDCs(List<String> docs,
			OneClassOnlineAlgorithm<Map<Long, Double>> algo) throws InvalidVectorIndexException {
		
		double[] ret = new double[docs.size()];
		
		for (int i = 0; i < docs.size(); i++) {
			int docId=-1;
			try {
				docId = conn.getDocId(docs.get(i));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			ret[i] = getD(coord.getFeatures(docs.get(i)),algo,docId);
		
		}
		
		return ret;
		
	}

	private double getD(Map<Long, Double> map,
			OneClassOnlineAlgorithm<Map<Long, Double>> algo, int i) throws InvalidVectorIndexException {
		return (-1)*algo.getConfidence("", new DataObject<Map<Long,Double>>(map,i));
		
	}

	private Map<Integer, DataObject<Map<Long, Double>>> generateInstances(
			List<String> docs) {
		
		Map<Integer, DataObject<Map<Long, Double>>> map = new HashMap<Integer, DataObject<Map<Long,Double>>>(docs.size());
		
		for (int i = 0; i < docs.size(); i++) {
			
			map.put(i, new DataObject<Map<Long, Double>>(coord.getFeatures(docs.get(i)), i));
			
		}
		
		return map;
		
	}

	@Override
	public boolean predictUpdate(List<String> docs, int firstDocBeingProcessed){
		long start = CPUTimeMeasure.getCpuTime();
		if (docs.size()-firstDocBeingProcessed-1 > instancesToLook){
		
			if (firstPos == -1){ //firstTime
			
				for (firstPos = 0; firstPos  < instancesToLook; firstPos++) {
					
					double s = generateS(algoP,valsP,docs.get(firstPos),algoN,valsN);
					
					SumS += s;
					
					Ss[firstPos] = s;
					
				}

				firstPos = 0; //restart it to 0, which will be the first value to replace.
				
			}else{ //second time and so forth, have to grab the last one ? Yes, because updates go to the first "if"
				
				SumS -= Ss[firstPos];
				
				double s = generateS(algoP,valsP,docs.get(instancesToLook + firstDocBeingProcessed),algoN,valsN);
				
				Ss[firstPos] = s;
				
				firstPos = (firstPos + 1)%instancesToLook;
				
				SumS += s;
				
			}
			
			if (firstDocBeingProcessed > startAfter*(double)instancesToLook){
			
				double F = (1.0 - SumS/(double)instancesToLook);
				
				//System.err.println(F);
				long end = CPUTimeMeasure.getCpuTime();
				totalTime+=(end-start);
				numDetections++;
				return F > threshold;
			}else{
				long end = CPUTimeMeasure.getCpuTime();
				totalTime+=(end-start);
				numDetections++;
				return false;
				
			}
		}else{
			long end = CPUTimeMeasure.getCpuTime();
			totalTime+=(end-start);
			numDetections++;
			return false;
		}	
				
			

		
	}
	
	private double generateS(OneClassOnlineAlgorithm<Map<Long, Double>> algoP,
			double[] valsP, String doc, OneClassOnlineAlgorithm<Map<Long, Double>> algoN,
			double[] valsN) {
		
		Map<Long, Double> feats = coord.getFeatures(doc);
		int docId=-1;
		try {
			docId = conn.getDocId(doc);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		double dP = 0;
		double dN = 0;
		
		try {
			dP = getD(feats,algoP,docId);
			dN = getD(feats,algoN,docId);
		} catch (InvalidVectorIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		if (dP <= valsP[0] || dN <= valsN[0]){
			return 1.0;
		}
		
		if (dP >= valsP[1] + k*valsP[2]){ //it will be 0, so I take the value of the other one.
			if (dN >= valsN[1] + k*valsN[2]){
				return 0;
			} else{
				return computeOtherwise(valsN,dN);
			}
		} else {
			if (dN >= valsN[1] + k*valsN[2]){ //it will be 0, so I take the value of the other one.
				return computeOtherwise(valsP,dP);
			}else{
				return Math.max(computeOtherwise(valsP,dP),computeOtherwise(valsN,dN));
			}
		}
	}

	private double computeOtherwise(double[] vals, double d) {
		
		return (vals[1] + k*vals[2] - d)/(vals[1]+k*vals[2] - vals[0]);
		
	}

	@Override
	public void performUpdate(List<String> docs, List<String> relevantDocs) {
		
		docs = new ArrayList<String>(docs);
		
		docs.removeAll(relevantDocs);
		
		for (int i = 0; i < docs.size(); i++) {
			
			try {
				algoN.addExampleAndUpdate(new DataObject<Map<Long,Double>>(coord.getFeatures(docs.get(i)),0),false);
			} catch (InvalidVectorIndexException e) {
				e.printStackTrace();
			}
	
		}
		
		for (int i = 0; i < relevantDocs.size(); i++) {
			
			try {
				algoP.addExampleAndUpdate(new DataObject<Map<Long,Double>>(coord.getFeatures(relevantDocs.get(i)),0),false);
			} catch (InvalidVectorIndexException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		}
		
		try {
			dcsP = obtainDCs(relevantDocs,algoP);
			
			valsP = calculateValues(dcsP);
			
			dcsN = obtainDCs(docs, algoN);
			
			valsN = calculateValues(dcsN);
			
			firstPos = -1;
			
			SumS = 0;

			Ss = new double[instancesToLook];
			
		} catch (InvalidVectorIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
	}
	
	@Override
	public String report() {
		return "" + ((double)totalTime/(double)numDetections)/1000000;
	}
	

}
