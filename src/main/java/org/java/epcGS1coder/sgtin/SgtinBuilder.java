package org.java.epcGS1coder.sgtin;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;

public class SgtinBuilder {

    private static final byte sgtin96EpcHeader = 0b00110000;
    private static final String sgtin96UriHeader = "urn:epc:tag:sgtin-96:";
    private static final byte serialSize = 38;

    public static Sgtin96 fromFields(int filter,
                                     byte partition,
                                     long companyPrefix,
                                     int itemReference,
                                     long serial){
        return fromFields(SgtinFilter.values()[filter],partition,companyPrefix,itemReference,serial);
    }

    public static Sgtin96 fromFields(SgtinFilter filter,
                                     byte partition,
                                     long companyPrefix,
                                     int itemReference,
                                     long serial){
        Sgtin96 sgtin96= new Sgtin96(filter,partition,companyPrefix,itemReference,serial);
        sgtin96.setEpc(encode(sgtin96));
        sgtin96.setUri(encodeUri(sgtin96));
        return sgtin96;
    }

    public static Sgtin96 fromEpc(String epc){
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
            tmp+=1<<(i-(96-8));
        if (tmp != sgtin96EpcHeader)
            throw new RuntimeException("Invalid header"); //maybe the decoder could choose the structure from the header?

        for(tmp = 0, i = 96 - 8; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 1;)
            tmp+=1<<(i-(96-8-3));
        SgtinFilter filter = SgtinFilter.values()[(int) tmp];

        for(tmp = 0, i = 96 - 8 - 3; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - 1;)
            tmp+=1<<(i-(96-8-3-3));
        byte partition = (byte) tmp;

        byte cpb = getCompanyPrefixBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - 1;)
            tmp+=1<<(i-(96-8-3-3-cpb));
        long companyPrefix = tmp;

        byte irb = getItemReferenceBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - irb - 1;)
            tmp+=1<<(i-(96-8-3-3-cpb-irb));
        int itemReference = (int) tmp;

        //for the remainder, which is the serial, we can use fixed values
        for(tmp = 0, i = serialSize; (i = bs.previousSetBit(i-1)) > -1;)
            tmp+=1<<i;
        long serial = tmp;

        Sgtin96 sgtin96 = new Sgtin96(filter,partition,companyPrefix,itemReference,serial);
        sgtin96.setEpc(bs);
        sgtin96.setUri(encodeUri(sgtin96));
        return sgtin96;
    }

    public static Sgtin96 fromUri(String uri){
        if (!uri.startsWith(sgtin96UriHeader))
            throw new RuntimeException("Decoding error: wrong URI header, expected " + sgtin96UriHeader);

        String uriParts[] = uri.substring(sgtin96UriHeader.length()).split("\\.");
        SgtinFilter filter = SgtinFilter.values()[Integer.parseInt(uriParts[0])];
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        int itemReference = Integer.parseInt(uriParts[2]);
        long serial = Long.parseLong(uriParts[3]);

        Sgtin96 sgtin96 = fromFields(filter,partition,companyPrefix,itemReference,serial);
        sgtin96.setUri(uri);

        return sgtin96;
    }

    public static String encodeUri(Sgtin96 sgtin96){
        return sgtin96UriHeader +
                String.valueOf(sgtin96.getFilter().getValue()) + "." +
                String.format("%0"+String.valueOf(getCompanyPrefixDigits(sgtin96.getPartition()))+"d", sgtin96.getCompanyPrefix()) + "." +
                String.format("%0"+String.valueOf(1+12-getCompanyPrefixDigits(sgtin96.getPartition()))+"d",sgtin96.getItemReference()) + "." +
                String.valueOf(sgtin96.getSerial());
    }

    /**
     * Table 14-2 SGTIN Partition Table
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
     * Table 14-2 SGTIN Partition Table
     * @param partition
     * @return N value
     */
    private static byte getItemReferenceBits(int partition){
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
                throw new RuntimeException("Invalid Partition: " + partition + " (0-6)");
        }
    }

    /**
     * Table 14-2 SGTIN Partition Table
     * @param companyPrefixDigits (L) value
     * @return P value
     */
    private static int getPartition(int companyPrefixDigits){
        return 12-companyPrefixDigits;
    }

    /**
     * Table 14-2 SGTIN Partition Table
     * @param P
     * @return L
     */
    private static int getCompanyPrefixDigits(int partition){
        return 12-partition;
    }

    private static BitSet encode(Sgtin96 sgtin96) {
        //values => epc
        BitSet epc = new BitSet(8*96); //Sgtin96*8 bits
        int i = 0;

        for (int j = 0; j < serialSize; j++,i++)
            epc.set(i, ((sgtin96.getSerial() >> j) & 1)==1);

        for (int j = 0; j < getItemReferenceBits(sgtin96.getPartition()); j++,i++)
            epc.set(i, ((sgtin96.getItemReference() >> j) & 1)==1);

        for (int j = 0; j < getCompanyPrefixBits(sgtin96.getPartition()); j++,i++)
            epc.set(i, ((sgtin96.getCompanyPrefix() >> j) & 1)==1);

        for (int j = 0; j < 3; j++,i++)
            epc.set(i, ((sgtin96.getPartition() >> j) & 1)==1);

        for (int j = 0; j < 3; j++,i++)
            epc.set(i, ((sgtin96.getFilter().getValue() >> j) & 1)==1);

        for (int j = 0; j < 8; j++,i++)
            epc.set(i, ((sgtin96EpcHeader >> j) & 1)==1);

        return epc;
    }
}
