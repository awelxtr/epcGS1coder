package org.java.epcGS1coder.sscc;

import java.util.BitSet;

public class Sscc96 {

    private final byte epcHeader = 0b00110001;
    private final byte serialSize = 24;
    private final byte GtinMaxSize = 44;

    private BitSet epc;

    private SsccFilter filter;
    private byte partition;

    private long companyPrefix;
    private int itemReference;
    private long serial;

    private String uri = null;

    Sscc96(int filter,
            byte partition,
            long companyPrefix,
            int itemReference,
            long serial){
        this(SsccFilter.values()[filter], partition, companyPrefix, itemReference, serial);
    }

    Sscc96(SsccFilter filter,
            byte partition,
            long companyPrefix,
            int itemReference,
            long serial){
        this.filter = filter;
        this.partition = partition;
        this.companyPrefix = companyPrefix;
        this.itemReference = itemReference;
        this.serial = serial;
    }

    public SsccFilter getFilter() {
        return filter;
    }

    public byte getPartition() {
        return partition;
    }

    public long getCompanyPrefix() {
        return companyPrefix;
    }

    public int getItemReference() {
        return itemReference;
    }

    public long getSerial() {
        return serial;
    }

    public String getUri() {
        throw new RuntimeException("NOT_IMPLEMENTED");
    }

    public String getEpc(){
        throw new RuntimeException("NOT_IMPLEMENTED");
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
                throw new RuntimeException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-5 SSCC Partition Table
     * @param partition
     * @return N value
     */
    private static byte getItemReferenceBits(int partition){
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
                throw new RuntimeException("Invalid Partition: " + partition + " (0-6)");
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
    private static int getItemReferenceDigits(int partition){
        return partition+5;
    }
}
