package org.java.epcGS1coder.itip;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * The Individual Trade Item Piece EPC scheme is used to assign a unique identity to a subordinate
 * element of a trade item (e.g., left and right shoes, suit trousers and jacket, DIY trade item consisting
 * of several physical units), the latter of which comprises multiple pieces.
 */

public final class Itip212 extends Itip{
    private final static byte epcHeader = 0b01000001;
    private final static int serialSize = 140;
    private final static byte serialMaxChars = 20;
    private final static int padding = 12;
    private static final String uriHeader = "urn:epc:tag:itip-212:";
    // Table A-1 specifies the valid characters in serials, this set is to make the validators more maintenable
    private final static HashSet<Character> invalidTableA1Chars = Arrays.asList(0x23,0x24,0x40,0x5B,0x5C,0x5D,0x5E,0x60).stream().map(Character.class::cast).collect(Collectors.toCollection(HashSet::new));
    
    private String epc = null;
    
    private ItipFilter filter;
    private byte partition;
    
    private long companyPrefix;
    private int indicatorPadDigitItemReference;
    private byte piece;
    private byte total;
    private String serial; 
    
    private String uri = null;
    
    private Itip212(int filter,
                    int companyPrefixDigits,
                    long companyPrefix,
                    int indicatorPadDigitItemReference,
                    byte piece,
                    byte total,
                    String serial){
        this.filter = ItipFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (indicatorPadDigitItemReference >= 1l<<getIndicatorPadDigitItemReferenceBits(partition))
            throw new IllegalArgumentException("Indicator/Pad Digit and Item Reference too large, max value (exclusive):" + (1l<<getIndicatorPadDigitItemReferenceBits(partition)));
        this.indicatorPadDigitItemReference = indicatorPadDigitItemReference;
        if (piece < 0 || piece > 99)
            throw new IllegalArgumentException("Invalid piece, must be between 0 and 99");
        this.piece = piece;
        if (total < 0 || total > 99)
            throw new IllegalArgumentException("Invalid total, must be between 0 and 99");
        this.total = total;
        if (serial.length() > serialMaxChars)
            throw new IllegalArgumentException("Serial must at most " + serialMaxChars + " alphanumeric characters long");
        for (char ch : serial.toCharArray())
            if (ch < 0x21 || ch > 0x7A || invalidTableA1Chars.contains(ch))
                throw new IllegalArgumentException("Invalid serial character");
        this.serial = serial;
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(56*4); 
            int i = serialSize+padding-1;

            for (byte b : serial.getBytes()) {
                for (int j = 6; j >= 0; j--,i--)
                    epc.set(i, ((b >> j) & 1) == 1);
            }

            i = serialSize+padding;
            for (int j = 0; j < 7; j++,i++)
                epc.set(i, ((total >> j) & 1)==1);

            for (int j = 0; j < 7; j++,i++)
                epc.set(i, ((piece >> j) & 1)==1);

            for (int j = 0; j < getIndicatorPadDigitItemReferenceBits(partition); j++,i++)
                epc.set(i, ((indicatorPadDigitItemReference >> j) & 1)==1);

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

    public int getIndicatorPadDigitItemReference() {
        return indicatorPadDigitItemReference;
    }

    public byte getPiece(){
        return piece;
    }

    public byte getTotal(){
        return total;
    }

    public String getSerial() {
        return serial;
    }

    public String getUri(){
        if (uri == null)
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getIndicatorPadDigitItemReferenceDigits(partition)+"d",indicatorPadDigitItemReference)+"."+String.format("%02d",piece)+"."+String.format("%02d",total)+"."+serial.chars().mapToObj(c -> getUriSerialChar((char) c)).collect(Collectors.joining());
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
        if (!(o instanceof Itip212))
            return false;
        return ((Itip212) o).getUri().equals(getUri());
    }

    public static Itip212 fromFields(int filter,
                                    int companyPrefixDigits,
                                    long companyPrefix,
                                    int itemReference,
                                    byte piece,
                                    byte total,
                                    String serial){
        return new Itip212(filter, companyPrefixDigits,companyPrefix,itemReference,piece,total,serial);
    }

    public static Itip212 fromGs1Key(int filter,int companyPrefixDigits, String ai8006, String ai21) {
        if (ai8006.length()!= 18 || !StringUtils.isNumeric(ai8006))
            throw new IllegalArgumentException("ITIP must be 18 digits long");
        return new Itip212(filter, companyPrefixDigits, Long.parseLong(ai8006.substring(1, companyPrefixDigits + 1)), Integer.parseInt(ai8006.substring(companyPrefixDigits + 1, 14 - 1)), Byte.parseByte(ai8006.substring(14+1, 14+1+2-1)), Byte.parseByte(ai8006.substring(14+1+2, 14+1+2+2-1)), ai21);
    }


    public static Itip212 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        int indicatorPadDigitItemReference = Integer.parseInt(uriParts[2]);
        byte piece = Byte.parseByte(uriParts[3]);
        byte total = Byte.parseByte(uriParts[4]);
        String serial = uriParts[5];

        Itip212 itip212 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,indicatorPadDigitItemReference,piece,total,serial);
        itip212.setUri(uri);

        return itip212;
    }

    public static Itip212 fromEpc(String epc) {
        ArrayList<String> a = new ArrayList<String>();
        for (int i = 0; i<epc.length(); i+=2) {
            a.add(epc.substring(i, i+2));
        }

        ByteBuffer bb = ByteBuffer.allocate(28);
        for (int i = a.size() - 1; i>=0;i--)
            bb.put((byte) Integer.parseInt(a.get(i),16));
        bb.rewind();

        BitSet bs = BitSet.valueOf(bb);

        int i;
        long tmp;

        for(tmp = 0, i = 224; (i = bs.previousSetBit(i-1)) > 224 - 8 - 1;)
            tmp+=1L<<(i-(224-8));
        if (tmp != epcHeader)
            throw new IllegalArgumentException("Invalid header"); //maybe the decoder could choose the structure from the header?

        for(tmp = 0, i = 224 - 8; (i = bs.previousSetBit(i-1)) > 224 - 8 - 3 - 1;)
            tmp+=1L<<(i-(224-8-3));
        int filter = (int) tmp;

        for(tmp = 0, i = 224 - 8 - 3; (i = bs.previousSetBit(i-1)) > 224 - 8 - 3 - 3 - 1;)
            tmp+=1L<<(i-(224-8-3-3));
        byte partition = (byte) tmp;

        byte cpb = getCompanyPrefixBits(partition);
        for(tmp = 0, i = 224 - 8 - 3 - 3; (i = bs.previousSetBit(i-1)) > 224 - 8 - 3 - 3 - cpb - 1;)
            tmp+=1L<<(i-(224-8-3-3-cpb));
        long companyPrefix = tmp;

        byte ipdirb = getIndicatorPadDigitItemReferenceBits(partition);
        for(tmp = 0, i = 224 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 224 - 8 - 3 - 3 - cpb - ipdirb - 1;)
            tmp+=1L<<(i-(224-8-3-3-cpb-ipdirb));
        int indicatorPadDigitItemReference = (int) tmp;

        for(tmp = 0, i = 224 - 8 - 3 - 3 - cpb - ipdirb; (i = bs.previousSetBit(i-1)) > 224 - 8 - 3 - 3 - cpb - ipdirb - 7 - 1;)
            tmp+=1L<<(i-(224-8-3-3-cpb-ipdirb-7));
        byte piece = (byte) tmp;

        for(tmp = 0, i = 224 - 8 - 3 - 3 - cpb - ipdirb - 7; (i = bs.previousSetBit(i-1)) > 224 - 8 - 3 - 3 - cpb - ipdirb - 7 - 7 - 1;)
            tmp+=1L<<(i-(224-8-3-3-cpb-ipdirb-7-7));
        byte total = (byte) tmp;

        StringBuilder serialBuilder = new StringBuilder("");
        byte[] tmpba;

        i =224-8-3-3-44-7-7; //buffer size - epcheader.size - filter.size - partition.size - getCompanyPrefixBits(partition) - getIndicatorPadDigitItemReferenceBits(partition) - piece - total
        for(int j = 0;j < serialMaxChars && (tmpba = bs.get(i-7,i).toByteArray()).length!=0;i-=7,j++)
            serialBuilder.append(new String(tmpba));

        String serial = serialBuilder.toString();

        try{
            Itip212 itip212 = new Itip212(filter,getCompanyPrefixDigits(partition),companyPrefix,indicatorPadDigitItemReference,piece,total,serial);
            itip212.setEpc(epc);
            return itip212;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
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
}
