package pt.utl.ist.online.learning.exceptions;

public class InvalidVectorIndexException extends Exception {	
	public InvalidVectorIndexException(Number dimension){
		super("This implementation assumes that index " + dimension + " is reserved for constant vectors. Please, change your data accordingly");
	}
}
