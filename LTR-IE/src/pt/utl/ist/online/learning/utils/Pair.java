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
public class Pair<A, B> implements Serializable{
	
	/** The first. */
	private final A first;
	
	/** The second. */
	private final B second;

	/**
	 * Instantiates a new pair contaning the input elements
	 *
	 * @param a the first element of the new pair
	 * @param b the second element of the new pair
	 */
	public Pair(A a, B b) {
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
	public B second() {
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
		if (!(that instanceof Pair))
			return false;
		Pair thatPair = (Pair)that;
		return ((this.first.equals(thatPair.first)) && (this.second.equals(thatPair.second)));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return (31 * this.first.hashCode() + this.second.hashCode());
	}
}
