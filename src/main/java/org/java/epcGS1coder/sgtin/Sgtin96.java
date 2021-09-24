package org.java.epcGS1coder.sgtin;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

/**
 * The Serialised Global Trade Item Number EPC scheme is used to assign a unique
 * identity to an instance of a trade item, such as a specific instance 
 * of a product or SKU.
 */

public final class Sgtin96 extends Sgtin{

    private final static byte epcHeader = 0b00110000;
    private final static byte serialBitSize = 38;
    private static final String uriHeader = "urn:epc:tag:sgtin-96:";
    
    private String epc = null;
    
    private SgtinFilter filter;
    private byte partition;
    
    private long companyPrefix;
    private int itemReference;
    private long serial; 
    
    private String uri = null;
    
    private Sgtin96(int filter,
                    int companyPrefixDigits,
                    long companyPrefix,
                    int itemReference,
                    long serial){
        this.filter = SgtinFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (itemReference >= 1l<<getItemReferenceBits(partition))
            throw new IllegalArgumentException("Item Prefix too large, max value (exclusive):" + (1l<<getItemReferenceBits(partition)));
        this.itemReference = itemReference;
        if (serial >= 1l<<serialBitSize)
            throw new IllegalArgumentException("Serial too big, max value: " + ((1l<<serialBitSize)-1));
        this.serial = serial;
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(96); 
            int i = 0;

            for (int j = 0; j < serialBitSize; j++,i++)
                epc.set(i, ((serial >> j) & 1)==1);

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

            byte[] epcba = epc.toByteArray();
            StringBuffer sb = new StringBuffer(epcba.length*2);
            for (i = epcba.length-1; i>=0; i--)
                sb.append(String.format("%02X",epcba[i]));

            this.epc = sb.toString();
        }
        return epc;
    }

    private void setEpc(String epc) {
        this.epc = epc;
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

    public long getSerial() {
        return serial;
    }

    public String getUri(){
        if (uri == null)
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getItemReferenceDigits(partition)+"d",itemReference)+"."+String.valueOf(serial);
        return uri;
    }
    private void setUri(String uri){
        this.uri = uri;
    };

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Sgtin96))
            return false;
        return ((Sgtin96) o).getUri().equals(getUri());
    }

    public static Sgtin96 fromFields(int filter,
                                    int companyPrefixDigits,
                                    long companyPrefix,
                                    int itemReference,
                                    long serial){
        return new Sgtin96(filter, companyPrefixDigits,companyPrefix,itemReference,serial);
    }

    public static Sgtin96 fromGs1Key(int filter,int companyPrefixDigits, String ai01, long ai21) {
        if (ai01.length()!=14 || !StringUtils.isNumeric(ai01))
            throw new IllegalArgumentException("GTIN must be 14 digits long");
        return new Sgtin96(filter, companyPrefixDigits, Long.parseLong(ai01.substring(1, companyPrefixDigits + 1)), Integer.parseInt(ai01.substring(companyPrefixDigits + 1, 14 - 1)), ai21);
    }


    public static Sgtin96 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        int itemReference = Integer.parseInt(uriParts[2]);
        long serial = Long.parseLong(uriParts[3]);

        Sgtin96 sgtin96 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,itemReference,serial);
        sgtin96.setUri(uri);

        return sgtin96;
    }

    public static Sgtin96 fromEpc(String epc) {
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
            throw new IllegalArgumentException("Invalid header"); //maybe the decoder could choose the structure from the header?

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

        byte irb = getItemReferenceBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - irb - 1;)
            tmp+=1L<<(i-(96-8-3-3-cpb-irb));
        int itemReference = (int) tmp;

        //for the remainder, which is the serial, we can use fixed values
        for(tmp = 0, i = serialBitSize; (i = bs.previousSetBit(i-1)) > -1;)
            tmp+=1L<<i;
        long serial = tmp;
        try {
            Sgtin96 sgtin96 = new Sgtin96(filter,getCompanyPrefixDigits(partition),companyPrefix,itemReference,serial);
            sgtin96.setEpc(epc);
            return sgtin96;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }
}
