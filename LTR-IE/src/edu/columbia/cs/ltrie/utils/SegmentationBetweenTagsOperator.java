package edu.columbia.cs.ltrie.utils;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;


import com.ibm.avatar.algebra.util.regex.RegexMatcher;
import com.ibm.avatar.algebra.util.regex.SimpleRegex;
import com.ibm.avatar.algebra.util.regex.parse.ParseException;

import edu.columbia.cs.ltrie.datamodel.Span;

public class SegmentationBetweenTagsOperator {

	//private AhoCorasick trie = new AhoCorasick();
	private final int BEGIN = 0;
	private final int END = 1;
	private final int SIZE_END;
	private String begin;
	private String end;
	private transient SimpleRegex regex;
	private String pattern;
	private static int id=0;

	public SegmentationBetweenTagsOperator(String begin, String end){
		super();
		if(begin.startsWith("'")){
			begin=begin.substring(1);
		}
		if(begin.startsWith("\"")){
			begin=begin.substring(1);
		}
		if(begin.endsWith("'")){
			begin=begin.substring(0,begin.length()-1);
		}
		if(begin.endsWith("\"")){
			begin=begin.substring(0,begin.length()-1);
		}

		if(end.startsWith("'")){
			end=end.substring(1);
		}
		if(end.startsWith("\"")){
			end=end.substring(1);
		}
		if(end.endsWith("'")){
			end=end.substring(0,end.length()-1);
		}
		if(end.endsWith("\"")){
			end=end.substring(0,end.length()-1);
		}

		this.begin=begin.toLowerCase();
		this.end=end.toLowerCase();

		SIZE_END = end.length();
		//trie.add(begin.toLowerCase().getBytes(), BEGIN);
		//trie.add(end.toLowerCase().getBytes(), END);

		//trie.prepare();
		String p="(" + begin.toLowerCase() + ")|(" + end.toLowerCase() + ")";
		pattern=p;
		loadModel(pattern);
	}

	private void loadModel(String p){
		try {
			regex = new SimpleRegex(p);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean lowMemorySubstringComparison(String document, String output, int start, int end){
		for(int i=start;i<end;i++){
			if(document.charAt(i)!=output.charAt(i-start)){
				return false;
			}
		}
		return true;
	}

	private List<Span> segment(String text, Span parentSpan) {
		List<Span> entities = new ArrayList<Span>();

		text=text.toLowerCase();

		RegexMatcher matcher = regex.matcher(text);

		int lastMatch=-1;
		int lastMatchIndex=-1;

		while (matcher.find()) {
			int s = matcher.start();
			int e = matcher.end();

			String currentMatch = text.substring(s,e);

			if(lastMatch==BEGIN && currentMatch.equals(end)){
				//Match;

				String currentOut = end;

				String value = text.substring(lastMatchIndex,s);
								
				Span newSpan = new Span(parentSpan.getDoc(),parentSpan.getStart()+lastMatchIndex,parentSpan.getStart()+s,value);

				entities.add(newSpan);
				lastMatch=END;
			}else if(currentMatch.equals(begin)){
				String currentOut = begin;
				lastMatch=BEGIN;
				lastMatchIndex=e;
			}else{
				lastMatch=END;
			}

		}

		return entities;
	}

	public List<Span> execute(List<Span> D){
		List<Span> entities = new ArrayList<Span>();
		int i=0;
		for(Span d : D){
			entities.addAll(segment(d.getValue(), d));
			i++;
		}

		return entities;
	}

}
