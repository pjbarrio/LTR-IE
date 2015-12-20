package edu.columbia.cs.ltrie.sampling.queries.generation;

public class WordValidator {
	
	public synchronized boolean isValid(String word){
		
		if (word.length() <= 2)
			return false;
		
		char[] c = word.toCharArray();
		
		for (int i = 0; i < c.length; i++) {
			if (!isLatin(c[i]))
				return false;
		}
		
		return true;
		
	}
	
	private boolean isLatin(char c) {
		
		Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
		
		if (((Character.UnicodeBlock.BASIC_LATIN.equals(block) && (Character.isLetter(c) || c == '-')) || Character.UnicodeBlock.LATIN_1_SUPPLEMENT.equals(block) || c == '�'))
			return true;
		
		return false;
		
	}

	public static void main(String[] args) {
		
		System.out.println(Character.UnicodeBlock.of('\''));
		
		String in = new String("\"emptiness\"").intern();
		
		System.out.println(new WordValidator().isValid(in));
		
		System.out.println(in);
		
	}
	
}
