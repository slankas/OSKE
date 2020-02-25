package edu.ncsu.las.util;

import java.io.*;

public class Serializer {

	public static byte[] serialize(Object obj) throws IOException {
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    ObjectOutputStream out = new ObjectOutputStream(bos);
	    out.writeObject(obj);
	    out.flush();
	    out.close();
		return bos.toByteArray();
	}

	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
	    ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream(data));
	    Object obj = in.readObject();
	    in.close();
	    return obj;
	}

}
