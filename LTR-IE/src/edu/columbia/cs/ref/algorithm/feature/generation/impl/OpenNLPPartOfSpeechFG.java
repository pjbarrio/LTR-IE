package edu.columbia.cs.ref.algorithm.feature.generation.impl;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import edu.columbia.cs.ref.algorithm.feature.generation.CandidateSentenceFeatureGenerator;
import edu.columbia.cs.ref.algorithm.feature.generation.FeatureGenerator;
import edu.columbia.cs.ref.algorithm.feature.generation.SentenceFeatureGenerator;
import edu.columbia.cs.ref.model.CandidateSentence;
import edu.columbia.cs.ref.model.Sentence;
import edu.columbia.cs.ref.model.Span;
import edu.columbia.cs.ref.model.core.structure.OperableStructure;
import edu.columbia.cs.ref.model.entity.Entity;
import edu.columbia.cs.ref.model.feature.FeatureSet;
import edu.columbia.cs.ref.model.feature.impl.SequenceFS;


/**
 * The Class OpenNLPPartOfSpeechFG is a candidate sentence feature generator that 
 * finds the part-of-speech tags of the words in a sentence according to a previously
 * computed tokenization.
 * 
 * 
 * <br>
 * <br>
 * 
 * This class receives as input another feature generator that must produce the original
 * tokenization.
 *
 * @author      Pablo Barrio
 * @author		Goncalo Simoes
 * @version     0.1
 * @since       2011-09-27
 */
public class OpenNLPPartOfSpeechFG extends CandidateSentenceFeatureGenerator<SequenceFS<String>> {

	/** The tagger. */
	private transient POSTaggerME tagger;
	
	/** The tokenizer. */
	private FeatureGenerator<SequenceFS<String>> tokenizer;
	
	/** The path. */
	private String path;

	/**
	 * Instantiates a new OpenNLPPartOfSpeechFG.
	 *
	 * @param path the path to the POS model
	 * @param tokenizer the feature generator that produces the original tokenization
	 * @throws InvalidFormatException the invalid format exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public OpenNLPPartOfSpeechFG(String path,FeatureGenerator<SequenceFS<String>> tokenizer) throws InvalidFormatException, IOException{
		InputStream modelIn = null;
		POSModel modelPOS=null;
		modelIn = new FileInputStream(path);
		modelPOS = new POSModel(modelIn);
		modelIn.close();
		tagger = new POSTaggerME(modelPOS);
		this.tokenizer = tokenizer;
		this.path=path;
	}
	
	private transient Map<HashedSequenceFS,SequenceFS<String>> cache = new HashMap<HashedSequenceFS,SequenceFS<String>>();
	/* (non-Javadoc)
	 * @see edu.columbia.cs.ref.algorithm.feature.generation.CandidateSentenceFeatureGenerator#extractFeatures(edu.columbia.cs.ref.model.CandidateSentence)
	 */
	@Override
	protected SequenceFS<String> extractFeatures(CandidateSentence sentence) {
		if(cache==null){
			cache = new HashMap<HashedSequenceFS,SequenceFS<String>>();
		}
		
		if(cache.size()>=10000){
			cache.clear();
		}
		HashedSequenceFS tokenization = new HashedSequenceFS(sentence.getFeatures(tokenizer));
		SequenceFS<String> result = cache.get(tokenization);
		if(result==null){
			result = new SequenceFS<String>(tagger.tag(tokenization.toArray()));
			cache.put(tokenization, result);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see edu.columbia.cs.ref.algorithm.feature.generation.CandidateSentenceFeatureGenerator#retrieveRequiredFeatureGenerators()
	 */
	@Override
	protected List<FeatureGenerator> retrieveRequiredFeatureGenerators() {
		ArrayList<FeatureGenerator> ret = new ArrayList<FeatureGenerator>();
		
		ret.add(tokenizer);
	
		return ret;
	}

	private void writeObject(ObjectOutputStream out) throws IOException{
		out.defaultWriteObject();
		out.writeObject(path);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
		in.defaultReadObject();
		path=(String) in.readObject();
		InputStream modelIn = null;
		POSModel modelPOS=null;
		modelIn = new FileInputStream(path);
		modelPOS = new POSModel(modelIn);
		modelIn.close();
		tagger = new POSTaggerME(modelPOS);
	}
}
