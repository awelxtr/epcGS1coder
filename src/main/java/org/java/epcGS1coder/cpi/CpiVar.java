package org.java.epcGS1coder.cpi;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class CpiVar extends Cpi{
    
    private final static byte epcHeader = 0b00111101;
	private final static int serialSize = 40;
	private final static int padding = 10;
	private final static long maxSerialValue = 999999999999l;
	private final static String uriHeader = "urn:epc:tag:cpi-var:";
	
	private BitSet epc;
	
	private byte partition;
	private CpiFilter filter;
	private long companyPrefix;
	private String componentPartReference;
	private long serial;
	private String uri = null;

	CpiVar(int filter,
           int companyPrefixDigits,
           long companyPrefix,
           String componentPartReference,
           long serial){
		this.filter = CpiFilter.values()[filter];
		this.partition = (byte) getPartition(companyPrefixDigits);
		this.companyPrefix = companyPrefix;
        int maxDigits = getComponentPartReferenceMaximumDigits(partition);
        if (componentPartReference.length() > maxDigits)
            throw new RuntimeException("Company/Part Reference must at the very most "+maxDigits+" digits characters long");
        this.componentPartReference = componentPartReference;
		if (serial > maxSerialValue)
			throw new RuntimeException("Serial max value is " + maxSerialValue);
		this.serial = serial;
	}

	public static CpiVar fromFields(int filter,
									int companyPrefixDigits,
									long companyPrefix,
									String componentPartReference,
									long serial){
		return new CpiVar(filter,companyPrefixDigits,companyPrefix,componentPartReference,serial);
	}

	public static CpiVar fromGs1Key(int filter,int companyPrefixDigits, String ai8010, long ai8011) {
		if (ai8010.length()<6 || ai8010.length()>30)
			throw new RuntimeException("CPI must be between 6 and 30 alphanumeric characters long");

		return new CpiVar(filter, companyPrefixDigits, Long.parseLong(ai8010.substring(0, companyPrefixDigits)), ai8010.substring(companyPrefixDigits), ai8011);
	}

	public String getEpc() {
		// how many bytes takes to fit all the data.
		int bsSize = (int) Math.ceil((double) (8+3+3+getCompanyPrefixBits(partition) + (componentPartReference.length()+1) * 6 + 40)/4)*4;
		if (epc == null){
            epc = new BitSet(bsSize+bsSize%8);
            
            for (int j = 0, i=bsSize - 8; j < 8; j++,i++)
				epc.set(i, ((epcHeader >> j) & 1)==1);

			for (int j = 0, i=bsSize - (8+3); j < 3; j++,i++)
				epc.set(i, ((filter.getValue() >> j) & 1)==1);

            for (int j = 0, i=bsSize - (8+3+3); j < 3; j++,i++)
				epc.set(i, ((partition >> j) & 1)==1);
            
            for (int j = 0, i=bsSize - (8+3+3+getCompanyPrefixBits(partition)); j < getCompanyPrefixBits(partition); j++,i++)
				epc.set(i, ((companyPrefix >> j) & 1)==1);
                
                            
            int k = bsSize - (8+3+3+getCompanyPrefixBits(partition)+1);
			for (byte b : componentPartReference.getBytes()) {
				for (int j = 5; j >= 0; j--,k--)
					epc.set(k, ((b >> j) & 1) == 1);
			}
            //In the epc the companyPartReference must be followed by 0b000000;
            byte b = 0b000000;
            for (int j = 5; j >= 0; j--,k--)
				epc.set(k, ((b >> j) & 1) == 1);
            
			for (int j = 0, i = k - 40 + 1 ; j < serialSize; j++,i++) 
			 	epc.set(i, ((serial >> j) & 1)==1);
		}

		byte[] epcba = epc.toByteArray();

		StringBuffer sb = new StringBuffer();
		for (int i = epcba.length-1; i>=0; i--)
			sb.append(String.format("%02X",epcba[i]));
		while(sb.length() < sb.capacity())
			sb.append("0");

		return sb.toString();//.substring(0,bsSize/4);
	}

	public int getFilter() {
		return filter.getValue();
	}

	public long getCompanyPrefix() {
		return companyPrefix;
	}

	public String getComponentPartReference() {
		return componentPartReference;
	}

	public long getSerial() {
		return serial;
	}

	public String getUri() {
		if (uri == null)
			uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+componentPartReference.chars().mapToObj(c -> getUriCompanyPartReferenceChar((char) c)).collect(Collectors.joining())+"."+serial;
		return uri;
	}

	void setEpc(BitSet epc){ this.epc = epc; }
	void setUri(String uri){ this.uri = uri; }

    public static CpiVar fromUri(String uri) {
		if (!uri.startsWith(uriHeader))
			throw new RuntimeException("Decoding error: wrong URI header, expected " + uriHeader);

		String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
		int filter = Integer.parseInt(uriParts[0]);
		byte partition = (byte) getPartition(uriParts[1].length());
		long companyPrefix = Long.parseLong(uriParts[1]);
		String companyPartReference = uriParts[2];
		long serial = Long.parseLong(uriParts[3]);

		CpiVar cpiVar = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,companyPartReference,serial);
		cpiVar.setUri(uri);
		return cpiVar;
	}

	public static CpiVar fromEpc(String epc) {
		ArrayList<String> a = new ArrayList<String>();
		if (epc.length() % 2 != 0)
			epc+="0";
		for (int i = 0; i<epc.length(); i+=2) {
			a.add(epc.substring(i, i+2));
		}

		ByteBuffer bb = ByteBuffer.allocate(epc.length() * 4);
		for (int i = a.size() - 1; i>=0;i--)
			bb.put((byte) Integer.parseInt(a.get(i),16));
		bb.rewind();

		BitSet bs = BitSet.valueOf(bb);

		int i;
		int epcBits = epc.length()*4;
		long tmp;

		for(tmp = 0, i = epcBits; (i = bs.previousSetBit(i-1)) > epcBits - 8 - 1;)
			tmp+=1L<<(i-(epcBits-8));
		if (tmp != epcHeader)
			throw new RuntimeException("Invalid header"); //maybe the decoder could choose the structure from the header?

		for(tmp = 0, i = epcBits - 8; (i = bs.previousSetBit(i-1)) > epcBits - 8 - 3 - 1;)
			tmp+=1L<<(i-(epcBits-8-3));
		int filter = (int) tmp;

		for(tmp = 0, i = epcBits - 8 - 3; (i = bs.previousSetBit(i-1)) > epcBits - 8 - 3 - 3 - 1;)
			tmp+=1L<<(i-(epcBits-8-3-3));
		byte partition = (byte) tmp;

		byte cpb = getCompanyPrefixBits(partition);
		for(tmp = 0, i = epcBits - 8 - 3 - 3; (i = bs.previousSetBit(i-1)) > epcBits - 8 - 3 - 3 - cpb - 1;)
			tmp+=1L<<(i-(epcBits-8-3-3-cpb));
		long companyPrefix = tmp;

		byte[] tmpba;
		StringBuilder companyPartReferenceBuilder = new StringBuilder("");
		i++;
		for(int j = 0;j < getComponentPartReferenceMaximumDigits(partition) && (tmpba = bs.get(i-6,i).toByteArray()).length!=0;i-=6,j++){
			if (tmpba[0]>=0b000001 && tmpba[0] <= 0b011010) // Encoded [A-Z] => Table G-1 Characters Permitted in 6-bit Alphanumeric Fields
				tmpba[0]|=0b01000000;	
			companyPartReferenceBuilder.append(new String(tmpba));		
		}
		i-=6;
		String companyPartReference = companyPartReferenceBuilder.toString();

		long offset = i - serialSize;
		for(tmp = 0, i = serialSize; (i = bs.previousSetBit(i-1)) > -1;)
			tmp+=1L<<(i - offset);
		long serial = tmp;

		CpiVar cpiVar = new CpiVar(filter,getCompanyPrefixDigits(partition),companyPrefix,companyPartReference,serial);
		cpiVar.setEpc(bs);
		return cpiVar;
	}

	/**
	 * Table A-1 for the encoding
	 */
	private String getUriCompanyPartReferenceChar(char ch){
		if (!((ch>='0' && ch<='9') || (ch>='A' && ch<='Z') || ch=='#' || ch=='-' || ch=='/')) 
			throw new RuntimeException("Wrong char");
		switch (ch){
			case '#':
			case '/':
				return "%"+String.format("%02x",(int) ch).toUpperCase();
			default:
				return String.valueOf(ch);
		}
	}

    /**
     * Table 14-25 CPI-var Partition Table
     * @param partition
     * @return M value
     */
    protected static byte getCompanyPrefixBits(int partition){
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
     * Table 14-25 CPI-var Partition Table
     * @param partition
     * @return N value
     */
    protected static short getComponentPartReferenceMaximumBits(int partition){
        switch (partition){
            case 0:
                return 114;
            case 1:
                return 120;
            case 2:
                return 126;
            case 3:
                return 132;
            case 4:
                return 138;
            case 5:
                return 144;
            case 6:
                return 150;
            default:
                throw new RuntimeException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-25 CPI-var Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    protected static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-25 CPI-var Partition Table
     * @param P
     * @return L
     */
    protected static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    /**
     * Table 14-25 CPI-var Partition Table
     * @param P
     */
    protected static int getComponentPartReferenceMaximumDigits(int partition){
        return partition+18;
    }
}
