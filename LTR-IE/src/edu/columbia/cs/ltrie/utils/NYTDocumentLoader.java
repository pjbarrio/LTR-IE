package edu.columbia.cs.ltrie.utils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.columbia.cs.ltrie.datamodel.Span;
import edu.columbia.cs.ref.model.Document;
import edu.columbia.cs.ref.model.Segment;
import edu.columbia.cs.ref.model.relationship.RelationshipType;



public class NYTDocumentLoader {
	
	private SegmentationBetweenTagsOperator bodyFinder = new SegmentationBetweenTagsOperator("<block class=\"full_text\">", "</block>");
	private SegmentationBetweenTagsOperator paragraphFinder = new SegmentationBetweenTagsOperator("<p>", "</p>");

	
	public Document loadFile(String path, String docContent) {
		Span docSpan = new Span(path,0,docContent.length(),docContent);
		List<Span> initialList = new ArrayList<Span>();
		initialList.add(docSpan);
		
		List<Span> body = bodyFinder.execute(initialList);
		List<Span> paragraphs = paragraphFinder.execute(body);
		
		List<Segment> segments = new ArrayList<Segment>();
		for(Span span : paragraphs){
			String value = docContent.substring(span.getStart(), span.getEnd());
			int offset = span.getStart();
			segments.add(new Segment(value, offset));
		}
		
		File f = new File(path);
		
		Document result = new Document(f.getParent(), f.getName(), segments);
		
		return result;
	}
}
