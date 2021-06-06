package org.java.epcGS1coder.sgtin;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The Serialised Global Trade Item Number EPC scheme is used to assign a unique
 * identity to an instance of a trade item, such as a specific instance 
 * of a product or SKU.
 *
 * SGTIN Table data: Table 14-3 SGTIN-96, line 3591, page 106
 * 
 * @author awel
 *
 */

public class Sgtin96 {

	private final byte epcHeader = 0b00110000;
	private final byte serialSize = 38;
	private final byte GtinMaxSize = 44;
	
	private BitSet epc;
	
	private SgtinFilter filter;
	private byte partition;
	
	private long companyPrefix;
	private int itemReference;
	private long serial; 
	
	private String uri = null;
	
	Sgtin96(int filter,
			byte partition,
			long companyPrefix,
			int itemReference,
			long serial){
		this(SgtinFilter.values()[filter], partition, companyPrefix, itemReference, serial);
	}

	Sgtin96(SgtinFilter filter,
			byte partition,
			long companyPrefix,
			int itemReference,
			long serial){
		this.filter = filter;
		this.partition = partition;
		this.companyPrefix = companyPrefix;
		this.itemReference = itemReference;
		this.serial = serial;
	}

	public String getEpc() {
		StringBuffer sb = new StringBuffer(24);
		byte[] epcba = epc.toByteArray();
		for (int i = epcba.length-1; i>=0; i--)
			sb.append(String.format("%02X",epcba[i]));

		return sb.toString();
	}
	void setEpc(BitSet epc) { this.epc = epc; }
	void setEpc(String epc) {
		ArrayList<String> a = new ArrayList<String>();
		for (int i = 0; i<epc.length(); i+=2) {
			a.add(epc.substring(i, i+2));
		}

		ByteBuffer bb = ByteBuffer.allocate(12);
		a.stream().map(s -> Integer.parseInt(s, 16)).map(Integer::byteValue).forEach(bb::put);
		bb.rewind();

		this.epc = BitSet.valueOf(bb);
	}
	
	public SgtinFilter getFilter() {
		return filter;
	}

	public byte getPartition() {
		return partition;
	}

	public long getCompanyPrefix() {
		return companyPrefix;
	}

	public int getItemReference() {
		return itemReference;
	}

	public long getSerial() {
		return serial;
	}

	public String getPureUri(){
		return "urn:epc:id:sgtin:"+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getItemReferenceDigits(partition)+"d",itemReference)+"."+String.valueOf(serial);
	}

	public String getUri(){
		if (uri == null)
			uri = "urn:epc:tag:sgtin-96:"+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getItemReferenceDigits(partition)+"d",itemReference)+"."+String.valueOf(serial);
		return uri;
	}
	void setUri(String uri){
		this.uri = uri;
	};

	/**
	 * Table 14-2 SGTIN Partition Table
	 * @param partition
	 * @return M value
	 */
	private static byte getCompanyPrefixBits(int partition){
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
	private static byte getItemReferenceBits(int partition){
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
	private static int getPartition(int companyPrefixDigits){
		return 12-companyPrefixDigits;
	}

	/**
	 * Table 14-2 SGTIN Partition Table
	 * @param P
	 * @return L
	 */
	private static int getCompanyPrefixDigits(int partition){
		return 12-partition;
	}

	/**
	 * Table 14-2 SGTIN Partition Table
	 * @param P
	 */
	private static int getItemReferenceDigits(int partition){
		return partition+1;
	}
}
