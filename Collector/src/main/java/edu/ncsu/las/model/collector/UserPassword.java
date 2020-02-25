package edu.ncsu.las.model.collector;

import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.google.common.cache.CacheLoader;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.persist.collector.UserPasswordDAO;
import edu.ncsu.las.util.DateUtilities;
import edu.ncsu.las.util.StringValidation;

/**
 * Provides functionality to manage and validate user passwords
 * 
 * Static code adapted from https://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash#18143616
 * 
 */
public class UserPassword {
	
	//Authentication result values
	public static final String RESULT_INVALID_TOKEN        = "Invalid token";
	public static final String RESULT_ACCOUT_SUSPENDED     = "Account suspended";
	public static final String RESULT_MUST_CHANGE_PASSWORD = "mustChangePassword";
	public static final String RESULT_OTHER_ISSUE          = "Unable to authenticate";
	public static final String RESULT_PASSWORD_AGE         = "passwordAge";
	public static final String RESULT_INVALID_USER_PASSWORD  = "The user name and password combination is not recognized";
	public static final String RESULT_SUCCESS              = "success";
	
	private static Logger logger =Logger.getLogger(UserPassword.class.getName());
	
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int ITERATIONS = 10000;
	private static final int KEY_LENGTH = 256;

	/**
	 * Returns a random salt to be used to hash a password.
	 *
	 * @return a 16 bytes random salt
	 */
	protected static byte[] getNextSalt() {
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		return salt;
	}

	/**
	 * Returns a salted and hashed password using the provided hash.<br>
	 * Method is no
	 *
	 * @param password the password to be hashed
	 * @param salt     a 16 bytes salt, ideally obtained with the getNextSalt method
	 *
	 * @return the hashed password with a pinch of salt
	 */
	protected static byte[] hash(String password, byte[] salt) {
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			return skf.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
		} finally {
			spec.clearPassword();
		}
	}

	/**
	 * Returns true if the given password and salt match the hashed value, false otherwise.<br>
	 *
	 * @param password     the password to check
	 * @param salt         the salt used to hash the password
	 * @param expectedHash the expected hashed value of the password
	 *
	 * @return true if the given password and salt match the hashed value, false otherwise
	 */
	protected static boolean isExpectedPassword(String password, byte[] salt, byte[] expectedHash) {
		byte[] pwdHash = hash(password, salt);
		return java.util.Arrays.equals(expectedHash, pwdHash);
	}

	/**
	 * Generates a random password of a given length, using letters and digits.
	 * This can also be used to generate a temporary access token.
	 *
	 * @param length the length of the password
	 *
	 * @return a random password
	 */
	public static String generateRandomPassword(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int c = RANDOM.nextInt(62);
			if (c <= 9) {
				sb.append(String.valueOf(c));
			} else if (c < 36) {
				sb.append((char) ('a' + c - 10));
			} else {
				sb.append((char) ('A' + c - 36));
			}
		}
		return sb.toString();
	}	
	  
	private String _emailID;
    private byte[] _password;
	private byte[] _salt;
	private String _temporaryAccessToken;
	private boolean _mustChangePassword;
	private boolean _accountSuspended;
	private Instant _passwordChangedDateTime;
	private Instant _passwordAccountLockedUntilDateTime;
	  
	public UserPassword(String emailID, byte[] password, byte[] salt, String temporaryAccessToken, boolean mustChangePassword, 
			           Instant passwordChangedDateTime, Instant passwordAccountLockedUntilDateTime, boolean accountSuspended) {
		_emailID = emailID;
		_password = password;
		_salt = salt;
		_temporaryAccessToken = temporaryAccessToken;
		_mustChangePassword = mustChangePassword;
		_passwordChangedDateTime = passwordChangedDateTime;
		_passwordAccountLockedUntilDateTime = passwordAccountLockedUntilDateTime;		
		_accountSuspended = accountSuspended;
	}
	
	public String getEmailID() { return _emailID;	}
	public byte[] getPassword() { return _password;	}
	public byte[] getsalt() { return _salt; }
	public String getTemporaryAccessToken() { return _temporaryAccessToken; }
	public boolean isMustChangePassword() { return _mustChangePassword; }
	public Instant getPasswordChangedDateTime() { return _passwordChangedDateTime; }
	public Instant getPasswordAccountLockedUntilDateTime() { return _passwordAccountLockedUntilDateTime; }
	public boolean isAccountSuspended() { return _accountSuspended; }
	
	/**
	 * Generate and store a temporary access token for the current user
	 * 
	 * @param emailID
	 * @return null if any error occurs.  message printed to log
	 */
	public static String requestTemporaryAccessToken(String emailID) {
		try {
			UserPasswordDAO upd = new UserPasswordDAO();
			UserPassword record = upd.retrieve(emailID);
			if (record == null) {
				
				User u = User.findUser(emailID);
				if (u == null || u.hasAnyActiveAccess() == false) {
					
					logger.log(Level.WARNING, "Unable to generate temporary access token - no password record exists, email ID: "+ emailID);
					return null;
				}
				else {
					record = UserPassword.createAccount(emailID, UserPassword.generateRandomPassword(20)+"Ab1$"); //ensure all 4 character classes are meet for validation
				}
			}
			record._temporaryAccessToken = generateRandomPassword(100);
			if (upd.updateTemporaryAccessToken(record)) {
				return record._temporaryAccessToken;
			}
			else {
				logger.log(Level.WARNING, "Unable to store temporary access token, email ID: "+ emailID);
				return null;
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "temporary access token request.  email ID: "+ emailID +", exception: "+e.toString());
			return null;
		}
	}
	
	/**
	 * Authenticates the user based upon a temporary access token
	 * 
	 * @param emailID
	 * @param tempToken
	 * @return error message is the authentication failed
	 *         "mustChangePassword" - authentication succeeded, but user must change password before proceeding
	 */
	public static String authenticateUserViaTemporaryToken(String emailID, String tempToken) {
		try {
			UserPasswordDAO upd = new UserPasswordDAO();
			UserPassword record = upd.retrieve(emailID);
			if (record == null) {
				return RESULT_INVALID_TOKEN;
			}
			if (record.isAccountSuspended()) {
				return RESULT_ACCOUT_SUSPENDED;
			}
			if (!tempToken.equals(record.getTemporaryAccessToken())) {
				return RESULT_INVALID_TOKEN;
			}
			else {
				// success.  tokens are one-time use only, so don't allow again
				record._temporaryAccessToken = "noTokenAssigned";
				upd.updateTemporaryAccessToken(record);
				return RESULT_MUST_CHANGE_PASSWORD;
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to authenticate account via temporary token.  email ID: "+ emailID +", exception: "+e.toString());
			return RESULT_OTHER_ISSUE;
		}			
	}	
	

	
	public static boolean suspendAccount(String emailID) {
		try {
			UserPasswordDAO upd = new UserPasswordDAO();
			UserPassword record = upd.retrieve(emailID);
			if (record == null) {
				logger.log(Level.WARNING, "Unable to suspend account - no password record exists, email ID: "+ emailID);
				return false;
			}
			return upd.updateSuspend(record);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to suspend account.  email ID: "+ emailID +", exception: "+e.toString());
			return false;
		}
	}
	
	
	public static boolean lockAccount(String emailID, Duration forTimeAmount) {
		try {
			UserPasswordDAO upd = new UserPasswordDAO();
			UserPassword record = upd.retrieve(emailID);
			if (record == null) {
				logger.log(Level.WARNING, "Unable to lock account - no password record exists, email ID: "+ emailID);
				return false;
			}
			record._passwordAccountLockedUntilDateTime = Instant.now().plus(forTimeAmount);
			return upd.updateAccountLocked(record);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to lock account.  email ID: "+ emailID +", exception: "+e.toString());
			return false;
		}		
	}

	/**
	 * Authenticates the user based upon the password.
	 * 
	 * @param emailID
	 * @param password
	 * @return error message is the authentication failed, RESULT_SUCCESS if it succeeded.
	 *         "mustChangePassword" - authentication succeeded, but user must change password before proceeding
	 *         "passwordAge" - authentication succeeded, but the password is old and must be changed before proceeding
	 */
	public static String authenticateUser(String emailID, String password) {
		try {
			UserPasswordDAO upd = new UserPasswordDAO();
			UserPassword record = upd.retrieve(emailID);
			if (record == null) {
				return RESULT_INVALID_USER_PASSWORD;
			}
			if (record.getPasswordAccountLockedUntilDateTime().isAfter(Instant.now())) {
				return "Account locked until "+DateUtilities.getDateTimeISODateTimeFormat(record.getPasswordAccountLockedUntilDateTime());
			}
			if (record.isAccountSuspended()) {
				return RESULT_ACCOUT_SUSPENDED;
			}
			if (!isExpectedPassword(password, record.getsalt(), record.getPassword())) {
				return RESULT_INVALID_USER_PASSWORD;
			}
			if (record.isMustChangePassword()) {
				return RESULT_MUST_CHANGE_PASSWORD;
			}
			if (record.getPasswordChangedDateTime().plus(Configuration.getConfigurationPropertyAsLong(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LOCAL_MAX_PASSWORD_AGE), ChronoUnit.DAYS).isBefore(Instant.now())) {
				return RESULT_PASSWORD_AGE;
			}
			
			return RESULT_SUCCESS;
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to authenticate account.  email ID: "+ emailID +", exception: "+e.toString());
			return "Unable to authenticate";
		}			
	}
		
	public static UserPassword createAccount(String emailID, String password) throws ValidationException {
		java.util.List<String> errors = validateAccount(emailID, password);
		if (errors.size() > 0) {
			throw new ValidationException("Email address or password is not valid", errors);
		}
		
		byte[] salt = UserPassword.getNextSalt();
		byte[] hashedPassword = UserPassword.hash(password, salt);
		UserPassword record = new UserPassword(emailID, hashedPassword, salt, "noTokenAssigned", false, Instant.now(), Instant.ofEpochSecond(0), false);
		if  (new UserPasswordDAO().create(record)) {
			return record;
		}
		else {
			return null;
		}
	}

	private static Pattern upperCasePattern = Pattern.compile("[A-Z]");
	private static Pattern lowerCasePattern = Pattern.compile("[a-z]");
	private static Pattern digitPattern     = Pattern.compile("[0-9]");
	private static Pattern otherCharPattern = Pattern.compile("[^0-9A-Za-z]");
	
	/**
	 * Validates the email ID and password.  Assumes both parameters have already been trimmed before passing.
	 * 
	 * Implementation note: probably would have been faster to just loop through the password to check for characters than using regexes.
	 * 
	 * @param emailID
	 * @param password
	 * @return empty list if validation found no errors.
	 */
	public static java.util.List<String> validateAccount(String emailID, String password) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();
		
		if (StringValidation.isValidEmailAddress(emailID) == false) {
			errors.add("Invalid email address: "+emailID);
		}
		
		if (password.length() < Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LOCAL_MIN_PASSWORD_LENGTH)) {
			errors.add("Password is too short.  Minimum length: "+Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LOCAL_MIN_PASSWORD_LENGTH));
		}
		if (password.length() > Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LOCAL_MAX_PASSWORD_LENGTH)) {
			errors.add("Password is too short.  Maximum length: "+Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LOCAL_MAX_PASSWORD_LENGTH));
		}
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LOCAL_REQUIRED_LOWERCASE) &&
				!lowerCasePattern.matcher(password).find()) {
			errors.add("The password requires at least one lower case character.");			
		}
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LOCAL_REQUIRED_UPPERCASE) &&
				!upperCasePattern.matcher(password).find()) {
			errors.add("The password requires at least one upper case character.");			
		}
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LOCAL_REQUIRED_DIGIT) &&
				!digitPattern.matcher(password).find()) {
			errors.add("The password requires at least one number.");			
		}
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LOCAL_REQUIRED_SPECIAL) &&
				!otherCharPattern.matcher(password).find()) {
			errors.add("The password requires at least one character other than a number or letter.");			
		}

		return errors;
	}

	public static boolean changePassword(String emailID, String newPassword) {

		try {
			UserPasswordDAO upd = new UserPasswordDAO();
			UserPassword record = upd.retrieve(emailID);
			if (record == null) {
				logger.log(Level.WARNING, "Unable to change password - no password record exists, email ID: "+ emailID);
				return false;
			}
			record._salt = getNextSalt();
			record._password =  hash(newPassword, record._salt);
			return upd.updatePassword(record);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to change password.  email ID: "+ emailID +", exception: "+e.toString());
			return false;
		}		
		

		
	}

	public static String authenticateUserLDAP(String userID, String password) {
		
        java.util.Hashtable<String,String> props = new java.util.Hashtable<String,String>();
        String principalName = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LDAP_DN_FORMAT).replace("USERID", userID);
        props.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        
        boolean discoveredLDAPServer = false;
        try {
        	String activeDirectoryDomain = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LDAP_ACTIVE_DIRECTORY_DOMAIN);
        	if (activeDirectoryDomain != null && !activeDirectoryDomain.trim().equals("")) {
        		String discoveredController = _activeDirectoryCache.get(activeDirectoryDomain);  //utilize a cache with a 5 minute period to avoid constantly querying DNS on every authentication.
        		String url = "ldaps://"+discoveredController+":636";
        		props.put(javax.naming.Context.PROVIDER_URL,url);
        	}
        }
        catch (Exception e) {
        	logger.log(Level.WARNING,"Unable to discover active directory ldap server: "+e.toString());
        }
        
        if (!discoveredLDAPServer) {
        	props.put(javax.naming.Context.PROVIDER_URL, Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_LDAP_SERVER) );
        }
        
        
        props.put(javax.naming.Context.SECURITY_PRINCIPAL, principalName);
        props.put(javax.naming.Context.SECURITY_CREDENTIALS, password);
        props.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
        
        javax.naming.directory.DirContext context;

        try {
            context =  new InitialDirContext(props);
            context.close();
            return RESULT_SUCCESS;
        } catch (AuthenticationException a) {
            logger.log(Level.WARNING, "Unable to authenticate user, invalid credentials,  userID: "+ userID);
            return RESULT_INVALID_USER_PASSWORD;
        } catch (NamingException e) {
        	logger.log(Level.WARNING, "Unable to authenticate user, invalid credentials,  userID: "+ userID,e);
            return RESULT_INVALID_USER_PASSWORD;
        }		
	}
	
	
	private static final String[] SRV = new String[] { "SRV" };

	public static Collection<InetSocketAddress> srv(String name)  throws NamingException {
	    DirContext ctx = new InitialDirContext();

	    Attributes attrs = ctx.getAttributes("dns:/" + name, SRV);
	    if (attrs.get("SRV") == null) {
	        return Collections.emptyList();
	    }

	    NamingEnumeration<?> e = attrs.get("SRV").getAll();
	    TreeMap<Integer, InetSocketAddress> result = new TreeMap<Integer, InetSocketAddress>();

	    while(e.hasMoreElements()) {
	        String line = (String) e.nextElement();

	        // The line is priority weight port host
	        String[] parts = line.split("\\s+");

	        int prio = Integer.parseInt(parts[0]);
	        int port = Integer.parseInt(parts[2]);
	        String host = parts[3];

	        result.put(prio, new InetSocketAddress(host, port));
	    }

	    return result.values();
	}
	
	private static com.google.common.cache.LoadingCache<String,String> _activeDirectoryCache = com.google.common.cache.CacheBuilder.newBuilder()
			.expireAfterWrite(300, TimeUnit.SECONDS)
            .recordStats()
            .build(new CacheLoader<String, String>() {
                public String load(String activeDirectoryDomain) throws Exception {
                    return discoverActiveDomainControllerViaDNS(activeDirectoryDomain);
                  }
                });
	

	/** 
	 * For a given domain (such as test.com), attempt to find the active directory controllers by
	 * querying DNS SRV records by prepending _ldap._tcp.dc._msdcs. to the domain name.
	 * 
	 * https://technet.microsoft.com/pt-pt/library/cc759550(v=ws.10).aspx
	 * 
	 * @param domain
	 * @return the first srv record where the host name field is defined.
	 * @throws NamingException
	 */
	public static String discoverActiveDomainControllerViaDNS(String domain) throws NamingException {
		Collection<InetSocketAddress> results = srv("_ldap._tcp.dc._msdcs."+ domain);
		if (results.isEmpty()) {
			return null;
		}
		for (InetSocketAddress a: results) {
			if (a.getHostName() != null) { return a.getHostName(); }
		}
		return null;
	}	
	
}
