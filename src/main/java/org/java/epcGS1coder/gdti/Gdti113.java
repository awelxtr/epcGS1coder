package org.java.epcGS1coder.gdti;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

/**
 * The Global Document Type Identifier EPC scheme is used to assign a unique identity to a specific
 * document, such as land registration papers, an insurance policy, and others.
 */

public final class Gdti113 extends Gdti {
    private final static byte epcHeader = 0b00111010;
    private final static int serialSize = 58;
    private final static int serialMaxChars = 17;
    private final static int padding = (32*4)-113; // GDTI-113 epc is 32 hex chars long
    private final static String uriHeader = "urn:epc:tag:gdti-113:";
    
    private String epc;
    
    private byte partition;
    private GdtiFilter filter;
    private long companyPrefix;
    private int documentType;
    private String serial;
    private String uri = null;

    private Gdti113(int filter,
                    int companyPrefixDigits,
                    long companyPrefix,
                    int documentType,
                    String serial){
        this.filter = GdtiFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (documentType >= 1l<<getDocumentTypeBits(partition))
            throw new IllegalArgumentException("Document Type too large, max value (exclusive):" + (1l<<getDocumentTypeBits(partition)));
        this.documentType = documentType;
        if (serial.length() > serialMaxChars || !StringUtils.isNumeric(serial))
            throw new IllegalArgumentException("Serial must be at most 17 numeric characters");
        this.serial = serial;
    }

    public static Gdti113 fromFields(int filter,
                                      int companyPrefixDigits,
                                      long companyPrefix,
                                      int documentType,
                                      String serial){
        return new Gdti113(filter, companyPrefixDigits, companyPrefix, documentType, serial);
    }

    public static Gdti113 fromGs1Key(int filter,int companyPrefixDigits, String ai253) {
        if (ai253.length()<14 || !StringUtils.isNumeric(ai253))
            throw new IllegalArgumentException("GDTI with Serial must be at least 14 digits long");

        return new Gdti113(filter, companyPrefixDigits, Long.parseLong(ai253.substring(0, companyPrefixDigits)), Integer.parseInt(ai253.substring(companyPrefixDigits, 13 - 1)), ai253.substring(13));
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(32*4); // GDTI-113 epc is 32 hex chars long
            int i = padding;

            long serial = Long.parseLong("1"+this.serial); // Numeric string encoding prepends a "1" at the beginning of the encoded serial

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
            StringBuffer sb = new StringBuffer(44);
            for (i = epcba.length-1; i>=0; i--)
                sb.append(String.format("%02X",epcba[i]));
            this.epc = sb.toString();
        }
        
        return epc;
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

    public String getSerial() {
        return serial;
    }

    public String getUri() {
        if (uri == null)
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getDocumentTypeDigits(partition)+"d",documentType)+"."+serial;
        return uri;
    }

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Gdti113))
            return false;
        return ((Gdti113) o).getUri().equals(getUri());
    }

    private void setEpc(String epc){ this.epc = epc; }
    private void setUri(String uri){ this.uri = uri; }

    public static Gdti113 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        int documentType = Integer.parseInt(uriParts[2]);

        String serial = uriParts[3];
        StringBuilder sb = new StringBuilder();
        String[] serialSplit = serial.split("%");
        sb.append(serialSplit[0]);
        for (int i = 1; i < serialSplit.length; i++){
            sb.append((char) Integer.parseInt(serialSplit[i].substring(0,2),16));
            sb.append(serialSplit[i].substring(2));
        }

        Gdti113 gdti113 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,documentType,sb.toString());
        gdti113.setUri(uri);
        return gdti113;
    }

    public static Gdti113 fromEpc(String epc) {
        ArrayList<String> a = new ArrayList<String>();
        for (int i = 0; i<epc.length(); i+=2) {
            a.add(epc.substring(i, i+2));
        }

        ByteBuffer bb = ByteBuffer.allocate(16);
        for (int i = a.size() - 1; i>=0;i--)
            bb.put((byte) Integer.parseInt(a.get(i),16));
        bb.rewind();

        BitSet bs = BitSet.valueOf(bb);

        int i;
        long tmp;

        for(tmp = 0, i = 128; (i = bs.previousSetBit(i-1)) > 128 - 8 - 1;)
            tmp+=1L<<(i-(128-8));
        if (tmp != epcHeader)
            throw new IllegalArgumentException("Invalid header"); //maybe the decoder could choose the structure from the header?

        for(tmp = 0, i = 128 - 8; (i = bs.previousSetBit(i-1)) > 128 - 8 - 3 - 1;)
            tmp+=1L<<(i-(128-8-3));
        int filter = (int) tmp;

        for(tmp = 0, i = 128 - 8 - 3; (i = bs.previousSetBit(i-1)) > 128 - 8 - 3 - 3 - 1;)
            tmp+=1L<<(i-(128-8-3-3));
        byte partition = (byte) tmp;

        byte cpb = getCompanyPrefixBits(partition);
        for(tmp = 0, i = 128 - 8 - 3 - 3; (i = bs.previousSetBit(i-1)) > 128 - 8 - 3 - 3 - cpb - 1;)
            tmp+=1L<<(i-(128-8-3-3-cpb));
        long companyPrefix = tmp;

        byte dtb = getDocumentTypeBits(partition);
        for(tmp = 0, i = 128 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 128 - 8 - 3 - 3 - cpb - dtb - 1;)
            tmp+=1L<<(i-(128-8-3-3-cpb-dtb));
        int documentType = (int) tmp;

        //for the remainder, which is the serial, we can use fixed values
        for(tmp = 0, i = serialSize + padding; (i = bs.previousSetBit(i-1)) > -1;)
            tmp+=1L<<(i-padding);
        String serial = String.valueOf(tmp).substring(1); // Numeric string encoding prepends a "1" at the beginning of the encoded serial

        try{
            Gdti113 gdti113 = new Gdti113(filter,getCompanyPrefixDigits(partition),companyPrefix,documentType,serial);
            gdti113.setEpc(epc);
            return gdti113;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }
}
