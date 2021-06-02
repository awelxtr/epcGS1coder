package org.java.epcGS1coder.sgtin;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;

/**
 * The Serialised Global Trade Item Number EPC scheme is used to assign a unique
 * identity to an instance of a trade item, such as a specific instance 
 * of a product or SKU.
 * 
 * @author awel
 *
 */

public class Sgtin198 {

	private final byte epcHeader = 0b00110110;
	private final int serialSize = 140;
	
	private BitSet epc;
	
	private byte partition;
	private SgtinFilter filter;
	private long companyPrefix;
	private int itemReference;
	private String serial;
	private String uri = null;

	public Sgtin198(int filter,
					byte partition,
					long companyPrefix,
					int itemReference,
					String serial){
		this(SgtinFilter.values()[filter], partition, companyPrefix, itemReference, serial);
	}

	public Sgtin198(SgtinFilter filter,
					byte partition,
					long companyPrefix,
					int itemReference,
					String serial){
		this.filter = filter;
		this.partition = partition;
		this.companyPrefix = companyPrefix;
		this.itemReference = itemReference;
		this.serial = serial;
	}

	public BitSet getEpc() {
		return epc;
	}

	public byte getPartition() {
		return partition;
	}

	public SgtinFilter getFilter() {
		return filter;
	}

	public long getCompanyPrefix() {
		return companyPrefix;
	}

	public int getItemReference() {
		return itemReference;
	}

	public String getSerial() {
		return serial;
	}

	public String getTagUri() {
		if (uri == null)
			uri = "urn:epc:tag:sgtin-198:"+String.valueOf(companyPrefix)+"."+String.valueOf(itemReference)+"."+serial.chars().mapToObj(c -> getUriSerialChar((char) c)).collect(Collectors.joining());
		return uri;
	}

	void setEpc(BitSet epc){ this.epc = epc; }

	private String getUriSerialChar(String ch){
		return getUriSerialChar(ch.charAt(0));
	}

	/**
	 * Table A-1 for the encoding
	 */
	private String getUriSerialChar(char ch){
		if (ch < 0x21 || ch > 0x7A)
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
