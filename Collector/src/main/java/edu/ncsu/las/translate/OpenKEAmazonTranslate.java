package edu.ncsu.las.translate;

import java.util.AbstractMap.SimpleEntry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClient;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;

import com.google.common.util.concurrent.RateLimiter;

import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.nlp.SentenceSegmenter;

public class OpenKEAmazonTranslate {
	/** what is the maximum text length that can be translated at a time by AWS */
	public static final int MAX_TEXT_LENGTH_TRANSLATABLE = 5000; 
	
	public static final int MAX_CHARACTERS_TRANSLATED_PER_SECOND = 95000; // should be put into configuration
	
	private static Logger logger = Logger.getLogger(Domain.class.getName());

	private static final String REGION = "us-east-1";
	private static String testKey = "";
	private static String testkeySecret = "";

	private String awskey;
	private String awskeySecret;
	private String awsRegion;
	
	private String translatedDoc;

	private RateLimiter requestRateLimiter;
	private RateLimiter sizeRateLimiter;

	private BasicAWSCredentials awsCredentials;

	private static OpenKEAmazonTranslate _amazonTranslator = null;


	public static OpenKEAmazonTranslate getTheAmazonTranslator() {
		if (_amazonTranslator == null) {
			_amazonTranslator = new OpenKEAmazonTranslate();
		}
		return _amazonTranslator;

	}

	private OpenKEAmazonTranslate() {
		this.awskey = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.AWS_KEY);
		this.awskeySecret = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.AWS_SECRETKEY);
		this.awsRegion = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.AWS_REGION);
		this.requestRateLimiter = RateLimiter.create(Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM, ConfigurationType.AWS_TRANSLATE_MAX_TPS));
		this.sizeRateLimiter = RateLimiter.create(MAX_CHARACTERS_TRANSLATED_PER_SECOND);
		this.awsCredentials = new BasicAWSCredentials(awskey, awskeySecret);
	}

	/**
	 * Translate text
	 * 
	 * @param srclang
	 * @param destlang
	 * @param text
	 * @return translated text. Returns null upon any error with messages to the
	 *         console
	 */
	public String getTranslation(String srclang, String destlang, String text) {
		logger.log(Level.INFO, "getTranslation: src,dest = " + srclang + "," + destlang);

		// Break text into sentences if necessary
		List<String> sentences = new java.util.ArrayList<String>();

		if (srclang != null && destlang != null && text != null) {
			if (text.length() < MAX_TEXT_LENGTH_TRANSLATABLE) {
				sentences.add(text);
			} else {
				// sentences = Arrays.asList(text.split("\r?\n"));
				try {
					sentences = SentenceSegmenter.segment(text, srclang);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "getTranslation- SentanceSegmenter error: " + e);
					return null;
				}
				sentences = combineSentences(sentences);
			}

			AmazonTranslate translate = AmazonTranslateClient.builder().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(awsRegion).build();

			// translate sentences
			StringBuilder sb = new StringBuilder();
			for (String sentence : sentences) {
				requestRateLimiter.acquire();
				sizeRateLimiter.acquire(sentence.length());
				TranslateTextRequest request = new TranslateTextRequest().withText(sentence).withSourceLanguageCode(srclang).withTargetLanguageCode(destlang);

				try {
					System.out.println("Request text:" + sentence);
					TranslateTextResult result = translate.translateText(request);
					sb.append(result.getTranslatedText());
					System.out.println("Request text:" + sentence);
					System.out.println("Response:" + result.getTranslatedText());

				} 
				catch (com.amazonaws.services.translate.model.AmazonTranslateException ate) {
					if (ate.getStatusCode() == 429) {
						try {
							TimeUnit.SECONDS.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("Request text:" + sentence);
						TranslateTextResult result = translate.translateText(request);
						sb.append(result.getTranslatedText());
						System.out.println("Request text:" + sentence);
						System.out.println("Response:" + result.getTranslatedText());
					}
					logger.log(Level.SEVERE, "Amazon Translate Exception: " + ate.getMessage());
				}
				catch (com.amazonaws.AbortedException e) {
					logger.log(Level.SEVERE, "Aborted Exception: " + e.getMessage(), e);
					System.out.println("Request text:" + sentence);
					sb.append(sentence);
					translate.shutdown();
					translate = AmazonTranslateClient.builder().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(awsRegion).build();
					
				} catch (com.amazonaws.AmazonClientException e) {
					logger.log(Level.SEVERE, "getTranslation- Amazon Translate error: " + e.getMessage(), e);
					System.out.println("Request text:" + sentence);
					sb.append(sentence);
					translate.shutdown();
					translate = AmazonTranslateClient.builder().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(awsRegion).build();
				} catch (com.amazonaws.SdkBaseException e) {
					logger.log(Level.SEVERE, "getTranslation- Amazon Translate error: " + e.getMessage(), e);
					System.out.println("Request text:" + sentence);
					sb.append(sentence);
					translate.shutdown();
					translate = AmazonTranslateClient.builder().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(awsRegion).build();
				}
			}

			translatedDoc = sb.toString();
			System.out.println("******** OpenKEAmazon getTranslation: translatedDoc = " + translatedDoc); // TODO: remove
			translate.shutdown();
		}
		
		return translatedDoc;
	}

	public static java.util.List<String> combineSentences(List<String> sentences) {
		java.util.ArrayList<String> result = new java.util.ArrayList<String>();
		
		StringBuilder currentSentence = new StringBuilder();
		for (String s: sentences) {
			if (s.trim().equals("")) { continue; }  // skip blank lines
			if (s.length() > MAX_TEXT_LENGTH_TRANSLATABLE) {
				if (currentSentence.length() > 0) {
					result.add(currentSentence.toString());
					result.add(s);  // May not be translated due to length issues
					currentSentence = new StringBuilder();
				}
			}
			else if (s.length() + currentSentence.length() > (MAX_TEXT_LENGTH_TRANSLATABLE - 250) ) {
				if (currentSentence.length() > 0) {
					result.add(currentSentence.toString());
				}
				currentSentence = new StringBuilder(s);
			}
			else {
				if (currentSentence.length() > 0) {
					currentSentence.append("\n");
				}
				currentSentence.append(s);
			}
			
		}
		if (currentSentence.length() > 0) {
			result.add(currentSentence.toString());
		}
		
		
		return result;
	}

	public Map<String, String> getSupportedLanguages() {
		return Collections.unmodifiableMap(Stream
				.of(new SimpleEntry<>("Arabic", "ar"), new SimpleEntry<>("Chinese (Simplified)", "zh"), new SimpleEntry<>("Chinese (Traditional)", "zh-TW"), new SimpleEntry<>("Czech", "cs"), new SimpleEntry<>("Danish", "da"), new SimpleEntry<>("Dutch", "nl"), new SimpleEntry<>("English", "en"),
						new SimpleEntry<>("Finnish", "fi"), new SimpleEntry<>("French", "fr"), new SimpleEntry<>("German", "de"), new SimpleEntry<>("Hebrew", "he"), new SimpleEntry<>("Indonesian", "id"), new SimpleEntry<>("Italian", "it"), new SimpleEntry<>("Japanese", "ja"),
						new SimpleEntry<>("Korean", "ko"), new SimpleEntry<>("Polish", "pl"), new SimpleEntry<>("Portuguese", "pt"), new SimpleEntry<>("Russian", "ru"), new SimpleEntry<>("Spanish", "es"), new SimpleEntry<>("Swedish", "sv"), new SimpleEntry<>("Turkish", "tr"))
				.collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
	}

	public static void main(String[] args) {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(testKey, testkeySecret);

		AmazonTranslate translate = AmazonTranslateClient.builder().withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion(REGION).build();

		TranslateTextRequest request = new TranslateTextRequest().withText("election,fraud,voter,recount").withSourceLanguageCode("en").withTargetLanguageCode("es");
		TranslateTextResult result = translate.translateText(request);
		System.out.println(result.getTranslatedText());

	}

}