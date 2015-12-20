package pt.utl.ist.online.learning.utils;


import java.io.Serializable;

/**
 * The Class Pair represents a generic pair of two objects
 *
 * @param <A> the type of the objects that can be the first elements of the pair
 * @param <B> the type of the objects that can be the second elements of the pair
 * @author      Pablo Barrio
 * @author		Goncalo Simoes
 * @version     0.1
 * @since       2011-09-27
 */
public class UnorderedPair<A> implements Serializable{
	
	/** The first. */
	private final A first;
	private int hashA = -1;
	
	/** The second. */
	private final A second;
	private int hashB = -1;
	
	private transient int hashCode=-1;

	/**
	 * Instantiates a new pair contaning the input elements
	 *
	 * @param a the first element of the new pair
	 * @param b the second element of the new pair
	 */
	public UnorderedPair(A a, A b) {
		this.first = a;
		this.second = b;
	}

	/**
	 * Returns the first element of the pair
	 *
	 * @return the first element of the pair
	 */
	public A first() {
		return this.first;
	}

	/**
	 * Returns the second element of the pair
	 *
	 * @return the second element of the pair
	 */
	public A second() {
		return this.second;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "(" + first() + "," + second() + ")";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object that) {
		if (!(that instanceof UnorderedPair))
			return false;
		UnorderedPair thatPair = (UnorderedPair)that;
		int myCode=hashCode();
		int otherCode=this.hashCode();
		if(myCode==otherCode){
			if(((this.hashA==thatPair.hashA) && (this.hashB==thatPair.hashB)) ||
			   ((this.hashA==thatPair.hashB) && (this.hashB==thatPair.hashA))){
				return ((this.first.equals(thatPair.first)) && (this.second.equals(thatPair.second))) ||
						((this.first.equals(thatPair.second)) && (this.second.equals(thatPair.first)));
			}
		}
		return false;
		
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		if(hashA==-1){
			hashA=this.first.hashCode();
		}
		if(hashB==-1){
			hashB=this.second.hashCode();
		}
		if(hashCode==-1){
			hashCode=hashA + hashB;
		}
		return hashCode;
	}
}
