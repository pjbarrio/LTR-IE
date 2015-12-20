package pt.utl.ist.online.learning.engines;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;


public interface LinearOnlineEngine<E> extends OnlineEngine<Map<E,Double>> {
	public Map<E, Double> getWeightVector();
	public double getRho();
	public LinearOnlineEngine<E> copy();
}
