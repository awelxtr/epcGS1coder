package org.java.epcGS1coder.sgln;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

/**
 * The SGLN EPC scheme is used to assign a unique identity to a physical location, such as a specific
 * building or a specific unit of shelving within a warehouse.
 */

public final class Sgln96 extends Sgln {

    private final static byte epcHeader = 0b00110010;
    private static final String uriHeader = "urn:epc:tag:sgln-96:";
    private static final int extensionSize = 41;

    private String epc = null;
    private String uri = null;

    private SglnFilter filter;
    private byte partition;
    private long companyPrefix;
    private int locationReference;
    private long extension;

    private Sgln96(int filter,
                   int companyPrefixDigits,
                   long companyPrefix,
                   int locationReference,
                   long extension){
        this.filter = SglnFilter.values()[filter];
        this.partition = (byte) getPartition(companyPrefixDigits);
        if (companyPrefix >= 1l<<getCompanyPrefixBits(partition))
            throw new IllegalArgumentException("Company Prefix too large, max value (exclusive):" + (1l<<getCompanyPrefixBits(partition)));
        this.companyPrefix = companyPrefix;
        if (locationReference >= 1l<<getLocationReferenceBits(partition))
            throw new IllegalArgumentException("Location Reference too large, max value (exclusive):" + (1l<<getLocationReferenceBits(partition)));
        this.locationReference = locationReference;
        if (extension >= 1l<<extensionSize)
            throw new IllegalArgumentException("Extension too large, max value (exclusive):" + (1l<<extensionSize));
        this.extension = extension;
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(96);
            int i = 0;

            for (int j = 0; j < extensionSize; j++,i++)
                epc.set(i, ((extension >> j) & 1)==1);

            for (int j = 0; j < getLocationReferenceBits(partition); j++,i++)
                epc.set(i, ((locationReference >> j) & 1)==1);

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

    private void setEpc(String epc) { this.epc = epc; }
    public int getFilter() { return filter.getValue(); }
    public long getCompanyPrefix() { return companyPrefix; }
    public int getLocationReference() { return locationReference; }
    public long getExtension() { return extension; }

    public String getUri(){
        if (uri == null)
            uri = uriHeader+String.valueOf(filter.getValue())+"."+String.format("%0"+getCompanyPrefixDigits(partition)+"d",companyPrefix) +"."+String.format("%0"+getLocationReferenceDigits(partition)+"d",locationReference)+"."+String.valueOf(extension);
        return uri;
    }
    private void setUri(String uri) { this.uri = uri; }

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Sgln96))
            return false;
        return ((Sgln96) o).getUri().equals(getUri());
    }

    public static Sgln96 fromFields(int filter,
                                    int companyPrefixDigits,
                                    long companyPrefix,
                                    int locationReference,
                                    long extension){
        return new Sgln96(filter, companyPrefixDigits, companyPrefix, locationReference, extension);
    }

    public static Sgln96 fromGs1Key(int filter,int companyPrefixDigits, String ai414, long ai254) {
        if (ai414.length()!=13 || !StringUtils.isNumeric(ai414))
            throw new IllegalArgumentException("GLN must be 13 digits long");
        return new Sgln96(filter, companyPrefixDigits, Long.parseLong(ai414.substring(0, companyPrefixDigits)), Integer.parseInt(ai414.substring(companyPrefixDigits, 13-1)), ai254);
    }

    public static Sgln96 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        byte partition = (byte) getPartition(uriParts[1].length());
        long companyPrefix = Long.parseLong(uriParts[1]);
        int locationReference = Integer.parseInt(uriParts[2]);
        long extension = Long.parseLong(uriParts[3]);

        Sgln96 sgln96 = fromFields(filter,getCompanyPrefixDigits(partition),companyPrefix,locationReference,extension);
        sgln96.setUri(uri);

        return sgln96;
    }

    public static Sgln96 fromEpc(String epc) {
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

        byte lrb = getLocationReferenceBits(partition);
        for(tmp = 0, i = 96 - 8 - 3 - 3 - cpb; (i = bs.previousSetBit(i-1)) > 96 - 8 - 3 - 3 - cpb - lrb - 1;)
            tmp+=1L<<(i-(96-8-3-3-cpb-lrb));
        int locationReference = (int) tmp;

        //for the remainder, which is the extension, we can use fixed values
        for(tmp = 0, i = extensionSize; (i = bs.previousSetBit(i-1)) > -1;)
            tmp+=1L<<i;
        long extension = tmp;
        try {
            Sgln96 sgln96 = new Sgln96(filter,getCompanyPrefixDigits(partition),companyPrefix,locationReference,extension);
            sgln96.setEpc(epc);
            return sgln96;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }
}
