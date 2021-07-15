package org.java.epcGS1coder.sgln;

import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.stream.Collectors;

/**
 * The Extensionised Global Trade Item Number EPC scheme is used to assign a unique
 * identity to an instance of a trade item, such as a specific instance 
 * of a product or SKU.
 * 
 * @author awel
 *
 */

public class Sgln195 extends Sgln {

	private final static byte epcHeader = 0b00111001;
	private final static int extensionSize = 140;
	private final static int padding = 13;
	private final static String uriHeader = "urn:epc:tag:sgln-195:";
	
	private BitSet epc;
	
	private byte partition;
	private SglnFilter filter;
	private long companyPrefix;
	private int locationReference;
	private String extension;
	private String uri = null;

	Sgln195(int filter,
					int companyPrefixDigits,
					long companyPrefix,
					int locationReference,
					String extension){
		this.filter = SglnFilter.values()[filter];
		this.partition = (byte) getPartition(companyPrefixDigits);
		this.companyPrefix = companyPrefix;
		this.locationReference = locationReference;
		this.extension = extension;
	}

	public static Sgln195 fromFields(int filter,
									  int companyPrefixDigits,
									  long companyPrefix,
									  int locationReference,
									  String extension){
		return new Sgln195(filter,companyPrefixDigits,companyPrefix,locationReference,extension);
	}

	public static Sgln195 fromGs1Key(int filter,int companyPrefixDigits, String ai414, String ai254) {
        if (ai414.length()<13 || !StringUtils.isNumeric(ai414))
            throw new RuntimeException("GLN must be 13 digits long");

		return new Sgln195(filter, companyPrefixDigits, Long.parseLong(ai414.substring(0, companyPrefixDigits)), Integer.parseInt(ai414.substring(companyPrefixDigits, 13-1)), ai254);
	}

	public String getEpc() {
		if (epc == null){
			epc = new BitSet(52*4); //Sgln-195 epc is 52 hex chars long
			int i = extensionSize+padding-1;

			for (byte b : extension.getBytes()) {
				for (int j = 6; j >= 0; j--,i--)
					epc.set(i, ((b >> j) & 1) == 1);
			}

			i = extensionSize+padding;
			for (int j = 0; j < getLocationReferenceBits(partition); j++,i++)
				epc.set(i, ((locationReference >> j) & 1)==1);

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

	public int getLocationReference() {
		return locationReference;
	}

	public String getExtension() {
		return extension;
	}

	public String getUri() {
		if (uri == null)
			uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getLocationReferenceDigits(partition)+"d",locationReference)+"."+extension.chars().mapToObj(c -> getUriExtensionChar((char) c)).collect(Collectors.joining());
		return uri;
	}

	void setEpc(BitSet epc){ this.epc = epc; }
	void setUri(String uri){ this.uri = uri; }

	/**
	 * Table A-1 for the encoding
	 */
	private String getUriExtensionChar(char ch){
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

	public static Sgln195 fromUri(String uri) {
		if (!uri.startsWith(uriHeader))
			throw new RuntimeException("Decoding error: wrong URI header, expected " + uriHeader);

		String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
		int filter = Integer.parseInt(uriParts[0]);
		byte partition = (byte) getPartition(uriParts[1].length());
		long companyPrefix = Long.parseLong(uriParts[1]);
		int locationReference = Integer.parseInt(uriParts[2]);

		String extension = uriParts[3];
		StringBuilder sb = new StringBuilder();
		String[] extensionSplit = extension.split("%");
		sb.append(extensionSplit[0]);
		for (int i = 1; i < extensionSplit.length; i++){
			sb.append((char) Integer.parseInt(extensionSplit[i].substring(0,2),16));
			sb.append(extensionSplit[i].substring(2));
		}

		Sgln195 sgln195 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,locationReference,sb.toString());
		sgln195.setUri(uri);
		return sgln195;
	}

	public static Sgln195 fromEpc(String epc) {
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

		byte irb = getLocationReferenceBits(partition);
		for(tmp = 0, i = 208 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 208 - 8 - 3 - 3 - cpb - irb - 1;)
			tmp+=1L<<(i-(208-8-3-3-cpb-irb));
		int locationReference = (int) tmp;

		StringBuilder extensionBuilder = new StringBuilder("");
		byte[] tmpba;

		i =208-55; //buffer size - epcheader.size - filter.size - partition.size - getCompanyPrefixBits(partition) - getLocationReferenceBits(partition)
		for(int j = 0;j < 20 && (tmpba = bs.get(i-7,i).toByteArray()).length!=0;i-=7,j++)
			extensionBuilder.append(new String(tmpba));

		String extension = extensionBuilder.toString();

		Sgln195 sgln195 = new Sgln195(filter,getCompanyPrefixDigits(partition),companyPrefix,locationReference,extension);
		sgln195.setEpc(bs);
		return sgln195;
	}

}
