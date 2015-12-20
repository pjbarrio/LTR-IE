package edu.columbia.cs.ltrie.datamodel;

import java.io.Serializable;



public class Span implements Serializable {
	private String doc;
	private int start;
	private int end;
	private String value;
	
	public Span(String doc, int start, int end, String value) {
		this.doc = doc;
		this.start = start;
		this.end = end;
		this.value= String.copyValueOf(value.toCharArray());
	}
	
	public String getDoc() {
		return doc;
	}
	public void setDoc(String doc) {
		this.doc = doc;
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	
	public boolean contains(Span s){
		return doc.equals(s.doc) && start<=s.start && end>=s.end;
	}
	
	public boolean overlaps(Span s){
		return doc.equals(s.doc) && 
		((end>=s.start && start<=s.start)
		|| (end>=s.end && start<=s.end)
		|| contains(s)
		|| s.contains(this));
	}

	public String getValue() {
		return value;
	}
	
	public String toString(){
		return getValue() + "|" + doc + "|" + start + "|" + end;
	}
	
	public boolean equals(Object o){
		if(o instanceof Span){
			return doc.equals(((Span) o).doc) && 
				   start==((Span) o).start &&
				   end  ==((Span) o).end;
		}
		
		return false;
	}
	
	@Override
	public int hashCode(){
		return end;
	}
}
