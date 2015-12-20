package edu.columbia.cs.ltrie.datamodel;

public class ValueSpan extends Span {

	public ValueSpan(String value) {
		super("doc", -1, -1, value.toLowerCase());
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int hashCode() {
		return super.getValue().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		return super.getValue().equals(((Span)o).getValue());
	}
	
}
