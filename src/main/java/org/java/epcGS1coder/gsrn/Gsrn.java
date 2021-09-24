package org.java.epcGS1coder.gsrn;

class Gsrn {

    /**
     * Table 14-17 GSRN Partition Table
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
     * Table 14-17 GSRN Partition Table
     * @param partition
     * @return N value
     */
    protected static byte getServiceReferenceBits(int partition){
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
     * Table 14-17 GSRN Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    protected static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-17 GSRN Partition Table
     * @param P
     * @return L
     */
    protected static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    /**
     * Table 14-17 GSRN Partition Table
     * @param P
     */
    protected static int getServiceReferenceDigits(int partition){
        return partition+5;
    }

    protected enum GsrnFilter{
        all_others_0(0),
        reserved_1(1),
        reserved_2(2),
        reserved_3(3),
        reserved_4(4),
        reserved_5(5),
        reserved_6(6),
        reserved_7(7);

        protected int value;

        GsrnFilter(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}

