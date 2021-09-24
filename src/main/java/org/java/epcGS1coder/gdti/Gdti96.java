package org.java.epcGS1coder.gdti;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

/**
 * The Global Document Type Identifier EPC scheme is used to assign a unique identity to a specific
 * document, such as land registration papers, an insurance policy, and others.
 */

public final class Gdti96 extends Gdti{

    private final static byte epcHeader = 0b00101100;
    private final static byte serialSize = 41;
    private static final String uriHeader = "urn:epc:tag:gdti-96:";
    
    private String epc = null;
    
    private GdtiFilter filter;
    private byte partition;
    
    private long companyPrefix;
    private int documentType;
    private long serial; 
    
    private String uri = null;
    
    private Gdti96(int filter,
                   int companyPrefixDigits,
                   long companyPrefix,
                   int documentType,
                   long serial){
        this.filter = GdtiFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (documentType >= 1l<<getDocumentTypeBits(partition))
            throw new IllegalArgumentException("Document Type too large, max value (exclusive):" + (1l<<getDocumentTypeBits(partition)));
        this.documentType = documentType;
        if (serial >= 1l<<serialSize)
            throw new IllegalArgumentException("Serial too large, max value (exclusive):" + (1l<<serialSize));
        this.serial = serial;
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(96); // Gdti96 bits
            int i = 0;

            for (int j = 0; j < serialSize; j++,i++)
                epc.set(i, ((serial >> j) & 1)==1);

            for (int j = 0; j < getDocumentTypeBits(partition); j++,i++)
                epc.set(i, ((documentType >> j) & 1)==1);

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

    public int getDocumentType() {
        return documentType;
    }

    public long getSerial() {
        return serial;
    }

    public String getUri(){
        if (uri == null)
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getDocumentTypeDigits(partition)+"d",documentType)+"."+String.valueOf(serial);
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
        if (!(o instanceof Gdti96))
            return false;
        return ((Gdti96) o).getUri().equals(getUri());
    }

    public static Gdti96 fromFields(int filter,
                                    int companyPrefixDigits,
                                    long companyPrefix,
                                    int documentType,
                                    long serial){
        return new Gdti96(filter, companyPrefixDigits,companyPrefix,documentType,serial);
    }

    public static Gdti96 fromGs1Key(int filter,int companyPrefixDigits, String ai253) {
        if (ai253.length()<14 || !StringUtils.isNumeric(ai253))
            throw new IllegalArgumentException("GDTI with Serial must be at least 14 digits long");
        return new Gdti96(filter, companyPrefixDigits, Long.parseLong(ai253.substring(0, companyPrefixDigits)), Integer.parseInt(ai253.substring(companyPrefixDigits, 13 - 1)), Long.parseLong(ai253.substring(13)));
    }


    public static Gdti96 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        int documentType = Integer.parseInt(uriParts[2]);
        long serial = Long.parseLong(uriParts[3]);

        Gdti96 gdti96 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,documentType,serial);
        gdti96.setUri(uri);

        return gdti96;
    }

    public static Gdti96 fromEpc(String epc) {
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

        byte irb = getDocumentTypeBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - irb - 1;)
            tmp+=1L<<(i-(96-8-3-3-cpb-irb));
        int documentType = (int) tmp;

        //for the remainder, which is the serial, we can use fixed values
        for(tmp = 0, i = serialSize; (i = bs.previousSetBit(i-1)) > -1;)
            tmp+=1L<<i;
        long serial = tmp;

        try{
            Gdti96 gdti96 = new Gdti96(filter,getCompanyPrefixDigits(partition),companyPrefix,documentType,serial);
            gdti96.setEpc(epc);
            return gdti96;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }
}
