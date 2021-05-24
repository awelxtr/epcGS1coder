package org.java.epcGS1coder.sgtin;

import java.math.BigInteger;
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
	private BigInteger serial; 
	
}
