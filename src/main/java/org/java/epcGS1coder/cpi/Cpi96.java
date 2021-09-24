package org.java.epcGS1coder.cpi;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

/**
 * The Component / Part EPC identifier is designed for use by the technical industries (including the
 * automotive sector) for the unique identification of parts or components.
 * 
 * The CPI EPC construct provides a mechanism to directly encode unique identifiers in RFID tags and
 * to use the URI representations at other layers of the EPCglobal architecture.
 */

public final class Cpi96 extends Cpi{

    private final static byte epcHeader = 0b00111100;
    private final static byte serialSize = 31;
    private static final String uriHeader = "urn:epc:tag:cpi-96:";
    
    private String epc = null;
    
    private CpiFilter filter;
    private byte partition;
    
    private long companyPrefix;
    private int componentPartReference;
    private long serial; 
    
    private String uri = null;
    
    private Cpi96(int filter,
                  int companyPrefixDigits,
                  long companyPrefix,
                  int componentPartReference,
                  long serial){
        this.filter = CpiFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (componentPartReference >= 1l<<getComponentPartReferenceBits(partition))
            throw new IllegalArgumentException("Component Part Reference too large, max value (exclusive):" + (1l<<getComponentPartReferenceBits(partition)));
        this.componentPartReference = componentPartReference;
        if (serial >= 1l<<serialSize)
            throw new IllegalArgumentException("Serial too large, max value (exclusive):" + (1l<<serialSize));
        this.serial = serial;
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(96); 
            int i = 0;

            for (int j = 0; j < serialSize; j++,i++)
                epc.set(i, ((serial >> j) & 1)==1);

            for (int j = 0; j < getComponentPartReferenceBits(partition); j++,i++)
                epc.set(i, ((componentPartReference >> j) & 1)==1);

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

    public int getComponentPartReference() {
        return componentPartReference;
    }

    public long getSerial() {
        return serial;
    }

    public String getUri(){
        if (uri == null)
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+componentPartReference+"."+String.valueOf(serial);
        return uri;
    }
    private void setUri(String uri){
        this.uri = uri;
    };

    public static Cpi96 fromFields(int filter,
                                    int companyPrefixDigits,
                                    long companyPrefix,
                                    int componentPartReference,
                                    long serial){
        return new Cpi96(filter, companyPrefixDigits,companyPrefix,componentPartReference,serial);
    }

    public static Cpi96 fromGs1Key(int filter,int companyPrefixDigits, String ai8010, long ai8011) {
        if (ai8010.length()<7 || ai8010.length()>30 || !StringUtils.isNumeric(ai8010))
            throw new IllegalArgumentException("CPI must be between 7 and 30 digits long");
        return new Cpi96(filter, companyPrefixDigits, Long.parseLong(ai8010.substring(0, companyPrefixDigits)), Integer.parseInt(ai8010.substring(companyPrefixDigits)), ai8011);
    }


    public static Cpi96 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        int componentPartReference = Integer.parseInt(uriParts[2]);
        long serial = Long.parseLong(uriParts[3]);

        Cpi96 cpi96 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,componentPartReference,serial);
        cpi96.setUri(uri);

        return cpi96;
    }

    public static Cpi96 fromEpc(String epc) {
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

        byte cprb = getComponentPartReferenceBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - cprb - 1;)
            tmp+=1L<<(i-(96-8-3-3-cpb-cprb));
        int componentPartReference = (int) tmp;

        //for the remainder, which is the serial, we can use fixed values
        for(tmp = 0, i = serialSize; (i = bs.previousSetBit(i-1)) > -1;)
            tmp+=1L<<i;
        long serial = tmp;

        try{
            Cpi96 cpi96 = new Cpi96(filter,getCompanyPrefixDigits(partition),companyPrefix,componentPartReference,serial);
            cpi96.setEpc(epc);
            return cpi96;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Cpi96))
            return false;
        return ((Cpi96) o).getUri().equals(getUri());
    }

    /**
     * Table 14-20 CPI-96 Partition Table
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
     * Table 14-20 CPI-96 Partition Table
     * @param partition
     * @return N value
     */
    protected static byte getComponentPartReferenceBits(int partition){
        switch (partition){
            case 0:
                return 11;
            case 1:
                return 14;
            case 2:
                return 17;
            case 3:
                return 21;
            case 4:
                return 24;
            case 5:
                return 27;
            case 6:
                return 31;
            default:
                throw new IllegalArgumentException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-20 CPI-96 Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    protected static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-20 CPI-96 Partition Table
     * @param P
     * @return L
     */
    protected static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    /**
     * Table 14-20 CPI-96 Partition Table
     * @param P
     */
    protected static int getComponentPartReferenceDigits(int partition){
        return partition+3;
    }
}
