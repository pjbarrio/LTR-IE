package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
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

public class TuplesToSentencesTest {

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

		String path = "/proj/db-files2/NoBackup/pjbarrio/Dataset/NYTValidationSplit/";
		String subPath = "425";
		String string = "0424274.xml";

		String[] relationships = new String[]{"PersonCareer"/*,"NaturalDisaster","ManMadeDisaster","VotingResult","Indictment-Arrest-Trial","OrgAff","Outbreaks"*/};

		String[] extractors = new String[]{"Pablo-Sub-sequences"};

		NYTDocumentLoader loader = new NYTDocumentLoader();

		for (String relationship : relationships) {

			for (String extractor : extractors) {

				String resultsPath = "results" + relationship;

				System.out.println("Initiating IE programs");

				System.out.println(relationship + "-" + extractor + "-" + subPath);

				CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();

				if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
				}else{
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + ".data");
				}

				List<StoredInformation> listInfo = new ArrayList<StoredInformation>();

				List<Tuple> tuples = extractWrapper.getTuplesDocument(string);

				if (tuples != null && !tuples.isEmpty()){

					//Need to find the sentences in the document that produce the tuples...

					String filePath = path + subPath + "/" + string;

					File file = new File(filePath);

					String content = DocumentLoader.loadDocument(filePath);

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

									System.out.println(sentence.get(TextAnnotation.class));
									System.out.println(sentenceName);
									System.out.println(tuples.get(t));
									
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


		}




	}

	private static void storeResults(List<StoredInformation> listInfo, String path) throws SQLException, IOException {
		SerializationHelper.write(path, listInfo);
	}

}
