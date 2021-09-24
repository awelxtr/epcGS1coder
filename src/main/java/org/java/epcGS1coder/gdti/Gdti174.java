package org.java.epcGS1coder.gdti;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * The Global Document Type Identifier EPC scheme is used to assign a unique identity to a specific
 * document, such as land registration papers, an insurance policy, and others.
 */

public final class Gdti174 extends Gdti {
    private final static byte epcHeader = 0b00111110;
    private final static int serialSize = 119;
    private final static int padding = (44*4)-174;
    private final static byte serialMaxChars = 17;
    private final static String uriHeader = "urn:epc:tag:gdti-174:";
    // Table A-1 specifies the valid characters in serials, this set is to make the validators more maintenable
    private final static HashSet<Character> invalidTableA1Chars = Arrays.asList(0x23,0x24,0x40,0x5B,0x5C,0x5D,0x5E,0x60).stream().map(Character.class::cast).collect(Collectors.toCollection(HashSet::new));
    
    private String epc;
    
    private byte partition;
    private GdtiFilter filter;
    private long companyPrefix;
    private int documentType;
    private String serial;
    private String uri = null;

    private Gdti174(int filter,
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
        if (serial.length() > serialMaxChars)
            throw new IllegalArgumentException("Serial must at most " + serialMaxChars + " alphanumeric characters long");
        for (char ch : serial.toCharArray())
            if (ch < 0x21 || ch > 0x7A || invalidTableA1Chars.contains(ch))
                throw new IllegalArgumentException("Invalid serial character");
        this.serial = serial;
    }

    public static Gdti174 fromFields(int filter,
                                      int companyPrefixDigits,
                                      long companyPrefix,
                                      int documentType,
                                      String serial){
        return new Gdti174(filter, companyPrefixDigits, companyPrefix, documentType, serial);
    }

    public static Gdti174 fromGs1Key(int filter,int companyPrefixDigits, String ai253) {
        if (ai253.length()<14 || !StringUtils.isNumeric(ai253.substring(0, 13)))
            throw new IllegalArgumentException("GRAI (must be numeric) with serial must be 14 digits long");

        return new Gdti174(filter, companyPrefixDigits, Long.parseLong(ai253.substring(0, companyPrefixDigits)), Integer.parseInt(ai253.substring(companyPrefixDigits, 13 - 1)), ai253.substring(13));
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(44*4); //Gdti-174 epc is 44 hex chars long
            int i = serialSize+padding-1;

            for (byte b : serial.getBytes()) {
                for (int j = 6; j >= 0; j--,i--)
                    epc.set(i, ((b >> j) & 1) == 1);
            }

            i = serialSize+padding;
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
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getDocumentTypeDigits(partition)+"d",documentType)+"."+serial.chars().mapToObj(c -> getUriSerialChar((char) c)).collect(Collectors.joining());
        return uri;
    }

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Gdti174))
            return false;
        return ((Gdti174) o).getUri().equals(getUri());
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

    public static Gdti174 fromUri(String uri) {
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

        Gdti174 gdti174 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,documentType,sb.toString());
        gdti174.setUri(uri);
        return gdti174;
    }

    public static Gdti174 fromEpc(String epc) {
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

        final byte dtb = getDocumentTypeBits(partition);
        for(tmp = 0, i = 176 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 176 - 8 - 3 - 3 - cpb - dtb - 1;)
            tmp+=1L<<(i-(176-8-3-3-cpb-dtb));
        int documentType = (int) tmp;

        StringBuilder serialBuilder = new StringBuilder("");
        byte[] tmpba;

        i =176-8-3-3-cpb-dtb; //buffer size - epcheader.size - filter.size - partition.size - getCompanyPrefixBits(partition) - getDocumentTypeBits(partition)
        for(int j = 0;j < serialMaxChars && (tmpba = bs.get(i-7,i).toByteArray()).length!=0;i-=7,j++)
            serialBuilder.append(new String(tmpba));

        String serial = serialBuilder.toString();

        try{
            Gdti174 gdti174 = new Gdti174(filter,getCompanyPrefixDigits(partition),companyPrefix,documentType,serial);
            gdti174.setEpc(epc);
            return gdti174;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }
}
