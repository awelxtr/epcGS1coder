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
	private final int padding = 10;
	
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

	public String getEpc() {
		if (epc == null){
			epc = new BitSet(52*4); //Sgtin-198 epc is 52 hex chars long
			int i = serialSize+padding-1;

			for (byte b : serial.getBytes()) {
				for (int j = 6; j >= 0; j--,i--)
					epc.set(i, ((b >> j) & 1) == 1);
			}

			i = serialSize+padding;
			for (int j = 0; j < getItemReferenceBits(this.getPartition()); j++,i++)
				epc.set(i, ((this.getItemReference() >> j) & 1)==1);

			for (int j = 0; j < getCompanyPrefixBits(this.getPartition()); j++,i++)
				epc.set(i, ((this.getCompanyPrefix() >> j) & 1)==1);

			for (int j = 0; j < 3; j++,i++)
				epc.set(i, ((this.getPartition() >> j) & 1)==1);

			for (int j = 0; j < 3; j++,i++)
				epc.set(i, ((this.getFilter().getValue() >> j) & 1)==1);

			for (int j = 0; j < 8; j++,i++)
				epc.set(i, ((epcHeader >> j) & 1)==1);
		}
		byte[] epcba = epc.toByteArray();
		StringBuffer sb = new StringBuffer(52);
		for (int i = epcba.length-1; i>=0; i--)
			sb.append(String.format("%02X",epcba[i]));

		return sb.toString();
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
			uri = "urn:epc:tag:sgtin-198:"+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getItemReferenceDigits(partition)+"d",itemReference)+"."+serial.chars().mapToObj(c -> getUriSerialChar((char) c)).collect(Collectors.joining());
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

	/**
	 * Table 14-2 SGTIN Partition Table
	 * @param partition
	 * @return M value
	 */
	private byte getCompanyPrefixBits(int partition){
		switch (partition){
			case 0:
				return 40;
			case 1:
				return 37;
			case 2:
				return 34;
			case 3:
				return 30;
			case 4:
				return 27;
			case 5:
				return 24;
			case 6:
				return 20;
			default:
				throw new RuntimeException("Invalid Partition: " + partition + " (0-6)");
		}
	}

	/**
	 * Table 14-2 SGTIN Partition Table
	 * @param partition
	 * @return N value
	 */
	private byte getItemReferenceBits(int partition){
		switch (partition){
			case 0:
				return 4;
			case 1:
				return 7;
			case 2:
				return 10;
			case 3:
				return 14;
			case 4:
				return 17;
			case 5:
				return 20;
			case 6:
				return 24;
			default:
				throw new RuntimeException("Invalid Partition: " + partition + " (0-6)");
		}
	}

	/**
	 * Table 14-2 SGTIN Partition Table
	 * @param companyPrefixDigits (L) value
	 * @return P value
	 */
	private int getPartition(int companyPrefixDigits){
		return 12-companyPrefixDigits;
	}

	/**
	 * Table 14-2 SGTIN Partition Table
	 * @param P
	 * @return L
	 */
	private int getCompanyPrefixDigits(int partition){
		return 12-partition;
	}

	/**
	 * Table 14-2 SGTIN Partition Table
	 * @param P
	 */
	private int getItemReferenceDigits(int partition){
		return partition+1;
	}
}
