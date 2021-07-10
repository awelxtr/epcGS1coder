package org.java.epcGS1coder.grai;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class Grai170 extends Grai{
    private final static byte epcHeader = 0b00110111;
	private final static int serialSize = 112;
	private final static int padding = 6;
	private final static String uriHeader = "urn:epc:tag:grai-170:";
	
	private BitSet epc;
	
	private byte partition;
	private GraiFilter filter;
	private long companyPrefix;
	private int assetType;
	private String serial;
	private String uri = null;

	public Grai170(int filter,
					int companyPrefixDigits,
					long companyPrefix,
					int assetType,
					String serial){
		this.filter = GraiFilter.values()[filter];
		this.partition = (byte) getPartition(companyPrefixDigits);
		this.companyPrefix = companyPrefix;
		this.assetType = assetType;
		this.serial = serial;
	}

	public static Grai170 fromFields(int filter,
									  int companyPrefixDigits,
									  long companyPrefix,
									  int assetType,
									  String serial){
		return new Grai170(filter, companyPrefixDigits, companyPrefix, assetType, serial);
	}

	public static Grai170 fromGs1Key(int filter,int companyPrefixDigits, String ai8003) {
        if (ai8003.length()<14 || !StringUtils.isNumeric(ai8003.substring(0, 13)))
            throw new RuntimeException("GRAI (must be numeric) with serial must be 14 digits long");

        return new Grai170(filter, companyPrefixDigits, Long.parseLong(ai8003.substring(0, companyPrefixDigits)), Integer.parseInt(ai8003.substring(companyPrefixDigits, 13 - 1)), ai8003.substring(13));
	}

	public String getEpc() {
		if (epc == null){
			epc = new BitSet(44*4); //Grai-170 epc is 44 hex chars long
			int i = serialSize+padding-1;

			for (byte b : serial.getBytes()) {
				for (int j = 6; j >= 0; j--,i--)
					epc.set(i, ((b >> j) & 1) == 1);
			}

			i = serialSize+padding;
			for (int j = 0; j < getAssetTypeBits(partition); j++,i++)
				epc.set(i, ((assetType >> j) & 1)==1);

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
		StringBuffer sb = new StringBuffer(44);
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

	public int getAssetType() {
		return assetType;
	}

	public String getSerial() {
		return serial;
	}

	public String getUri() {
		if (uri == null)
			uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getAssetTypeDigits(partition)+"d",assetType)+"."+serial.chars().mapToObj(c -> getUriSerialChar((char) c)).collect(Collectors.joining());
		return uri;
	}

	void setEpc(BitSet epc){ this.epc = epc; }
	void setUri(String uri){ this.uri = uri; }

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

	public static Grai170 fromUri(String uri) {
		if (!uri.startsWith(uriHeader))
			throw new RuntimeException("Decoding error: wrong URI header, expected " + uriHeader);

		String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
		int filter = Integer.parseInt(uriParts[0]);
		byte partition = (byte) getPartition(uriParts[1].length());
		long companyPrefix = Long.parseLong(uriParts[1]);
		int assetType = Integer.parseInt(uriParts[2]);

		String serial = uriParts[3];
		StringBuilder sb = new StringBuilder();
		String[] serialSplit = serial.split("%");
		sb.append(serialSplit[0]);
		for (int i = 1; i < serialSplit.length; i++){
			sb.append((char) Integer.parseInt(serialSplit[i].substring(0,2),16));
			sb.append(serialSplit[i].substring(2));
		}

		Grai170 grai170 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,assetType,sb.toString());
		grai170.setUri(uri);
		return grai170;
	}

	public static Grai170 fromEpc(String epc) {
		ArrayList<String> a = new ArrayList<String>();
		for (int i = 0; i<epc.length(); i+=2) {
			a.add(epc.substring(i, i+2));
		}

		ByteBuffer bb = ByteBuffer.allocate(22);
		for (int i = a.size() - 1; i>=0;i--)
			bb.put((byte) Integer.parseInt(a.get(i),16));
		bb.rewind();

		BitSet bs = BitSet.valueOf(bb);

		int i;
		long tmp;

		for(tmp = 0, i = 176; (i = bs.previousSetBit(i-1)) > 176 - 8 - 1;)
			tmp+=1L<<(i-(176-8));
		if (tmp != epcHeader)
			throw new RuntimeException("Invalid header"); //maybe the decoder could choose the structure from the header?

		for(tmp = 0, i = 176 - 8; (i = bs.previousSetBit(i-1)) > 176 - 8 - 3 - 1;)
			tmp+=1L<<(i-(176-8-3));
		int filter = (int) tmp;

		for(tmp = 0, i = 176 - 8 - 3; (i = bs.previousSetBit(i-1)) > 176 - 8 - 3 - 3 - 1;)
			tmp+=1L<<(i-(176-8-3-3));
		byte partition = (byte) tmp;

		byte cpb = getCompanyPrefixBits(partition);
		for(tmp = 0, i = 176 - 8 - 3 - 3; (i = bs.previousSetBit(i-1)) > 176 - 8 - 3 - 3 - cpb - 1;)
			tmp+=1L<<(i-(176-8-3-3-cpb));
		long companyPrefix = tmp;

		byte irb = getAssetTypeBits(partition);
		for(tmp = 0, i = 176 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 176 - 8 - 3 - 3 - cpb - irb - 1;)
			tmp+=1L<<(i-(176-8-3-3-cpb-irb));
		int assetType = (int) tmp;

		StringBuilder serialBuilder = new StringBuilder("");
		byte[] tmpba;

		i =176-58; //buffer size - epcheader.size - filter.size - partition.size - getCompanyPrefixBits(partition) - getAssetTypeBits(partition)
		for(int j = 0;j < 20 && (tmpba = bs.get(i-7,i).toByteArray()).length!=0;i-=7,j++)
			serialBuilder.append(new String(tmpba));

		String serial = serialBuilder.toString();

		Grai170 grai170 = new Grai170(filter,getCompanyPrefixDigits(partition),companyPrefix,assetType,serial);
		grai170.setEpc(bs);
		return grai170;
	}
}
