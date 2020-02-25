package edu.ncsu.las.util.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


/**
 * Calculate an HMAC- SHA1 Signature
 *
 *
 */
public class HMAC_SHA1Signature {

	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

	// hex conversion from http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java 
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	private static byte[] sign(String data, String key)  {
		try {
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(signingKey);
			return mac.doFinal(data.getBytes());
		}
		catch (NoSuchAlgorithmException |InvalidKeyException e) {
			System.err.println(e);
			return new byte[0];
		}
	}
	
	public static String signToHex(String data, String key)		{
		return bytesToHex(sign(data,key));
	}
	
	public static String signToBase64(String data, String key)		{
		return Base64.getEncoder().encodeToString(sign(data,key));
	}	
	
}
