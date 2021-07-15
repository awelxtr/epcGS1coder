package org.java.epcGS1coder.grai;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

public class Grai96 extends Grai {

    private final static byte epcHeader = 0b00110011;
	private final static byte serialSize = 38;
	private static final String uriHeader = "urn:epc:tag:grai-96:";
	
	private String epc = null;
	
	private GraiFilter filter;
	private byte partition;
	
	private long companyPrefix;
	private int assetType;
	private long serial; 
	
	private String uri = null;
	
	Grai96(int filter,
			int companyPrefixDigits,
			long companyPrefix,
			int assetType,
			long serial){
		this.filter = GraiFilter.values()[filter];
		this.partition = (byte) getPartition(companyPrefixDigits);
		this.companyPrefix = companyPrefix;
		this.assetType = assetType;
		this.serial = serial;
	}

	public String getEpc() {
		if (epc == null){
			BitSet epc = new BitSet(96);
			int i = 0;

			for (int j = 0; j < serialSize; j++,i++)
				epc.set(i, ((serial >> j) & 1)==1);

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

			byte[] epcba = epc.toByteArray();
			StringBuffer sb = new StringBuffer(epcba.length*2);
			for (i = epcba.length-1; i>=0; i--)
				sb.append(String.format("%02X",epcba[i]));

			this.epc = sb.toString();
		}
		return epc;
	}

	void setEpc(String epc) {
		this.epc = epc;
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

	public long getSerial() {
		return serial;
	}

	public String getUri(){
		if (uri == null)
			uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getAssetTypeDigits(partition)+"d",assetType)+"."+String.valueOf(serial);
		return uri;
	}
	void setUri(String uri){
		this.uri = uri;
	};

	public static Grai96 fromFields(int filter,
									int companyPrefixDigits,
									long companyPrefix,
									int assetType,
									long serial){
		return new Grai96(filter, companyPrefixDigits,companyPrefix,assetType,serial);
	}

	public static Grai96 fromGs1Key(int filter,int companyPrefixDigits, String ai8003) {
		if (ai8003.length()<14 || !StringUtils.isNumeric(ai8003))
			throw new RuntimeException("GRAI with serial must be 14 digits long");
		return new Grai96(filter, companyPrefixDigits, Long.parseLong(ai8003.substring(0, companyPrefixDigits)), Integer.parseInt(ai8003.substring(companyPrefixDigits, 13 - 1)), Long.parseLong(ai8003.substring(13)));
	}


	public static Grai96 fromUri(String uri) {
		if (!uri.startsWith(uriHeader))
			throw new RuntimeException("Decoding error: wrong URI header, expected " + uriHeader);

		String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
		int filter = Integer.parseInt(uriParts[0]);
		byte partition = (byte) getPartition(uriParts[1].length());
		long companyPrefix = Long.parseLong(uriParts[1]);
		int assetType = Integer.parseInt(uriParts[2]);
		long serial = Long.parseLong(uriParts[3]);

		Grai96 grai96 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,assetType,serial);
		grai96.setUri(uri);

		return grai96;
	}

	public static Grai96 fromEpc(String epc) {
		ArrayList<String> a = new ArrayList<String>();
		for (int i = 0; i<epc.length(); i+=2) {
			a.add(epc.substring(i, i+2));
		}

		ByteBuffer bb = ByteBuffer.allocate(96/8);
		for (int i = a.size() - 1; i>=0;i--)
			bb.put((byte) Integer.parseInt(a.get(i),16));
		bb.rewind();

		BitSet bs = BitSet.valueOf(bb);

		int i;
		long tmp;

		for(tmp = 0, i = 96; (i = bs.previousSetBit(i-1)) > 96 - 8 - 1;)
			tmp+=1L<<(i-(96-8));
		if (tmp != epcHeader)
			throw new RuntimeException("Invalid header"); //maybe the decoder could choose the structure from the header?

		for(tmp = 0, i = 96 - 8; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 1;)
			tmp+=1L<<(i-(96-8-3));
		int filter = (int) tmp;

		for(tmp = 0, i = 96 - 8 - 3; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - 1;)
			tmp+=1L<<(i-(96-8-3-3));
		byte partition = (byte) tmp;

		byte cpb = getCompanyPrefixBits(partition);
		for(tmp = 0, i = 96 - 8 - 3 - 3; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - 1;)
			tmp+=1L<<(i-(96-8-3-3-cpb));
		long companyPrefix = tmp;

		byte irb = getAssetTypeBits(partition);
		for(tmp = 0, i = 96 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - irb - 1;)
			tmp+=1L<<(i-(96-8-3-3-cpb-irb));
		int assetType = (int) tmp;

		//for the remainder, which is the serial, we can use fixed values
		for(tmp = 0, i = serialSize; (i = bs.previousSetBit(i-1)) > -1;)
			tmp+=1L<<i;
		long serial = tmp;

		Grai96 grai96 = new Grai96(filter,getCompanyPrefixDigits(partition),companyPrefix,assetType,serial);
		grai96.setEpc(epc);
		return grai96;
	}
}
