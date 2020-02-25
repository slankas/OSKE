package edu.ncsu.las.util.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Class provides the ability to encrypt/decrypt messages using AES-256.
 * 
 * The key can also be directly provided.
 * 
 * OLD:  (This was when the unlimited crypto policy file was not in place)
 * The key is provided as an array of string values, which are then concatenated and hashed using SHA256. 
 * Bytes are xor's between the lower and upper halves to reduce the keysize in half to 128 bits.
 * 
 *
 */
public class AESEncryption {
	private static final Logger logger =Logger.getLogger(AESEncryption.class.getName());
	
	public static class Key extends SecretKeySpec{
		private static final long serialVersionUID = 1L;
		  
		public Key(String[] keyValues) {
			super(SHA256.hashString(keyValues),"AES");
		}

		public Key(byte[] keyValue) {
			super(keyValue,"AES");
			if( keyValue.length != 32) throw new IllegalArgumentException("Key length must be 256 bits (32 bytes)");
		}
		
		/*
		public static byte[] halveBytes(byte[] bytes) {
			byte[] halved = new byte[bytes.length / 2];

			for(int i = 0; i < halved.length; i++){
				halved[i] = (byte) (bytes[i] ^ bytes[i + halved.length]);
			}
			return halved;
		}
		*/
		
	}

    private Key _key;

	public AESEncryption(Key key) {
		_key = key;
	 }

	// the output is sent to users
	byte[] encrypt(byte[] src)   {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/GCM/NoPadding");
			
			final byte[] iv = new byte[12];
			SecureRandom.getInstanceStrong().nextBytes(iv);
			GCMParameterSpec spec = new GCMParameterSpec(16 * 8, iv);
			
			cipher.init(Cipher.ENCRYPT_MODE, _key,spec);

			byte[] cipherText = cipher.doFinal(src);
			assert cipherText.length == src.length + 16; // See question #3
			byte[] message = new byte[12 + src.length + 16]; // See question #4
			System.arraycopy(iv, 0, message, 0, 12);
			System.arraycopy(cipherText, 0, message, 12, cipherText.length);
			return message;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException |InvalidAlgorithmParameterException e) {
			logger.log(Level.SEVERE,"encrypt exception", e);
			return null;
		}
	}

	// the input comes from users
	byte[] decrypt(byte[] message)  {
		if (message.length < 12 + 16) throw new IllegalArgumentException();
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			GCMParameterSpec params = new GCMParameterSpec(128, message, 0, 12);
			cipher.init(Cipher.DECRYPT_MODE, _key, params);
			return cipher.doFinal(message, 12, message.length - 12);
		} catch (NoSuchAlgorithmException |NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException |BadPaddingException e) {
			logger.log(Level.SEVERE,"decrypt exception", e);
			return null;
		} 
	}

	public String encryptToBase64(byte[] src) {
		byte[] encrypted = this.encrypt(src);
		if (encrypted == null) { return null; }
		return java.util.Base64.getEncoder().encodeToString(encrypted);
	}

	public String encryptToBase64(String src) {
		byte[] encrypted = this.encrypt(src.getBytes(StandardCharsets.UTF_8));
		if (encrypted == null) { return null; }
		return java.util.Base64.getEncoder().encodeToString(encrypted);
	}	  
	  
	public byte[] decryptFromBase64(String encryptedMessage) {
		byte[] bytes = java.util.Base64.getDecoder().decode(encryptedMessage);
		return decrypt(bytes);
	}	  	
	
	public String decryptFromBase64ToString(String encryptedMessage) {
		byte[] bytes = java.util.Base64.getDecoder().decode(encryptedMessage);
		byte[] decryptedBytes = decrypt(bytes);
		String result = new String(decryptedBytes,StandardCharsets.UTF_8);
		
		return result;
	}	  	
	
	public static boolean hasUnlimitedStrengthPolicy() {
		String[] keyphrases = { "be updating our forecasts every time new data is available","HEllo world","java rules" };
		AESEncryption.Key k = new AESEncryption.Key(keyphrases);
		AESEncryption aes = new AESEncryption(k);
		if (aes.encryptToBase64("password") == null) {
			return false;
		}
		return true;
	}
	
	public static void main(String args[]) {
		System.out.println("Unlimited crypto strength policy files installed: "+AESEncryption.hasUnlimitedStrengthPolicy());
		
		String[] keyphrases = { "be updating our forecasts every time new data is available","HEllo world","java rules" };
		AESEncryption.Key k = new AESEncryption.Key(keyphrases);
		AESEncryption aes = new AESEncryption(k);
		String encryptedMessage = aes.encryptToBase64("password");
		System.out.println(encryptedMessage);
		System.out.println(aes.decryptFromBase64ToString(encryptedMessage));
		 
		
	}
	
}
