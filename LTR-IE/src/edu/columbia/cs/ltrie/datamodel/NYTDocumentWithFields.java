package edu.columbia.cs.ltrie.datamodel;

import java.io.File;
import java.util.List;

import com.nytlabs.corpus.NYTCorpusDocument;

public class NYTDocumentWithFields extends DocumentWithFields{
	
	public static final String ALL_FIELD = "ALL";
	public static final String TITLE_FIELD = "TITLE";
	public static final String LEAD_FIELD = "LEAD";
	public static final String BODY_FIELD = "BODY";
	public static final String DESCRIPTORS_FIELD = "DESCRIPTORS";
	public static final String LOCATIONS_FIELD = "LOCATIONS";
	public static final String CLASSIFIERS_FIELD = "CLASSIFIERS";

	public NYTDocumentWithFields(String path) {
		super(new File(path).getName());
	}
	
	public NYTDocumentWithFields(NYTCorpusDocument doc){
		super(doc.getSourceFile().getName());
		StringBuffer buf = new StringBuffer();
		String title = doc.getHeadline();
		if(title!=null){
			//addTitle(title);
			buf.append(title + "\n\n");
		}
		String leadParagraph = doc.getLeadParagraph();
		if(leadParagraph!=null){
			//addLead(leadParagraph);
			buf.append(leadParagraph + "\n\n");
		}
		String body = doc.getBody();
		if(body!=null){
			//addBody(body);
			buf.append(body + "\n\n");
		}
		
		addAll(buf.toString());
		/*List<String> descriptors = doc.getDescriptors();
		for(String desc : descriptors){
			addDescriptor(desc);
		}
		List<String> locations = doc.getLocations();
		for(String loc : locations){
			addLocation(loc);
		}
		List<String> classifiers = doc.getTaxonomicClassifiers();
		for(String cla : classifiers){
			addClassifier(cla);
		}*/
	}
	
	public void addAll(String text){
		try {
			super.addField(ALL_FIELD, text);
		} catch (UnsupportedFieldNameException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void addTitle(String value){
		try {
			super.addField(TITLE_FIELD, value);
		} catch (UnsupportedFieldNameException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void addLead(String value){
		try {
			super.addField(LEAD_FIELD, value);
		} catch (UnsupportedFieldNameException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void addBody(String value){
		try {
			super.addField(BODY_FIELD, value);
		} catch (UnsupportedFieldNameException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void addDescriptor(String value){
		try {
			super.addField(DESCRIPTORS_FIELD, value);
		} catch (UnsupportedFieldNameException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void addLocation(String value){
		try {
			super.addField(LOCATIONS_FIELD, value);
		} catch (UnsupportedFieldNameException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void addClassifier(String value){
		try {
			super.addField(CLASSIFIERS_FIELD, value);
		} catch (UnsupportedFieldNameException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
