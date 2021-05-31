package org.java.epcGS1coder.sgtin;

import java.util.Arrays;
import java.util.BitSet;

/**
 * The Serialised Global Trade Item Number EPC scheme is used to assign a unique
 * identity to an instance of a trade item, such as a specific instance 
 * of a product or SKU.
 * 
 * @author awel
 *
 */

public class Sgtin198 {

	private final byte epcHeader = 0b00110000;
	private final int serialSize = 140;
	
	private BitSet epc;
	
	private byte partition;
	private SgtinFilter filter;
	private int companyPrefix;
	private String serial;

	private String uri = null;

	private String getUriSerialChar(String ch){
		return getUriSerialChar(ch.charAt(0));
	}

	//Table A-1 for the encoding
	private String getUriSerialChar(char ch){
		if (ch < 34 || ch > 90)
			throw new RuntimeException("Wrong char");
		switch (ch){
			case '"':
			case '%':
			case '&':
			case '/':
			case '<':
			case '>':
			case '?':
				return "%"+String.format("%02x",(int) ch).toUpperCase();
			default:
				return String.valueOf(ch);
		}
	}
}
