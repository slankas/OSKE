package edu.ncsu.las.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** 
 * Builds a time-based UUID.  
 * 
 * The most significant digits contain the result from System.currentTimeMillis.  
 * The lower significant digits is divided into two parts:
 * The first 4 bytes represent the IP address of the server.
 * The second 4 bytes are an incremental counter ince the program was started.  
 *     rollover is acceptable as the we should have that many events with a milllisecond.
 * 
 *
 */
public class UUID {
	private static java.util.concurrent.atomic.AtomicInteger counter = new AtomicInteger(0);
	
	private static byte[] ipAddress;
	static {
		try {
			ipAddress = InetAddress.getLocalHost().getAddress();
		}
		catch (UnknownHostException ex) {
			ipAddress = new byte[4];
			SecureRandom sr = new SecureRandom();
		    sr.nextBytes(ipAddress);
		}
	}
	
	/**
	 * creates a time-based UUID.
	 * 
	 * @return UUID  See class comments for format.
	 */
	public static java.util.UUID createTimeUUID() {
		long upper = System.currentTimeMillis();
		long lower = lowerBytes(ipAddress,counter.incrementAndGet());
		
		return new java.util.UUID(upper,lower);
	}
	
	/**
	 * Sets the machine identifier (based upon a 4-byte ip address (or some other value that fits within that format.
	 * If this is not set, it defaults to the localhost address (no guarantee to which adapter is found) or to a random byte array
	 * 
	 * @param b byte array - must have a length of four, otherwise an illegal argument exception is thrown
	 */
	public static void setByteAddress(byte[] b) {
		if (b.length != 4) {
			throw new IllegalArgumentException("UUID.setByteAddress - byte array must have a length of 4");
		}
		ipAddress = b;
	}
	
	/**
	 * returns a long based upon the given byte array (upper 4 bytes), and a counter(lower 4 bytes))
	 * @param bytes
	 * @param counter
	 * @return
	 */
	private static long lowerBytes(byte[] bytes, int counter) {
        long val = 0;
        for (int i = 0; i < bytes.length; i++) {
            val=val<<8;
            val=val|(bytes[i] & 0xFF);
        }
        val = val <<32;
        val=val| (counter & 0xFFFFFFFF);
        return val;
    }
	
	public static void main(String args[]) {
		for (int i=0; i< 100; i++) {
			System.out.println(createTimeUUID());
		}
	}
}
