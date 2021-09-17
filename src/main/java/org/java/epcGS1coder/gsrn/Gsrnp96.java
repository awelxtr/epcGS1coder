package org.java.epcGS1coder.gsrn;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

/**
 * The Global Service Relation Number – Provider (GSRNP) EPC scheme is used to assign a unique
 * identity to a service provider.
 */

public final class Gsrnp96 extends Gsrn{

    private final static byte epcHeader = 0b00101110;
    private final static String uriHeader = "urn:epc:tag:gsrnp-96:";
    
    private Gsrnp96(int filter,
                    int companyPrefixDigits,
                    long companyPrefix,
                    long serviceReference){
        this.filter = GsrnFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        this.companyPrefix = companyPrefix;
        this.serviceReference = serviceReference;
    }

    protected String epc = null;
    
    protected GsrnFilter filter;
    protected byte partition;
    
    protected long companyPrefix;
    protected long serviceReference;
    
    protected String uri = null;
    
    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(96);
            int i = 0;

            for (int j = 0; j < getServiceReferenceBits(partition); j++,i++)
                epc.set(i, ((serviceReference >> j) & 1)==1);

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
            sb.append("000000"); //gsrn final 24 bits are 0s
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

    public long getServiceReference() {
        return serviceReference;
    }

    public String getUri(){
        if (uri == null)
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+serviceReference;
        return uri;
    }
    private void setUri(String uri){
        this.uri = uri;
    };

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Gsrnp96))
            return false;
        return ((Gsrnp96) o).getUri().equals(getUri());
    }

    public static Gsrnp96 fromFields(int filter,
                                    int companyPrefixDigits,
                                    long companyPrefix,
                                    long serviceReference){
        return new Gsrnp96(filter, companyPrefixDigits,companyPrefix,serviceReference);
    }

    public static Gsrnp96 fromGs1Key(int filter,int companyPrefixDigits, String ai8017) {
        if (ai8017.length()!=18 || !StringUtils.isNumeric(ai8017))
            throw new RuntimeException("GSRN-Provider must be 18 digits");
        return new Gsrnp96(filter, companyPrefixDigits, Long.parseLong(ai8017.substring(0, companyPrefixDigits)), Long.parseLong(ai8017.substring(companyPrefixDigits,ai8017.length()-1)));
    }


    public static Gsrnp96 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new RuntimeException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        long serviceReference = Long.parseLong(uriParts[2]);

        Gsrnp96 gsrnp96 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,serviceReference);
        gsrnp96.setUri(uri);

        return gsrnp96;
    }

    public static Gsrnp96 fromEpc(String epc) {
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
            throw new RuntimeException("Invalid header"); //maybe the decoder could choose the structure from the header?

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

        byte srb = getServiceReferenceBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - srb - 1;)
            tmp+=1L<<(i-(96-8-3-3-cpb-srb));
        long serviceReference = tmp;

        Gsrnp96 gsrnp96 = new Gsrnp96(filter,getCompanyPrefixDigits(partition),companyPrefix,serviceReference);
        gsrnp96.setEpc(epc);
        return gsrnp96;
    }
}
