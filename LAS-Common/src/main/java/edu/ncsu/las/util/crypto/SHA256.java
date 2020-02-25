package edu.ncsu.las.util.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides conveniences methods for SHA-256 hashing.
 * 
 */
public class SHA256 {
	
	public static byte[] hashString(String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
			return hash;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}	
	}

	public static byte[] hashString(String[] text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			for (String s: text) {
				digest.update(s.getBytes(StandardCharsets.UTF_8));
			}
			byte[] hash = digest.digest();
			return hash;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}	
	}	

	public static String hashStringToBase64(String[] text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			for (String s: text) {
				digest.update(s.getBytes(StandardCharsets.UTF_8));
			}
			byte[] hash = digest.digest();
			return java.util.Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}	
	}

	public static String hashStringToBase64(String text) {
		return encodeBase64(hashString(text)).replace("/","%2F");
	}

	public static void main(String args[]) {
		byte[] hash = SHA256.hashString("Hello");
		System.out.println(hash.length);
		System.out.println(java.util.Base64.getEncoder().encodeToString(hash));
	}
	
	  private static final char[] b64e = {
			    'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P',
			    'Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f',
			    'g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v',
			    'w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','-',
			  };
			  
			  public static String encodeBase64(byte[] b, int off, int len) {
			    int i = 0;
			    StringBuffer s = new StringBuffer();
			    while (len >= 3) {
			      i = ((b[off] & 0xFF) << 16) | ((b[off+1] & 0xFF) << 8) | (b[off+2] & 0xFF);
			      s.append(b64e[(i >> 18) & 0x3F]);
			      s.append(b64e[(i >> 12) & 0x3F]);
			      s.append(b64e[(i >> 6) & 0x3F]);
			      s.append(b64e[i & 0x3F]);
			      off += 3;
			      len -= 3;
			    }
			    switch (len) {
			    case 2:
			      i = ((b[off] & 0xFF) << 16) | ((b[off+1] & 0xFF) << 8);
			      s.append(b64e[(i >> 18) & 0x3F]);
			      s.append(b64e[(i >> 12) & 0x3F]);
			      s.append(b64e[(i >> 6) & 0x3F]);
			      s.append('=');
			      break;
			    case 1:
			      i = ((b[off] & 0xFF) << 16);
			      s.append(b64e[(i >> 18) & 0x3F]);
			      s.append(b64e[(i >> 12) & 0x3F]);
			      s.append('=');
			      s.append('=');
			      break;
			    }
			    return s.toString();
			  }	
	
			  public static String encodeBase64(byte[] b) {
				    return encodeBase64(b, 0, b.length);
				  }
}
