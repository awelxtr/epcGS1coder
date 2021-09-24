package org.java.epcGS1coder.sgtin;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * The Serialised Global Trade Item Number EPC scheme is used to assign a unique
 * identity to an instance of a trade item, such as a specific instance 
 * of a product or SKU.
 */

public final class Sgtin198 extends Sgtin {

    private final static byte epcHeader = 0b00110110;
    private final static int serialSize = 140;
    private final static int padding = 10;
    private final static byte serialMaxChars = 20;
    private final static String uriHeader = "urn:epc:tag:sgtin-198:";
    // Table A-1 specifies the valid characters in serials, this set is to make the validators more maintenable
    private final static HashSet<Character> invalidTableA1Chars = Arrays.asList(0x23,0x24,0x40,0x5B,0x5C,0x5D,0x5E,0x60).stream().map(Character.class::cast).collect(Collectors.toCollection(HashSet::new));
    
    private String epc;
    
    private byte partition;
    private SgtinFilter filter;
    private long companyPrefix;
    private int itemReference;
    private String serial;
    private String uri = null;

    private Sgtin198(int filter,
                     int companyPrefixDigits,
                     long companyPrefix,
                     int itemReference,
                     String serial){
        this.filter = SgtinFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (itemReference >= 1l<<getItemReferenceBits(partition))
            throw new IllegalArgumentException("Item Prefix too large, max value (exclusive):" + (1l<<getItemReferenceBits(partition)));
        this.itemReference = itemReference;
        if (serial.length() > serialMaxChars)
            throw new IllegalArgumentException("Serial must at most " + serialMaxChars + " alphanumeric characters long");
        for (char ch : serial.toCharArray())
            if (ch < 0x21 || ch > 0x7A || invalidTableA1Chars.contains(ch))
                throw new IllegalArgumentException("Invalid serial character");
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
            throw new IllegalArgumentException("GTIN must be 14 digits long");

        return new Sgtin198(filter, companyPrefixDigits, Long.parseLong(ai01.substring(1, companyPrefixDigits + 1)), Integer.parseInt(ai01.charAt(0) + ai01.substring(companyPrefixDigits + 1, 14 - 1)), ai21);
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(52*4); //Sgtin-198 epc is 52 hex chars long
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
            byte[] epcba = epc.toByteArray();
            StringBuffer sb = new StringBuffer(52);
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

    @Override
    public String toString(){
        return getUri();
    }

    private void setEpc(String epc){ this.epc = epc; }
    private void setUri(String uri){ this.uri = uri; }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Sgtin198))
            return false;
        return ((Sgtin198) o).getUri().equals(getUri());
    }

    /**
     * Table A-1 for the encoding
     */
    private String getUriSerialChar(char ch){
        if (ch < 0x21 || ch > 0x7A || invalidTableA1Chars.contains(ch))
            throw new IllegalArgumentException("Wrong char");
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
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

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
            throw new IllegalArgumentException("Invalid header"); //maybe the decoder could choose the structure from the header?

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

        i =208-58; //buffer size - epcheader.size - filter.size - partition.size - getCompanyPrefixBits(partition) - getItemReferenceBits(partition)
        for(;(tmpba = bs.get(i-7,i).toByteArray()).length!=0;i-=7)
            serialBuilder.append(new String(tmpba));

        String serial = serialBuilder.toString();
        try{
            Sgtin198 sgtin198 = new Sgtin198(filter,getCompanyPrefixDigits(partition),companyPrefix,itemReference,serial);
            sgtin198.setEpc(epc);
            return sgtin198;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }

}
