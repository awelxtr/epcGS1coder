package org.java.epcGS1coder.grai;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * The Global Returnable Asset Identifier EPC scheme is used to assign a unique identity to a specific
 * returnable asset, such as a reusable shipping container or a pallet skid.
 */

public final class Grai170 extends Grai{
    private final static byte epcHeader = 0b00110111;
    private final static int serialSize = 112;
    private final static byte serialMaxChars = 16;
    private final static int padding = 6;
    private final static String uriHeader = "urn:epc:tag:grai-170:";
    // Table A-1 specifies the valid characters in serials, this set is to make the validators more maintenable
    private final static HashSet<Character> invalidTableA1Chars = Arrays.asList(0x23,0x24,0x40,0x5B,0x5C,0x5D,0x5E,0x60).stream().map(Character.class::cast).collect(Collectors.toCollection(HashSet::new));
    
    private String epc;
    
    private byte partition;
    private GraiFilter filter;
    private long companyPrefix;
    private int assetType;
    private String serial;
    private String uri = null;

    private Grai170(int filter,
                    int companyPrefixDigits,
                    long companyPrefix,
                    int assetType,
                    String serial){
        this.filter = GraiFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (assetType >= 1l<<getAssetTypeBits(partition))
            throw new IllegalArgumentException("Asset Type too large, max value (exclusive):" + (1l<<getAssetTypeBits(partition)));
        this.assetType = assetType;
        if (serial.length() > serialMaxChars)
            throw new IllegalArgumentException("Serial must at most " + serialMaxChars + " alphanumeric characters long");
        for (char ch : serial.toCharArray())
            if (ch < 0x21 || ch > 0x7A || invalidTableA1Chars.contains(ch))
                throw new IllegalArgumentException("Invalid serial character");
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
        if (ai8003.length()!=14 || !StringUtils.isNumeric(ai8003.substring(0, 13)))
            throw new IllegalArgumentException("GRAI (must be numeric) with serial must be 14 digits long");

        return new Grai170(filter, companyPrefixDigits, Long.parseLong(ai8003.substring(0, companyPrefixDigits)), Integer.parseInt(ai8003.substring(companyPrefixDigits, 13 - 1)), ai8003.substring(13));
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(44*4); //Grai-170 epc is 44 hex chars long
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

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Grai170))
            return false;
        return ((Grai170) o).getUri().equals(getUri());
    }

    @Override
    public String toString(){
        return getUri();
    }

    private void setEpc(String epc){ this.epc = epc; }
    private void setUri(String uri){ this.uri = uri; }

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

    public static Grai170 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

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
            throw new IllegalArgumentException("Invalid header"); //maybe the decoder could choose the structure from the header?

        for(tmp = 0, i = 176 - 8; (i = bs.previousSetBit(i-1)) > 176 - 8 - 3 - 1;)
            tmp+=1L<<(i-(176-8-3));
        int filter = (int) tmp;

        for(tmp = 0, i = 176 - 8 - 3; (i = bs.previousSetBit(i-1)) > 176 - 8 - 3 - 3 - 1;)
            tmp+=1L<<(i-(176-8-3-3));
        byte partition = (byte) tmp;

        final byte cpb = getCompanyPrefixBits(partition);
        for(tmp = 0, i = 176 - 8 - 3 - 3; (i = bs.previousSetBit(i-1)) > 176 - 8 - 3 - 3 - cpb - 1;)
            tmp+=1L<<(i-(176-8-3-3-cpb));
        long companyPrefix = tmp;

        final byte atb = getAssetTypeBits(partition);
        for(tmp = 0, i = 176 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 176 - 8 - 3 - 3 - cpb - atb - 1;)
            tmp+=1L<<(i-(176-8-3-3-cpb-atb));
        int assetType = (int) tmp;

        StringBuilder serialBuilder = new StringBuilder("");
        byte[] tmpba;

        i =176-8-3-3-cpb-atb; //buffer size - epcheader.size - filter.size - partition.size - getCompanyPrefixBits(partition) - getAssetTypeBits(partition)
        for(;(tmpba = bs.get(i-7,i).toByteArray()).length!=0;i-=7)
            serialBuilder.append(new String(tmpba));

        String serial = serialBuilder.toString();

        try{
            Grai170 grai170 = new Grai170(filter,getCompanyPrefixDigits(partition),companyPrefix,assetType,serial);
            grai170.setEpc(epc);
            return grai170;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }
}
