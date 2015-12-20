package edu.columbia.cs.ltrie.utils;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class SerializationHelper {
	public static Object read(InputStream stream) throws IOException, ClassNotFoundException{
		ObjectInput input = new ObjectInputStream ( stream );
		Object result = input.readObject();
		input.close();
		return result;
	}
	
	public static Object read(String path) throws IOException, ClassNotFoundException{
		return read(new BufferedInputStream(new FileInputStream( path )));
	}
	
	public static void write(OutputStream stream, Object o) throws IOException{
		ObjectOutput out = new ObjectOutputStream(stream);
		out.writeObject(o);
		out.close();
	}
	
	public static void write(String path, Object o) throws IOException{
		write(new FileOutputStream(path),o);
	}
}
