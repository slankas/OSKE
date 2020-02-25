package edu.ncsu.las.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class FileUtilities {
	private static Logger logger =Logger.getLogger(FileUtilities.class.getName());
	public static void deleteDirectory(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
		   @Override
		   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			   Files.delete(file);
			   return FileVisitResult.CONTINUE;
		   }

		   @Override
		   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			   Files.delete(dir);
			   return FileVisitResult.CONTINUE;
		   }

	   });
	}
	
	/**
	 * Generates a SHA 256 Hash of the data (without a salt as this will be used to compare data)
	 * 
	 * @param data
	 * @return hash in base64 encoding
	 */
	public static String generateSHA256Hash(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(data); 
			byte[] digest = md.digest();
			return Base64.getEncoder().encodeToString(digest);
		}
		catch (NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE, "Unable to generate SHA 256 Hash: "+e);
			return null;
		}
	}
	
	public static String generateSHA256Hash(String text) {
		return generateSHA256Hash(text.getBytes(StandardCharsets.UTF_8));
	}
	
	public static String generateSHA256Hash(File f) {
		try {
		    byte[] buffer= new byte[8192];
		    int count;
		    MessageDigest md = MessageDigest.getInstance("SHA-256");
		    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
		    while ((count = bis.read(buffer)) > 0) {
		    	md.update(buffer, 0, count);
		    }
		    bis.close();
			byte[] digest = md.digest();
			return Base64.getEncoder().encodeToString(digest);
		}
		catch (NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE, "Unable to generate SHA 256 Hash: "+e);
			return null;
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to read file: "+f.toString());
			return null;
		}
	}
	
	
	public static void closeQuietly(final Closeable closeable) {
        try {
        	if (closeable != null) {
                closeable.close();
        	}
		} catch (final IOException ioe) {
		            // ignore
		}
	}
	
    /***
     * 
     * 
     * @param   is    Link to an input stream
     * @return          List of query strings.  If a processing error occured, this list is empty. 
     */
    public static java.util.ArrayList<String> readQueries(java.io.InputStream is) {
    	java.util.ArrayList<String> listOfQueries = new java.util.ArrayList<String>();
    	
        String queryLine =      new String();
        StringBuffer sBuffer =  new StringBuffer();
         
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, "UTF-8"))) {   

            //read the SQL file line by line
            while((queryLine = br.readLine()) != null)  
            {  
                // ignore comments beginning with #
                int indexOfCommentSign = queryLine.indexOf('#');
                if(indexOfCommentSign != -1)   {
                    if(queryLine.startsWith("#")) {
                        queryLine = new String("");
                    }
                    else {
                        queryLine = new String(queryLine.substring(0, indexOfCommentSign-1));
                    }
                }
                
                // ignore comments beginning with --
                indexOfCommentSign = queryLine.indexOf("--");
                if(indexOfCommentSign != -1) {
                    if(queryLine.startsWith("--")) {
                        queryLine = new String("");
                    }
                    else {
                        queryLine = new String(queryLine.substring(0, indexOfCommentSign-1));
                    }
                }
                // ignore comments surrounded by /* */
                indexOfCommentSign = queryLine.indexOf("/*");
                if(indexOfCommentSign != -1) {
                    if(queryLine.startsWith("#")) {
                        queryLine = new String("");
                    }
                    else {
                        queryLine = new String(queryLine.substring(0, indexOfCommentSign-1));
                    }
                     
                    sBuffer.append(queryLine + " "); 
                    // ignore all characters within the comment
                    do {
                        queryLine = br.readLine();
                    }  while(queryLine != null && !queryLine.contains("*/"));
                    
                    indexOfCommentSign = queryLine.indexOf("*/");
                    if(indexOfCommentSign != -1) {
                        if(queryLine.endsWith("*/")) {
                            queryLine = new String("");
                        }
                        else {
                            queryLine = new String(queryLine.substring(indexOfCommentSign+2, queryLine.length()-1));
                        }
                    }
                }
                 
                //  the + " " is necessary, because otherwise the content before and after a line break are concatenated
                // like e.g. a.xyz FROM becomes a.xyzFROM otherwise and can not be executed 
                if(queryLine != null) {
                    sBuffer.append(queryLine + " ");
                }
            }  
            br.close();
             
            // here is our splitter ! We use ";" as a delimiter for each request 
            String[] splittedQueries = sBuffer.toString().split(";");
             
            // filter out empty statements
            for(int i = 0; i<splittedQueries.length; i++)  {
                if(!splittedQueries[i].trim().equals("") && !splittedQueries[i].trim().equals("\t"))  {
                    listOfQueries.add(new String(splittedQueries[i]));
                }
            }
        }  
        catch(Exception e) { 
        	logger.log(Level.SEVERE, "Unable to parse SQL Statements: "+e);
        	logger.log(Level.SEVERE, "Buffer: "+sBuffer);  
        	listOfQueries.clear();
        }
        return listOfQueries;
    } 
    
    /**
     * Reads all the data from a named resource(file) that exists on the classpath.
     * 
     * @param resourceFile
     * @return
     * @throws IOException
     */
    public static byte[] readAllBytesFromResource(String resourceFile) throws IOException {
    	ClassLoader classLoader = FileUtilities.class.getClassLoader();
    	java.io.File file = new java.io.File(classLoader.getResource(resourceFile).getFile());
    	return readAllBytesFromFile(file);
    }
        

    /**
     * Reads the entire file into a byte array
     * 
     * @param f file to be loaded
     * @return byte array of the content
     * @throws IOException occurs when the system can not read the file.  needs be handled by the caller.
     */
    public static byte[] readAllBytesFromFile(java.io.File f) throws IOException {
    	return Files.readAllBytes(f.toPath());
    }
    
    
    /**
     * Read all bytes from the inputStream into a byte array.  The inputStream is not closed.
     * 
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] readAllBytes(InputStream is) throws IOException {
    	ByteArrayOutputStream buffer = new ByteArrayOutputStream();

	    int nRead;
	    byte[] data = new byte[16384];

	    while ((nRead = is.read(data, 0, data.length)) != -1) {
	      buffer.write(data, 0, nRead);
	    }

	    buffer.flush();

	    return buffer.toByteArray();
    }
    
    
    /**
     * Reads the contents from an InputStream into a String
     * 
     * @param input
     * @return
     * @throws IOException
     */
	public static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
	}
	
	/**
	 * Converts a GZIPInputStream to a bytearray
	 * 
	 * @param zis
	 * @return byte[] of the data
	 * @throws IOException
	 */
	public static byte[] uncompressGZipStream(GZIPInputStream zis) throws IOException {
		byte[] buffer = new byte[1024];
		
    	int size = (int) zis.available();
    	if (size <1024) size = 1024;
	    	   
    	ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
	    int len;
	    while ((len = zis.read(buffer)) > 0) {
	   	    baos.write(buffer, 0, len);
	    }
	    baos.flush();
	    baos.close();
	    byte[] data = baos.toByteArray();	
	    return data;
	}
	
}
