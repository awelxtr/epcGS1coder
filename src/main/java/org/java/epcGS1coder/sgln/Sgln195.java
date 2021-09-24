package org.java.epcGS1coder.sgln;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * The SGLN EPC scheme is used to assign a unique identity to a physical location, such as a specific
 * building or a specific unit of shelving within a warehouse.
 */

public final class Sgln195 extends Sgln {

    private final static byte epcHeader = 0b00111001;
    private final static int extensionSize = 140;
    private final static int padding = 13;
    private final static byte extensionMaxChars = 20;
    private final static String uriHeader = "urn:epc:tag:sgln-195:";
    // Table A-1 specifies the valid characters in serials, this set is to make the validators more maintenable
    private final static HashSet<Character> invalidTableA1Chars = Arrays.asList(0x23,0x24,0x40,0x5B,0x5C,0x5D,0x5E,0x60).stream().map(Character.class::cast).collect(Collectors.toCollection(HashSet::new));
    
    private String epc;
    
    private byte partition;
    private SglnFilter filter;
    private long companyPrefix;
    private int locationReference;
    private String extension;
    private String uri = null;

    private Sgln195(int filter,
                    int companyPrefixDigits,
                    long companyPrefix,
                    int locationReference,
                    String extension){
        this.filter = SglnFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (locationReference >= 1l<<getLocationReferenceBits(partition))
            throw new IllegalArgumentException("Location Reference too large, max value (exclusive):" + (1l<<getLocationReferenceBits(partition)));
        this.locationReference = locationReference;
        if (extension.length() > extensionMaxChars)
            throw new IllegalArgumentException("Extension must at most " + extensionMaxChars + " alphanumeric characters long");
        for (char ch : extension.toCharArray())
            if (ch < 0x21 || ch > 0x7A || invalidTableA1Chars.contains(ch))
                throw new IllegalArgumentException("Invalid extension character");
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
        if (ai414.length()!=13 || !StringUtils.isNumeric(ai414))
            throw new IllegalArgumentException("GLN must be 13 digits long");

        return new Sgln195(filter, companyPrefixDigits, Long.parseLong(ai414.substring(0, companyPrefixDigits)), Integer.parseInt(ai414.substring(companyPrefixDigits, 13-1)), ai254);
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(52*4); //Sgln-195 epc is 52 hex chars long
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
    
    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Sgln195))
            return false;
        return ((Sgln195) o).getUri().equals(getUri());
    }
    
    private void setEpc(String epc){ this.epc = epc; }
    private void setUri(String uri){ this.uri = uri; }

    /**
     * Table A-1 for the encoding
     */
    private String getUriExtensionChar(char ch){
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

    public static Sgln195 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

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

        try{
            Sgln195 sgln195 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,locationReference,sb.toString());
            sgln195.setUri(uri);
            return sgln195;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
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
            throw new IllegalArgumentException("Invalid header"); //maybe the decoder could choose the structure from the header?

        for(tmp = 0, i = 208 - 8; (i = bs.previousSetBit(i-1)) > 208 - 8 - 3 - 1;)
            tmp+=1L<<(i-(208-8-3));
        int filter = (int) tmp;

        for(tmp = 0, i = 208 - 8 - 3; (i = bs.previousSetBit(i-1)) > 208 - 8 - 3 - 3 - 1;)
            tmp+=1L<<(i-(208-8-3-3));
        byte partition = (byte) tmp;

        final byte cpb = getCompanyPrefixBits(partition);
        for(tmp = 0, i = 208 - 8 - 3 - 3; (i = bs.previousSetBit(i-1)) > 208 - 8 - 3 - 3 - cpb - 1;)
            tmp+=1L<<(i-(208-8-3-3-cpb));
        long companyPrefix = tmp;

        final byte lrb = getLocationReferenceBits(partition);
        for(tmp = 0, i = 208 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 208 - 8 - 3 - 3 - cpb - lrb - 1;)
            tmp+=1L<<(i-(208-8-3-3-cpb-lrb));
        int locationReference = (int) tmp;

        StringBuilder extensionBuilder = new StringBuilder("");
        byte[] tmpba;

        i =208-8-3-3-cpb-lrb; //buffer size - epcheader.size - filter.size - partition.size - getCompanyPrefixBits(partition) - getLocationReferenceBits(partition)
        for(;(tmpba = bs.get(i-7,i).toByteArray()).length!=0;i-=7)
            extensionBuilder.append(new String(tmpba));

        String extension = extensionBuilder.toString();

        Sgln195 sgln195 = new Sgln195(filter,getCompanyPrefixDigits(partition),companyPrefix,locationReference,extension);
        sgln195.setEpc(epc);
        return sgln195;
    }

}
