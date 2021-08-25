package org.java.epcGS1coder.usdod;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

public class Usdod96 {
    private final static byte epcHeader = 0b00101111;
	private final static byte serialSize = 36;
    private final static byte governmentManagedIdentifierSize = 6;
	private static final String uriHeader = "urn:epc:tag:usdod-96:";

    private UsdodFilter filter;
    private String governmentManagedIdentifier;
    private long serial;

    private String epc = null;
    private String uri = null;

    Usdod96(int filter,
            String governmentManagedIdentifier,
            long serial){
        this.filter = UsdodFilter.values()[filter];
        this.governmentManagedIdentifier = governmentManagedIdentifier;
        if (serial>= 1l<<serialSize)
            throw new RuntimeException("Serial too big (max: "+serialSize+" bits)");
        this.serial = serial;
    }

    private static byte getCageCodeByte(char cageChar){
        if ((cageChar >= 'A' && cageChar <= 'Z' && cageChar != 'I' && cageChar != 'O') || cageChar == ' ')
            return (byte) cageChar;
        if (cageChar >= '0' && cageChar <= '9')
            return (byte) (cageChar | 0b00110000);
        throw new RuntimeException("Invalid CAGE code character"); // [a-zIO] & the rest of possible chars
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
    void setEpc(String epc){
        this.epc = epc;
    }

    public String getUri() {
        if (uri == null)
            uri = uriHeader + filter.getValue() + "." + governmentManagedIdentifier + "." + String.valueOf(serial);

        return uri;
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
			throw new RuntimeException("Decoding error: wrong URI header, expected " + uriHeader);

		String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        String governmentManagedIdentifier = uriParts[1];
        long serial = Long.parseLong(uriParts[2]);

        return fromFields(filter, governmentManagedIdentifier, serial);
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
			throw new RuntimeException("Invalid header"); //maybe the decoder could choose the structure from the header?

        for(tmp = 0, i = 96 - 8; (i = bs.previousSetBit(i-1)) > 96 - 8 - 4 - 1;)
			tmp+=1L<<(i-(96-8-4));
		int filter = (int) tmp;

        StringBuilder cageBuilder = new StringBuilder("");
		byte[] tmpba;

        i=96-8-4;
        if (bs.get(i-8,i).toByteArray()[0] != 32) // the encoded CAGE starts with a ' ' 
            throw new RuntimeException("CAGE code incorrectly encoded");
        i-=8;    
        for(int j = 0;j < governmentManagedIdentifierSize && (tmpba = bs.get(i-8,i).toByteArray()).length!=0;i-=8,j++)
			cageBuilder.append(getCageCodeChar(tmpba[0]));

        String governmentManagedIdentifier = cageBuilder.toString(); // encoded CAGE code starts with ' '

        for(tmp = 0, i = serialSize; (i = bs.previousSetBit(i-1)) > -1;)
			tmp+=1L<<i;
		long serial = tmp;

        Usdod96 usdod96 = new Usdod96(filter, governmentManagedIdentifier, serial);
        usdod96.setEpc(epc);
        return usdod96;
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
