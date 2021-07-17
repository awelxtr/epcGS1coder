package org.java.epcGS1coder.cpi;

public class CpiVar extends Cpi{
    
    /**
     * Table 14-25 CPI-var Partition Table
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
     * Table 14-25 CPI-var Partition Table
     * @param partition
     * @return N value
     */
    protected static short getComponentPartReferenceMaximumBits(int partition){
        switch (partition){
            case 0:
                return 114;
            case 1:
                return 120;
            case 2:
                return 126;
            case 3:
                return 132;
            case 4:
                return 138;
            case 5:
                return 144;
            case 6:
                return 150;
            default:
                throw new RuntimeException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-25 CPI-var Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    protected static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-25 CPI-var Partition Table
     * @param P
     * @return L
     */
    protected static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    /**
     * Table 14-25 CPI-var Partition Table
     * @param P
     */
    protected static int getComponentPartReferenceMaximumDigits(int partition){
        return partition+18;
    }
}
