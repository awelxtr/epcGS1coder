package org.java.epcGS1coder.usdod;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * <p>The US Department of Defense identifier is defined by the United States Department of Defense. This tag data construct may be used to encode 96-bit Class 1 tags for shipping goods to the United States Department of Defense by a supplier who has already been assigned a CAGE (Commercial and Government Entity) code. </p>
 * <p>At the time of this writing, the details of what information to encode into these fields is explained in a document titled "United States Department of Defense Supplier's Passive RFID Information Guide" that can be obtained at the United States Department of Defense's web site (http://www.dodrfid.org/supplierguide.htm). </p>
 * <p>Note that the DoD Guide explicitly recognises the value of cross-branch, globally applicable standards, advising that "suppliers that are EPCglobal subscribers and possess a unique [GS1] Company Prefix may use any of the identity types and encoding instructions described in the EPC™ Tag Data Standards document to encode tags."</p>
 */

public final class Usdod96 {
    private final static byte epcHeader = 0b00101111;
    private final static byte serialSize = 36;
    private final static byte governmentManagedIdentifierSize = 6;
    private static final String uriHeader = "urn:epc:tag:usdod-96:";

    private UsdodFilter filter;
    private String governmentManagedIdentifier;
    private long serial;

    private String epc = null;
    private String uri = null;

    private Usdod96(int filter,
                    String governmentManagedIdentifier,
                    long serial){
        this.filter = UsdodFilter.values()[filter];
        if (governmentManagedIdentifier.length() != governmentManagedIdentifierSize)
            throw new IllegalArgumentException("CAGE string must be "+governmentManagedIdentifierSize + " characters long");
        this.governmentManagedIdentifier = governmentManagedIdentifier;
        for (char ch : governmentManagedIdentifier.toCharArray())
            getCageCodeByte(ch); //Will throw exception if there is a problem
        if (serial>= 1l<<serialSize)
            throw new IllegalArgumentException("Serial too big (max, exclusive: "+serialSize+" bits)");
        this.serial = serial;
    }

    private static byte getCageCodeByte(char cageChar){
        if ((cageChar >= 'A' && cageChar <= 'Z' && cageChar != 'I' && cageChar != 'O') || cageChar == ' ')
            return (byte) cageChar;
        if (cageChar >= '0' && cageChar <= '9')
            return (byte) (cageChar | 0b00110000);
        throw new IllegalArgumentException("Invalid CAGE code character"); // [a-zIO] & the rest of possible chars
    }

    private static char getCageCodeChar(byte cageByte){
        if (cageByte>=0b00110000 && cageByte<=0b00111001)
            return (char)(cageByte & 0b00001111);
        return (char) cageByte;
    }

    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(96); 
            int i = 0;

            for (int j = 0; j < serialSize; j++,i++)
                epc.set(i, ((serial >> j) & 1)==1);

            i = 96 - (8 + 4 + 1);
            for (int t : (" " + governmentManagedIdentifier).chars().map(c -> getCageCodeByte((char) c)).toArray()){
                byte b = (byte) t;
                for (int j = 7; j >= 0; j--,i--)
                    epc.set(i, ((b >> j) & 1) == 1);
            }
            i = 96 - (8 + 4);
            for (int j = 0; j < 4; j++,i++)
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
    private void setEpc(String epc){
        this.epc = epc;
    }

    private void setUri(String uri){
        this.uri = uri;
    }

    public String getUri() {
        if (uri == null)
            uri = uriHeader + filter.getValue() + "." + governmentManagedIdentifier + "." + String.valueOf(serial);

        return uri;
    }

    @Override
    public String toString(){
        return getUri();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Usdod96))
            return false;
        return ((Usdod96) o).getUri().equals(getUri());
    }

    public String getGovernmentManagedIdentifier() {
        return governmentManagedIdentifier;
    }
    public long getSerial() {
        return serial;
    }

    public static Usdod96 fromFields(int filter,
                                     String governmentManagedIdentifier,
                                     long serial){
        return new Usdod96(filter, governmentManagedIdentifier, serial);
    }

    public static Usdod96 fromUri(String uri){
        if (!uri.startsWith(uriHeader))
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        String governmentManagedIdentifier = uriParts[1];
        long serial = Long.parseLong(uriParts[2]);
        Usdod96 usdod96 = fromFields(filter, governmentManagedIdentifier, serial);
        usdod96.setUri(uri);
        return usdod96;
    }

    public static Usdod96 fromEpc(String epc){
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

        for(tmp = 0, i = 96 - 8; (i = bs.previousSetBit(i-1)) > 96 - 8 - 4 - 1;)
            tmp+=1L<<(i-(96-8-4));
        int filter = (int) tmp;

        StringBuilder cageBuilder = new StringBuilder("");
        byte[] tmpba;

        i=96-8-4;
        if (bs.get(i-8,i).toByteArray()[0] != 32) // the encoded CAGE starts with a ' ' 
            throw new IllegalArgumentException("CAGE code incorrectly encoded");
        i-=8;    
        for(int j = 0;j < governmentManagedIdentifierSize && (tmpba = bs.get(i-8,i).toByteArray()).length!=0;i-=8,j++)
            cageBuilder.append(getCageCodeChar(tmpba[0]));

        String governmentManagedIdentifier = cageBuilder.toString(); // encoded CAGE code starts with ' '

        for(tmp = 0, i = serialSize; (i = bs.previousSetBit(i-1)) > -1;)
            tmp+=1L<<i;
        long serial = tmp;
        
        try{
            Usdod96 usdod96 = new Usdod96(filter, governmentManagedIdentifier, serial);
            usdod96.setEpc(epc);
            return usdod96;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }

    enum UsdodFilter {
        pallet_0(0x0),
        case_1(0x1),
        unit_pack_2(0x2),
        reserved_3(0x3),
        reserved_4(0x4),
        reserved_5(0x5),
        reserved_6(0x6),
        reserved_7(0x7),
        reserved_8(0x8),
        reserved_9(0x9),
        reserved_a(0xa),
        reserved_b(0xb),
        reserved_c(0xc),
        reserved_d(0xd),
        reserved_e(0xe),
        reserved_f(0xf);

        private int value;

        UsdodFilter(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
