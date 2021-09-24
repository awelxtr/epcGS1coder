package org.java.epcGS1coder.gid;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * The General Identifier EPC scheme is independent of any specifications or identity scheme outside
 * the EPCglobal Tag Data Standard.
 */

public final class Gid96 {
    private final static byte epcHeader = 0b00110101;
    private final static byte generalManagerNumberSize = 28;
    private final static byte objectClassSize = 24;
    private final static byte serialSize = 36;
    private static final String uriHeader = "urn:epc:tag:gid-96:";
    
    private String epc = null;
    private String uri = null;

    private int generalManagerNumber;
    private int objectClass;
    private long serial;

    private Gid96(int generalManagerNumber,
                  int objectClass,
                  long serial){
        if (generalManagerNumber > 1l<<generalManagerNumberSize)
            throw new IllegalArgumentException("General Manager Number too large, max number (exclusive): "+ (1l<<generalManagerNumberSize));
        this.generalManagerNumber = generalManagerNumber;
        if (objectClass > 1l<<objectClassSize)
            throw new IllegalArgumentException("Object class too large, max number (exclusive): "+ (1l<<objectClassSize));
        this.objectClass = objectClass;
        if (serial > 1l<<serialSize)
            throw new IllegalArgumentException("Serial too large, max number (exclusive): "+ (1l<<serialSize));
        this.serial = serial;
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(96); 
            int i = 0;

            for (int j = 0; j < serialSize; j++,i++)
                epc.set(i, ((serial >> j) & 1)==1);
            
            for (int j = 0; j < objectClassSize; j++,i++)
                epc.set(i, ((objectClass >> j) & 1)==1);
            
            for (int j = 0; j < generalManagerNumberSize; j++,i++)
                epc.set(i, ((generalManagerNumber >> j) & 1)==1);

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

    public int getGeneralManagerNumber() {
        return generalManagerNumber;
    }
    public int getObjectClass() {
        return objectClass;
    }
    public long getSerial() {
        return serial;
    }

    public String getUri(){
        if (uri == null)
            uri = uriHeader+String.valueOf(generalManagerNumber)+"."+String.valueOf(objectClass)+"."+String.valueOf(serial);
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
        if (!(o instanceof Gid96))
            return false;
        return ((Gid96) o).getUri().equals(getUri());
    }

    public static Gid96 fromFields(int generalManagerNumber,
                                   int objectClass,
                                   long serial){
        return new Gid96(generalManagerNumber, objectClass, serial);
    }

    public static Gid96 fromUri(String uri) {
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int generalManagerNumber = Integer.parseInt(uriParts[0]);
        int objectClass = Integer.parseInt(uriParts[1]);
        long serial = Long.parseLong(uriParts[2]);

        Gid96 gid96 = fromFields(generalManagerNumber,objectClass,serial);
        gid96.setUri(uri);

        return gid96;
    }

    public static Gid96 fromEpc(String epc) {
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

        for(tmp = 0, i = 96 - 8; (i = bs.previousSetBit(i-1)) > 96 - 8 - generalManagerNumberSize - 1;)
            tmp+=1L<<(i-(96-8-generalManagerNumberSize));
        int generalManagerNumber = (int) tmp;


        for(tmp = 0, i = 96 - 8 - generalManagerNumberSize; (i = bs.previousSetBit(i-1)) > 96 - 8 - generalManagerNumberSize - objectClassSize - 1;)
        tmp+=1L<<(i-(96-8-generalManagerNumberSize-objectClassSize));
        int objectClass = (int) tmp;

        //for the remainder, which is the serial, we can use fixed values
        for(tmp = 0, i = serialSize; (i = bs.previousSetBit(i-1)) > -1;)
            tmp+=1L<<i;
        long serial = tmp;

        try{
            Gid96 gid96 = fromFields(generalManagerNumber,objectClass,serial);
            gid96.setEpc(epc);
            return gid96;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }
}
