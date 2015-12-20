package edu.columbia.cs.ltrie.active.learning.execute;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.apache.commons.io.FileUtils;

import edu.columbia.cs.ltrie.baseline.factcrawl.utils.MapBasedComparator;
import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.AdditiveFileSystemWrapping;

public class CompareActiveLearning {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws WriteException 
	 * @throws RowsExceededException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, RowsExceededException, WriteException {


		String[][] relationships= new String[][]{{"ManMadeDisaster","HMM"},{"PersonCareer","CRF"},{"VotingResult","CRF"},{"Indictment-Arrest-Trial","CRF"}};

		String[][] extractors = {{"Pablo-Sub-sequences","SSK"}};

		boolean[] stopWordsA = {true,false};

//		List<String> docs = FileUtils.readLines(new File("/proj/dbNoBackup/pjbarrio/Dataset/filesValNYT.txt"));
		
		List<String> docs = FileUtils.readLines(new File("/proj/dbNoBackup/pjbarrio/Dataset/listRandom.txt"));
		
		int numDocs = 63000;
		
		for (int ext = 0; ext < extractors.length; ext++) {

			String modelExt = extractors[ext][1];

			System.out.println("running");

			int rel = Integer.valueOf(args[0]);

			String relationship = relationships[rel][0];

			//String path = "/local/pjbarrio/Files/Downloads/NYTValidationSplit/";
			int numPaths=672;
			String[] subPaths = new String[numPaths];
			for(int i=1; i<=numPaths; i++){
				subPaths[i-1]=String.format("%03d", i);
			}
			
			String resultsPath = "results" + relationship;
			System.out.println("Initiating IE programs");
			AdditiveFileSystemWrapping extractWrapper = new AdditiveFileSystemWrapping();
			for(String subPath : subPaths){
				if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
				}else{
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractors[ext][0] + "_" + relationship + ".data");
				}
			}
			
			Map<String,Integer> relevance = new HashMap<String,Integer>();
			
			for (String doc : docs) {
				
				int num = extractWrapper.getNumTuplesDocument(doc);
				if(num!=0){
					relevance.put(doc, num);
				}
				
			}
			
			ExcelGenerator gen = new ExcelGenerator();
			gen.addRankingCurve("RandomRanking", new BaselineCurve(numDocs));
			gen.addRankingCurve("PerfectRanking", new PerfectCurve(numDocs, relevance));
						
			for (int split = 1; split <= 5; split++) {

				for (int numQueries = 50; numQueries <= 500 ; numQueries+=50){

					for (int docsPerQuery = 10; docsPerQuery <= 50; docsPerQuery+=10) {

						for (int i = 0; i < stopWordsA.length; i++) {

							boolean stopWords = stopWordsA[i];

							String prefixOutp =  "outputRandomWekaClassifiers/" + relationship + "/" + modelExt + "/" + split + "/" + stopWords + "/";;

							new File(prefixOutp).mkdirs();

							String name = "NQ" + numQueries + "-DPQ" + docsPerQuery;

							boolean equivalents[] = {false,true};

							for (int size = 100; size <= 2000; size+=100) {

								for (int eqv = 0; eqv < equivalents.length; eqv++) {

									String nm = name + "SZ-" + size + "EQV-" + equivalents[eqv] + ".data";

									for (int feat = 100; feat <= 1000; feat+=100) {

										for (int classi = 2; classi <=5 ; classi++) {

											String trained = nm + "FEAT-" + feat + "CLASS-" + classi;

											File outp = new File(prefixOutp, trained);

											String nom = "SPL-" + split + "SW-" + stopWords + "NQ-" + numQueries + "DPQ-" + docsPerQuery + "S-" + size + "EQV-" + equivalents[eqv] + "F-" + feat + "C-" + classi;
											
											if (!outp.exists()){
												System.out
														.println("does not exist");
												continue;
											}else{
												System.out
														.println("exists" + nom);
											}
											
											List<String> sortedDocumentsf = sort(FileUtils.readLines(outp));
											
											nom.replace("-", "");
											
											gen.addRankingCurve(nom, new SortedCurve(numDocs, sortedDocumentsf, relevance));
											
										}

									}

								}

							}

						}

					}

				}

				//				}

				//			}

			}
			
			gen.generateR("test_RandomActiveL_"+relationship,300,true);

			
		}
	}

	private static List<String> sort(List<String> docs) {
		
		Map<String,Double> map = new HashMap<String, Double>(docs.size());
		
		for (String docScore : docs) {
			
			String[] spls = docScore.split(",");
			
			if (spls.length > 1)
				try{
					map.put(spls[0], Double.parseDouble(spls[1]));
				}catch(Exception e){
					//haha;
				}
			
		}
		
		List<String> ret = new ArrayList<String>(map.keySet());
		
		Collections.sort(ret,new MapBasedComparator<String>(map, true));
		
		return ret;
		
	}

}
