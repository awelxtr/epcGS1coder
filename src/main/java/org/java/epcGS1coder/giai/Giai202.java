package org.java.epcGS1coder.giai;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * The Global Individual Asset Identifier EPC scheme is used to assign a unique identity to a specific
 * asset, such as a forklift or a computer.
 */

public final class Giai202 extends Giai{
    private final static byte epcHeader = 0b00111000;
    private final static int padding = 6;
    private final static byte individualAssetReferenceMaxChars = 24;
    private final static String uriHeader = "urn:epc:tag:giai-202:";
    // Table A-1 specifies the valid characters in serials, this set is to make the validators more maintenable
    private final static HashSet<Character> invalidTableA1Chars = Arrays.asList(0x23,0x24,0x40,0x5B,0x5C,0x5D,0x5E,0x60).stream().map(Character.class::cast).collect(Collectors.toCollection(HashSet::new));
    
    private String epc;
    
    private byte partition;
    private GiaiFilter filter;
    private long companyPrefix;
    private String individualAssetReference;
    private String uri = null;

    private Giai202(int filter,
                    int companyPrefixDigits,
                    long companyPrefix,
                    String individualAssetReference){
        this.filter = GiaiFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (individualAssetReference.length() > individualAssetReferenceMaxChars)
            throw new IllegalArgumentException("Individual Asset Reference must at most " + individualAssetReferenceMaxChars + " alphanumeric characters long");
        for (char ch : individualAssetReference.toCharArray())
            if (ch < 0x21 || ch > 0x7A || invalidTableA1Chars.contains(ch))
                throw new IllegalArgumentException("Invalid Individual Asset Reference character");
        this.individualAssetReference = individualAssetReference;
    }

    public static Giai202 fromFields(int filter,
                                      int companyPrefixDigits,
                                      long companyPrefix,
                                      String individualAssetReference){
        return new Giai202(filter, companyPrefixDigits, companyPrefix, individualAssetReference);
    }

    public static Giai202 fromGs1Key(int filter,int companyPrefixDigits, String ai8004) {
        if (ai8004.length()<7 || !StringUtils.isNumeric(ai8004.substring(0, companyPrefixDigits)))
            throw new IllegalArgumentException("GIAI (must be numeric) with individual asset reference must be at least 7 digits long");

        return new Giai202(filter, companyPrefixDigits, Long.parseLong(ai8004.substring(0, companyPrefixDigits)), ai8004.substring(companyPrefixDigits));
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(52*4); //Giai-202 epc is 52 hex chars long
            int i = getIndividualAssetReferenceBits(partition)+padding-1;

            for (byte b : individualAssetReference.getBytes()) {
                for (int j = 6; j >= 0; j--,i--)
                    epc.set(i, ((b >> j) & 1) == 1);
            }

            i = getIndividualAssetReferenceBits(partition)+padding;
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

    public String getIndividualAssetReference() {
        return individualAssetReference;
    }

    public String getUri() {
        if (uri == null)
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+individualAssetReference.chars().mapToObj(c -> getUriIndividualAssetReferenceChar((char) c)).collect(Collectors.joining());
        return uri;
    }

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Giai202))
            return false;
        return ((Giai202) o).getUri().equals(getUri());
    }

    private void setEpc(String epc){ this.epc = epc; }
    private void setUri(String uri){ this.uri = uri; }

    /**
     * Table A-1 for the encoding
     */
    private String getUriIndividualAssetReferenceChar(char ch){
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

    public static Giai202 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        String individualAssetReference = uriParts[2];
        StringBuilder sb = new StringBuilder();
        String[] iarSplit = individualAssetReference.split("%");
        sb.append(iarSplit[0]);
        for (int i = 1; i < iarSplit.length; i++){
            sb.append((char) Integer.parseInt(iarSplit[i].substring(0,2),16));
            sb.append(iarSplit[i].substring(2));
        }

        Giai202 giai202 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,sb.toString());
        giai202.setUri(uri);
        return giai202;
    }

    public static Giai202 fromEpc(String epc) {
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

        StringBuilder individualAssetReferenceBuilder = new StringBuilder("");
        byte[] tmpba;

        i =208-8-3-3-cpb; //buffer size - epcheader.size - filter.size - partition.size - getCompanyPrefixBits(partition)
        for(; (tmpba = bs.get(i-7,i).toByteArray()).length!=0;i-=7)
            individualAssetReferenceBuilder.append(new String(tmpba));

        String individualAssetReference = individualAssetReferenceBuilder.toString();
        
        try{
            Giai202 giai202 = new Giai202(filter,getCompanyPrefixDigits(partition),companyPrefix,individualAssetReference);
            giai202.setEpc(epc);
            return giai202;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }

    /**
     * Table 14-15 GIAI-202 Partition Table
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
                throw new IllegalArgumentException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-15 GIAI-202 Partition Table
     * @param partition
     * @return N value
     */
    protected static int getIndividualAssetReferenceBits(int partition){
        switch (partition){
            case 0:
                return 148;
            case 1:
                return 151;
            case 2:
                return 154;
            case 3:
                return 158;
            case 4:
                return 161;
            case 5:
                return 164;
            case 6:
                return 168;
            default:
                throw new IllegalArgumentException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-15 GIAI-202 Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    protected static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-15 GIAI-202 Partition Table
     * @param P
     * @return L
     */
    protected static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    /**
     * Table 14-15 GIAI-202 Partition Table
     * @param P
     */
    protected static int getIndividualAssetReferenceMaxLength(int partition){
        return 18+partition;
    }
}
