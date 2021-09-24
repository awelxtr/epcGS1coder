package org.java.epcGS1coder.sgcn;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

/**
 * The Global Coupon Number EPC scheme is used to assign a unique identity to a coupon.
 */

public final class Sgcn96 {
    private final static byte epcHeader = 0b00111111;
    private final static int serialSize = 41;
    private final static byte serialMaxChars = 12;
    private final static String uriHeader = "urn:epc:tag:sgcn-96:";
    
    private String epc;
    
    private byte partition;
    private SgcnFilter filter;
    private long companyPrefix;
    private int couponReference;
    private String serial;
    private String uri = null;

    private Sgcn96(int filter,
                   int companyPrefixDigits,
                   long companyPrefix,
                   int couponReference,
                   String serial){
        this.filter = SgcnFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (couponReference >= 1l<<getCouponReferenceBits(partition))
            throw new IllegalArgumentException("Coupon reference too large, max value (exclusive):" + (1l<<getCouponReferenceBits(partition)));
        this.couponReference = couponReference;
        if (!StringUtils.isNumeric(serial) || serial.length() > serialMaxChars)
            throw new IllegalArgumentException("Serial must be numeric and shorter than 12 digits");
        this.serial = serial;
    }

    public static Sgcn96 fromFields(int filter,
                                    int companyPrefixDigits,
                                    long companyPrefix,
                                    int couponReference,
                                    String serial){
        return new Sgcn96(filter, companyPrefixDigits, companyPrefix, couponReference, serial);
    }

    public static Sgcn96 fromGs1Key(int filter,int companyPrefixDigits, String ai255) {
        if (ai255.length()<14 || ai255.length()>25 || !StringUtils.isNumeric(ai255))
            throw new IllegalArgumentException("GCN with serial must be between 14 and 25 digits long");

        return new Sgcn96(filter, companyPrefixDigits, Long.parseLong(ai255.substring(0, companyPrefixDigits)), Integer.parseInt(ai255.substring(companyPrefixDigits, 13 - 1)), ai255.substring(13));
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(96); 
            int i = 0;

            long serial = Long.parseLong("1"+this.serial); // Numeric string encoding prepends a "1" at the beginning of the encoded serial

            for (int j = 0; j < serialSize; j++,i++)
                epc.set(i, ((serial >> j) & 1)==1);

            for (int j = 0; j < getCouponReferenceBits(partition); j++,i++)
                epc.set(i, ((couponReference >> j) & 1)==1);

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

    public int getCouponReference() {
        return couponReference;
    }

    public String getSerial() {
        return serial;
    }

    public String getUri() {
        if (uri == null)
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getCouponReferenceDigits(partition)+"d",couponReference)+"."+serial;
        return uri;
    }

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Sgcn96))
            return false;
        return ((Sgcn96) o).getUri().equals(getUri());
    }

    private void setEpc(String epc){ this.epc = epc; }
    private void setUri(String uri){ this.uri = uri; }

    public static Sgcn96 fromUri(String uri) {
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

        Sgcn96 sgcn96 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,documentType,sb.toString());
        sgcn96.setUri(uri);
        return sgcn96;
    }

    public static Sgcn96 fromEpc(String epc) {
        ArrayList<String> a = new ArrayList<String>();
        for (int i = 0; i<epc.length(); i+=2) {
            a.add(epc.substring(i, i+2));
        }

        ByteBuffer bb = ByteBuffer.allocate(12);
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

        byte crb = getCouponReferenceBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - crb - 1;)
            tmp+=1L<<(i-(96-8-3-3-cpb-crb));
        int documentType = (int) tmp;

        //for the remainder, which is the serial, we can use fixed values
        for(tmp = 0, i = serialSize; (i = bs.previousSetBit(i-1)) > -1;)
            tmp+=1L<<i;
        String serial = String.valueOf(tmp).substring(1); // Numeric string encoding prepends a "1" at the beginning of the encoded serial

        try{
            Sgcn96 sgcn96 = new Sgcn96(filter,getCompanyPrefixDigits(partition),companyPrefix,documentType,serial);
            sgcn96.setEpc(epc);
            return sgcn96;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }

    /**
     * Table 14-28 SGCN Partition Table
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
     * Table 14-28 SGCN Partition Table
     * @param partition
     * @return N value
     */
    private static byte getCouponReferenceBits(int partition){
        switch (partition){
            case 0:
                return 1;
            case 1:
                return 4;
            case 2:
                return 7;
            case 3:
                return 11;
            case 4:
                return 14;
            case 5:
                return 17;
            case 6:
                return 21;
            default:
                throw new IllegalArgumentException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-28 SGCN Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    private static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-28 SGCN Partition Table
     * @param P
     * @return L
     */
    private static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    /**
     * Table 14-28 SGCN Partition Table
     * @param P
     */
    private static int getCouponReferenceDigits(int partition){
        return partition;
    }

    enum SgcnFilter{
        all_others_0(0),
        reserved_1(1),
        reserved_2(2),
        reserved_3(3),
        reserved_4(4),
        reserved_5(5),
        reserved_6(6),
        reserved_7(7);

        private int value;

        SgcnFilter(int value) {
            this.value = value;
        }
        public int getValue(){
            return value;
        }
    }
}
