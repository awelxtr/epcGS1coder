package org.java.epcGS1coder.sgtin;

import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
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

public class Sgtin198 extends Sgtin {

	private final static byte epcHeader = 0b00110110;
	private final static int serialSize = 140;
	private final static int padding = 10;
	private final static String uriHeader = "urn:epc:tag:sgtin-198:";
	
	private BitSet epc;
	
	private byte partition;
	private SgtinFilter filter;
	private long companyPrefix;
	private int itemReference;
	private String serial;
	private String uri = null;

	public Sgtin198(int filter,
					int companyPrefixDigits,
					long companyPrefix,
					int itemReference,
					String serial){
		this.filter = SgtinFilter.values()[filter];
		this.partition = (byte) getPartition(companyPrefixDigits);
		this.companyPrefix = companyPrefix;
		this.itemReference = itemReference;
		this.serial = serial;
	}

	public static Sgtin198 fromFields(int filter,
									  int companyPrefixDigits,
									  long companyPrefix,
									  int itemReference,
									  String serial){
		return new Sgtin198(filter,companyPrefixDigits,companyPrefix,itemReference,serial);
	}

	public static Sgtin198 fromGs1Key(int filter,int companyPrefixDigits, String ai01, String ai21) {
		if (ai01.length()<14 || !StringUtils.isNumeric(ai01))
			throw new RuntimeException("GTIN must be 14 digits long");

		return fromUri(uriHeader + filter + "." + ai01.substring(1, companyPrefixDigits + 1) + "." + ai01.charAt(0) + ai01.substring(companyPrefixDigits + 1, 14 - 1) + "." + ai21);
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
			for (int j = 0; j < getItemReferenceBits(partition); j++,i++)
				epc.set(i, ((itemReference >> j) & 1)==1);

			for (int j = 0; j < getCompanyPrefixBits(partition); j++,i++)
				epc.set(i, ((companyPrefix >> j) & 1)==1);

			for (int j = 0; j < 3; j++,i++)
				epc.set(i, ((partition >> j) & 1)==1);

			for (int j = 0; j < 3; j++,i++)
				epc.set(i, ((filter.getValue() >> j) & 1)==1);

			for (int j = 0; j < 8; j++,i++)
				epc.set(i, ((epcHeader >> j) & 1)==1);
		}
		byte[] epcba = epc.toByteArray();
		StringBuffer sb = new StringBuffer(52);
		for (int i = epcba.length-1; i>=0; i--)
			sb.append(String.format("%02X",epcba[i]));

		return sb.toString();
	}

	public int getFilter() {
		return filter.getValue();
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

	public String getUri() {
		if (uri == null)
			uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getItemReferenceDigits(partition)+"d",itemReference)+"."+serial.chars().mapToObj(c -> getUriSerialChar((char) c)).collect(Collectors.joining());
		return uri;
	}

	void setEpc(BitSet epc){ this.epc = epc; }
	void setUri(String uri){ this.uri = uri; }

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

	public static Sgtin198 fromUri(String uri) {
		if (!uri.startsWith(uriHeader))
			throw new RuntimeException("Decoding error: wrong URI header, expected " + uriHeader);

		String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
		int filter = Integer.parseInt(uriParts[0]);
		byte partition = (byte) getPartition(uriParts[1].length());
		long companyPrefix = Long.parseLong(uriParts[1]);
		int itemReference = Integer.parseInt(uriParts[2]);

		String serial = uriParts[3];
		StringBuilder sb = new StringBuilder();
		String[] serialSplit = serial.split("%");
		sb.append(serialSplit[0]);
		for (int i = 1; i < serialSplit.length; i++){
			sb.append((char) Integer.parseInt(serialSplit[i].substring(0,2),16));
			sb.append(serialSplit[i].substring(2));
		}

		Sgtin198 sgtin198 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,itemReference,sb.toString());
		sgtin198.setUri(uri);
		return sgtin198;
	}

	public static Sgtin198 fromEpc(String epc) {
		ArrayList<String> a = new ArrayList<String>();
		for (int i = 0; i<epc.length(); i+=2) {
			a.add(epc.substring(i, i+2));
		}

		ByteBuffer bb = ByteBuffer.allocate(26);
		for (int i = a.size() - 1; i>=0;i--)
			bb.put((byte) Integer.parseInt(a.get(i),16));
		bb.rewind();

		BitSet bs = BitSet.valueOf(bb);

		int i;
		long tmp;

		for(tmp = 0, i = 208; (i = bs.previousSetBit(i-1)) > 208 - 8 - 1;)
			tmp+=1L<<(i-(208-8));
		if (tmp != epcHeader)
			throw new RuntimeException("Invalid header"); //maybe the decoder could choose the structure from the header?

		for(tmp = 0, i = 208 - 8; (i = bs.previousSetBit(i-1)) > 208 - 8 - 3 - 1;)
			tmp+=1L<<(i-(208-8-3));
		int filter = (int) tmp;

		for(tmp = 0, i = 208 - 8 - 3; (i = bs.previousSetBit(i-1)) > 208 - 8 - 3 - 3 - 1;)
			tmp+=1L<<(i-(208-8-3-3));
		byte partition = (byte) tmp;

		byte cpb = getCompanyPrefixBits(partition);
		for(tmp = 0, i = 208 - 8 - 3 - 3; (i = bs.previousSetBit(i-1)) > 208 - 8 - 3 - 3 - cpb - 1;)
			tmp+=1L<<(i-(208-8-3-3-cpb));
		long companyPrefix = tmp;

		byte irb = getItemReferenceBits(partition);
		for(tmp = 0, i = 208 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 208 - 8 - 3 - 3 - cpb - irb - 1;)
			tmp+=1L<<(i-(208-8-3-3-cpb-irb));
		int itemReference = (int) tmp;

		StringBuilder serialBuilder = new StringBuilder("");
		byte[] tmpba;

		i =208-58;
		for(int j = 0;j < 20 && (tmpba = bs.get(i-7,i).toByteArray()).length!=0;i-=7,j++)
			serialBuilder.append(new String(tmpba));

		String serial = serialBuilder.toString();

		Sgtin198 sgtin198 = new Sgtin198(filter,getCompanyPrefixDigits(partition),companyPrefix,itemReference,serial);
		sgtin198.setEpc(bs);
		return sgtin198;
	}

}
