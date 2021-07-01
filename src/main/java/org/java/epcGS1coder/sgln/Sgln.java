package org.java.epcGS1coder.sgln;

public class Sgln {

    /**
     * Table 14-7 SGLN Partition Table
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
                throw new RuntimeException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-7 SGLN Partition Table
     * @param partition
     * @return N value
     */
    protected static byte getLocationReferenceBits(int partition){
        switch (partition){
            case 0:
                return 1;
            case 1:
                return 4;
            case 2:
                return 7;
            case 3:
                return 10;
            case 4:
                return 14;
            case 5:
                return 17;
            case 6:
                return 21;
            default:
                throw new RuntimeException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-7 SGLN Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    protected static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-7 SGLN Partition Table
     * @param P
     * @return L
     */
    protected static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    /**
     * Table 14-7 SGLN Partition Table
     * @param P
     */
    protected static int getLocationReferenceDigits(int partition){
        return partition;
    }

    protected enum SglnFilter{
        all_others_0(0),
        reserved_1(1),
        reserved_2(2),
        reserved_3(3),
        reserved_4(4),
        reserved_5(5),
        reserved_6(6),
        reserved_7(7);

        protected int value;

        SglnFilter(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
