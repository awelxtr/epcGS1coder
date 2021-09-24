package org.java.epcGS1coder.sscc;

import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * The Serial Shipping Container Code EPC scheme is used to assign a unique identity to a logistics 
 * handling unit, such as the aggregate contents of a shipping container or a pallet load.
 */

public final class Sscc96 {

    private static final byte epcHeader = 0b00110001;
    private static final byte reservedSize = 24;
    private static final String tagUriHeader = "urn:epc:tag:sscc-96:";

    private String epc;

    private SsccFilter filter;
    private byte partition;

    private long companyPrefix;
    private long serialReference;

    private String uri = null;

    private Sscc96(int filter,
                   int companyPrefixDigits,
                   long companyPrefix,
                   long serialReference){
        this.filter = SsccFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (serialReference >= 1l<<getSerialReferenceBits(partition))
            throw new IllegalArgumentException("Serial reference too large, max value (exclusive):" + (1l<<getSerialReferenceBits(partition)));
        this.serialReference = serialReference;
    }

    public static Sscc96 fromFields(int filter,
                             int companyPrefixDigits,
                             long companyPrefix,
                             long serialReference){
        return new Sscc96(filter, companyPrefixDigits, companyPrefix, serialReference);
    }

    public static Sscc96 fromGs1Key(int filter,int companyPrefixDigits,String ai00){
        if (ai00.length()!=18 || !StringUtils.isNumeric(ai00))
            throw new IllegalArgumentException("AI 00 must be 18 digits long");

        return new Sscc96(filter, companyPrefixDigits, Long.parseLong(ai00.substring(1,companyPrefixDigits+1)), Long.parseLong(ai00.charAt(0)+ai00.substring(companyPrefixDigits+1,17)));
    }

    public static Sscc96 fromEpc(String epc){
        if (epc.length()<24)
            throw new IllegalArgumentException("Invalid EPC: shorter than 96 bits");

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
            tmp += 1l << (i - (96 - 8));
        if (tmp != epcHeader)
            throw new IllegalArgumentException("Invalid header"); //maybe the decoder could choose the structure from the header?

        for(tmp = 0, i = 96 - 8; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 1;)
            tmp += 1l << (i - (96 - 8 - 3));
        int filter = (int) tmp;

        for(tmp = 0, i = 96 - 8 - 3; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - 1;)
            tmp += 1l << (i - (96 - 8 - 3 - 3));
        byte partition = (byte) tmp;

        byte cpb = getCompanyPrefixBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - 1;)
            tmp += 1l << (i - (96 - 8 - 3 - 3 - cpb));
        long companyPrefix = tmp;

        byte irb = getSerialReferenceBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > reservedSize - 1;)
            tmp += 1l << (i - (96 - 8 - 3 - 3 - cpb - irb));
        long serialReference = tmp;

        try{
            Sscc96 sscc96 = new Sscc96(filter,partition,companyPrefix,serialReference);
            sscc96.setEpc(epc);
            return sscc96;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }

    public static Sscc96 fromUri(String uri){
        if (!uri.startsWith(tagUriHeader))
            throw new IllegalArgumentException("Wrong URI");
        String[] uriParts = uri.split(":");
        uriParts = uriParts[uriParts.length-1].split("\\.");
        if ((uriParts[1].length() + uriParts[2].length())<17)
            throw new IllegalArgumentException("Company prefix and serial reference must have 17 digits in total");
        int filter = Integer.parseInt(uriParts[0]);
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        long serialReference = Long.parseLong(uriParts[2]);
        Sscc96 sscc96 = new Sscc96(filter,getCompanyPrefixDigits(partition),companyPrefix,serialReference);
        sscc96.setUri(uri);
        return sscc96;
    }

    public int getFilter() {
        return filter.getValue();
    }

    public long getCompanyPrefix() {
        return companyPrefix;
    }

    public long getSerialReference() {
        return serialReference;
    }

    private void setEpc(String epc){ this.epc = epc; }
    private void setUri(String uri){ this.uri = uri; }
    public String getUri() {
        if (uri == null){
            uri = tagUriHeader+filter.getValue()+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix)+"."+String.format("%0"+getSerialReferenceDigits(partition)+"d",serialReference);
        }
        return uri;
    }

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Sscc96))
            return false;
        return ((Sscc96) o).getUri().equals(getUri());
    }

    public String getEpc(){
        if (epc == null ){
            BitSet epc = new BitSet(96);
            int i = reservedSize;

            for (int j = 0; j < getSerialReferenceBits(partition); j++,i++)
                epc.set(i, ((serialReference >> j) & 1)==1);

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

    /**
     * Table 14-5 SSCC Partition Table
     * @param partition
     * @return M value
     */
    private static byte getCompanyPrefixBits(int partition){
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
     * Table 14-5 SSCC Partition Table
     * @param partition
     * @return N value
     */
    private static byte getSerialReferenceBits(int partition){
        switch (partition){
            case 0:
                return 18;
            case 1:
                return 21;
            case 2:
                return 24;
            case 3:
                return 28;
            case 4:
                return 31;
            case 5:
                return 34;
            case 6:
                return 38;
            default:
                throw new IllegalArgumentException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-5 SSCC Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    private static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-5 SSCC Partition Table
     * @param P
     * @return L
     */
    private static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    /**
     * Table 14-5 SSCC Partition Table
     * @param P
     */
    private static int getSerialReferenceDigits(int partition){
        return partition+5;
    }

    enum SsccFilter {
        all_others_0(0),
        reserved_1(1),
        case_2(2),
        reserved_3(3),
        reserved_4(4),
        reserved_5(5),
        unit_load_6(6),
        reserved_7(7);

        private int value;

        SsccFilter(int value) {
            this.value = value;
        }
        public int getValue(){
            return value;
        }
    }
}
