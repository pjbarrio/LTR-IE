package pt.utl.ist.online.learning.utils;

import java.io.Serializable;

public class DataObject<E> implements Serializable {
	private E data;
	private int hash = -1;
	private boolean computedHash=false;
	private int id;
	
	public DataObject(E object, int id){
		data=object;
		this.id=id;
	}
	
	public E getData(){
		return data;
	}
	
	public int getId(){
		return id;
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof DataObject){
			return id==((DataObject) other).id && data.equals(((DataObject) other).data);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		if(!computedHash){
			hash=data.hashCode();
			computedHash=true;
		}
		return hash;
	}
}
