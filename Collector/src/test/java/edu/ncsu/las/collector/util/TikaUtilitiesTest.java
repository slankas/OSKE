package edu.ncsu.las.collector.util;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 *
 */
public class TikaUtilitiesTest {

    @BeforeClass
    public void setUp() {

    }	
	
    @Test
	public void testLanguageDetection() {
		long time = System.currentTimeMillis();
		String lang1 = TikaUtilities.detectLanguage("In China’s swiftly evolving new world of state surveillance, there are fewer and fewer private spaces. Authorities who once had to use informants to find out what people said in private now rely on a vast web of new technology. They can identify citizens as they walk down the street, monitor their online behavior and snoop on cellphone messaging apps to identify suspected malcontents.");
		long time2 = System.currentTimeMillis();
		String lang2 = TikaUtilities.detectLanguage("BILD bietet Ihnen Nachrichten rund um die Uhr. Unsere 500 Reporter berichten für Sie aus aller Welt. Um das zu ermöglichen, sind wir auch auf Werbeeinnahmen angewiesen.");
		long time3 = System.currentTimeMillis();
		
		System.out.println(time2-time);
		System.out.println(time3-time2);
		System.out.println(lang1);
		System.out.println(lang2);

	}
	
	
}
