package edu.columbia.cs.ltrie;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.nytlabs.corpus.NYTCorpusDocument;

import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.CompressedAdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.utils.DocumentLoader;
import edu.columbia.cs.ltrie.utils.NYTDocumentLoader;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.ltrie.utils.StoredInformation;
import edu.columbia.cs.ref.model.Document;
import edu.columbia.cs.ref.model.Segment;
import edu.columbia.cs.ref.model.Span;
import edu.stanford.nlp.ling.CoreAnnotations.BeginIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SpanAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;

public class TuplesToSentences {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {

		Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		
		String path = args[0];//"/proj/db-files2/NoBackup/pjbarrio/Dataset/NYTValidationSplit/"; "/proj/db-files2/NoBackup/pjbarrio/Dataset/NYTTrain/"
		
		String suffix = args[1]; //"Validation" or Train 
		
		String[] relationships = new String[]{"PersonCareer"/*,"NaturalDisaster","ManMadeDisaster","VotingResult","Indictment-Arrest-Trial"/*,"OrgAff","Outbreaks"*/};
		
		String[] extractors = new String[]{"Pablo-Sub-sequences"};// new String[]{"Pablo-N-Grams","Pablo-Dependency-Graph",,"Pablo-Shortest-Path"}; //,
		
		NYTDocumentLoader loader = new NYTDocumentLoader();
		
		for (String relationship : relationships) {

			for (String extractor : extractors) {
				
				String resultsPath = "results" + relationship;
				
				int numPaths = new File(path).list().length;
				
				System.out.println("Size: " + numPaths);
				
				String[] subPaths = new String[numPaths];
				
				String folderDesign = "%0" + String.valueOf(numPaths).length() + "d";
				
				if (String.valueOf(numPaths).length() < 3){ //In train
					folderDesign = "%0" + 3 + "d";
				}
				
				for(int i=1; i<=numPaths; i++){
					subPaths[i-1]=String.format(folderDesign, i);
				}
				
				System.out.println("Initiating IE programs");
				for(String subPath : subPaths){

					String outputFile;
					
					if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
						outputFile = "results" + relationship + "/" + subPath + "_" + relationship + "-sentence.data";
					}else{
						outputFile = "results" + relationship + "/" + subPath + "_" + extractor + "_" + relationship + "_" + suffix + "-sentence.data";
					}
					
					if (new File(outputFile).exists())
						continue;
					
					
					System.out.println(relationship + "-" + extractor + "-" + subPath);
					
					CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();
				
					if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
						extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
					}else{
						
						try{
							
							extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + "_" + suffix + ".data");

						} catch (IOException e){
							
							PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("Missing_Extracion"+suffix+".txt", true)));
						    out.println(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + "_" + suffix + ".data");
						    out.close();
							
							continue;
						}
					}
					
					Collection<String> dcos = extractWrapper.getAllDocs();
					
					List<StoredInformation> listInfo = new ArrayList<StoredInformation>();
					
					for (String string : dcos) {
						
						List<Tuple> tuples = extractWrapper.getTuplesDocument(string);
						
						if (tuples != null && !tuples.isEmpty()){
							
							//Need to find the sentences in the document that produce the tuples...
							
							String filePath = path + subPath + "/" + string;
							
							File file = new File(filePath);
							
							String content;
							
							try{
								
								content = DocumentLoader.loadDocument(filePath);
								
							} catch (IOException e){
								PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("Missing_Doc"+suffix+".txt", true)));
							    out.println(filePath + "...When Running..." + resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + "_" + suffix + ".data");
							    out.close();
								continue;
							}
														
							Document doc = loader.loadFile(filePath, content);
							
							int i = 1;

							List<Span> tupleSpans = new ArrayList<Span>(tuples.size());
							
							for (Tuple tuple : tuples) {
								
								int begin = Integer.MAX_VALUE;
								int end = Integer.MIN_VALUE;
								
								for (String field : tuple.getFieldNames()) {
									
									begin = Math.min(tuple.getData(field).getStart(), begin);
									end = Math.max(tuple.getData(field).getEnd(), end);
									
								}
								
								tupleSpans.add(new Span(begin,end));
								
							}
							
							for (Segment segment : doc.getPlainText()) {
								
								Annotation document = new Annotation(segment.getValue());
							    
							    // run all Annotators on this text
							    pipeline.annotate(document);
							    
							    // these are all the sentences in this document
							    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
							    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
							    
							    for(CoreMap sentence: sentences) {
							    	
							    	String sentenceName = file.getName() + "." + i++;

							    	int begin = sentence.get(CharacterOffsetBeginAnnotation.class) + segment.getOffset();
							    	int end = sentence.get(CharacterOffsetEndAnnotation.class)  + segment.getOffset();
							    	
							    	Span sentenceSpan = new Span(begin, end);
							    	
							    	int t = 0;
							    	
							    	List<Tuple> itsTuples = new ArrayList<Tuple>();
							    	
							    	for (Span tupleSpan : tupleSpans) {
										
							    		if (sentenceSpan.contains(tupleSpan)){
							    			
							    			itsTuples.add(tuples.get(t));
							    		
							    		}
							    	
							    		t++;
							    								    		
									}
							    	
					    			if(itsTuples.size()!=0){
										listInfo.add(new StoredInformation(extractor, relationship, sentenceName,itsTuples));
									}
					    			


							    }
								
							}
							
							
							
						}
						
					}
					
					
										
					storeResults(listInfo,outputFile);
					
				}

				

				
			}

			
		}

		
		
		
	}

	private static void storeResults(List<StoredInformation> listInfo, String path) throws SQLException, IOException {
		SerializationHelper.write(path, listInfo);
	}
	
}
