package org.java.epcGS1coder.giai;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

/**
 * The Global Individual Asset Identifier EPC scheme is used to assign a unique identity to a specific
 * asset, such as a forklift or a computer.
 */

public final class Giai96 extends Giai {

    private final static byte epcHeader = 0b00110100;
    private static final String uriHeader = "urn:epc:tag:giai-96:";
    
    private String epc = null;
    
    private GiaiFilter filter;
    private byte partition;
    
    private long companyPrefix;
    private long individualAssetReference;
    
    private String uri = null;
    
    private Giai96(int filter,
                   int companyPrefixDigits,
                   long companyPrefix,
                   long individualAssetReference){
        this.filter = GiaiFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (individualAssetReference >= 1l<<getIndividualAssetReferenceBits(partition))
            throw new IllegalArgumentException("Individual Asset Reference too large, max value (exclusive):" + (1l<<getIndividualAssetReferenceBits(partition)));
        this.individualAssetReference = individualAssetReference;
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(96);
            int i = 0;

            for (int j = 0; j < getIndividualAssetReferenceBits(partition); j++,i++)
                epc.set(i, ((individualAssetReference >> j) & 1)==1);

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

    public long getIndividualAssetReference() {
        return individualAssetReference;
    }

    public String getUri(){
        if (uri == null)
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+individualAssetReference;
        return uri;
    }
    private void setUri(String uri){
        this.uri = uri;
    };

    public static Giai96 fromFields(int filter,
                                    int companyPrefixDigits,
                                    long companyPrefix,
                                    long individualAssetReference){
        return new Giai96(filter, companyPrefixDigits,companyPrefix,individualAssetReference);
    }

    public static Giai96 fromGs1Key(int filter,int companyPrefixDigits, String ai8004) {
        if (ai8004.length()<7 || !StringUtils.isNumeric(ai8004))
            throw new IllegalArgumentException("GRAI with Individual Asset Reference must be at least 7 digits long");
        return new Giai96(filter, companyPrefixDigits, Long.parseLong(ai8004.substring(0, companyPrefixDigits)), Integer.parseInt(ai8004.substring(companyPrefixDigits)));
    }


    public static Giai96 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        int individualAssetReference = Integer.parseInt(uriParts[2]);

        Giai96 grai96 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,individualAssetReference);
        grai96.setUri(uri);

        return grai96;
    }

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Giai96))
            return false;
        return ((Giai96) o).getUri().equals(getUri());
    }

    public static Giai96 fromEpc(String epc) {
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

        byte irb = getIndividualAssetReferenceBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - irb - 1;)
            tmp+=1L<<(i-(96-8-3-3-cpb-irb));
        int individualAssetReference = (int) tmp;

        try{
            Giai96 grai96 = new Giai96(filter,getCompanyPrefixDigits(partition),companyPrefix,individualAssetReference);
            grai96.setEpc(epc);
            return grai96;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }

    /**
     * Table 14-13 GIAI-96 Partition Table
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
     * Table 14-13 GIAI-96 Partition Table
     * @param partition
     * @return N value
     */
    protected static byte getIndividualAssetReferenceBits(int partition){
        switch (partition){
            case 0:
                return 42;
            case 1:
                return 45;
            case 2:
                return 48;
            case 3:
                return 52;
            case 4:
                return 55;
            case 5:
                return 58;
            case 6:
                return 62;
            default:
                throw new IllegalArgumentException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-13 GIAI-96 Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    protected static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-13 GIAI-96 Partition Table
     * @param P
     * @return L
     */
    protected static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    /**
     * Table 14-13 GIAI-96 Partition Table
     * @param P
     */
    protected static int getIndividualAssetReferenceDigits(int partition){
        return 13+partition;
    }
}
