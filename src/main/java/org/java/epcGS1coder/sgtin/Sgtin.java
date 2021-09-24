package org.java.epcGS1coder.sgtin;

class Sgtin {

    /**
     * Table 14-2 SGTIN Partition Table
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
     * Table 14-2 SGTIN Partition Table
     * @param partition
     * @return N value
     */
    protected static byte getItemReferenceBits(int partition){
        switch (partition){
            case 0:
                return 4;
            case 1:
                return 7;
            case 2:
                return 10;
            case 3:
                return 14;
            case 4:
                return 17;
            case 5:
                return 20;
            case 6:
                return 24;
            default:
                throw new IllegalArgumentException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-2 SGTIN Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    protected static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-2 SGTIN Partition Table
     * @param P
     * @return L
     */
    protected static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    /**
     * Table 14-2 SGTIN Partition Table
     * @param P
     */
    protected static int getItemReferenceDigits(int partition){
        return partition+1;
    }

    protected enum SgtinFilter {
        all_others_0(0),
        pos_item_1(1),
        case_2(2),
        inner_pack_4(4),
        reserved_3(3),
        reserved_5(5),
        unit_load_6(6),
        component_7(7);

        private int value;

        SgtinFilter(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
