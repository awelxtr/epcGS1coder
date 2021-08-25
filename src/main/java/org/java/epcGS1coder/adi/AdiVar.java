package org.java.epcGS1coder.adi;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

public class AdiVar {
    private final static byte epcHeader = 0b00111011;
    private final static byte cageSize = 5;
	private static final String uriHeader = "urn:epc:tag:adi-var:";

    private AdiFilter filter;
    private String cage;
    private String partNumber;
    private String serial;

    private String epc = null;
    private String uri = null;

    AdiVar(int filter,
            String cage,
            String partNumber,
            String serial){
        this.filter = AdiFilter.values()[filter];
        this.cage = cage;
        this.partNumber = partNumber;
        this.serial = serial;
    }

    private static byte getCageCodeByte(char cageChar){
        if ((cageChar >= 'A' && cageChar <= 'Z' && cageChar != 'I' && cageChar != 'O') || cageChar == ' ')
            return (byte) (cageChar & 0b111111);
        if (cageChar >= '0' && cageChar <= '9')
            return (byte) cageChar;
        throw new RuntimeException("Invalid CAGE code character"); // [a-zIO] & the rest of possible chars
    }

    private static char getCageCodeChar(byte cageByte){
        if (cageByte>=0b00110000 && cageByte<=0b00111001)
            return (char)cageByte;
        return (char) (cageByte|0b01000000);
    }
   
    public String getEpc() {
        if (epc == null){
            BitSet epc = new BitSet(434); 

            int i = 434 - (8 + 6 + 1);
            for (int t : (" " + cage).chars().map(c -> getCageCodeByte((char) c)).toArray()){
                byte b = (byte) t;
                for (int j = 7; j >= 0; j--,i--)
					epc.set(i, ((b >> j) & 1) == 1);
            }

            for (byte b : partNumber.getBytes()) {
				for (int j = 6; j >= 0; j--,i--)
					epc.set(i, ((b >> j) & 1) == 1);
			}
            i+=6;

            for (byte b : serial.getBytes()) {
				for (int j = 6; j >= 0; j--,i--)
					epc.set(i, ((b >> j) & 1) == 1);
			}

            i = 434 - (8 + 6);
            for (int j = 0; j < 6; j++,i++)
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
            uri = uriHeader + filter.getValue() + "." + cage + "." + partNumber + "." + String.valueOf(serial);

        return uri;
    }

    public String getCage() {
        return cage;
    }
    public String getPartNumber() {
        return partNumber;
    }
    public String getSerial() {
        return serial;
    }

    public static AdiVar fromFields(int filter,
                                     String cage,
                                     String partNumber,
                                     String serial){
        return new AdiVar(filter, cage, partNumber,serial);
    }

    public static AdiVar fromUri(String uri){
        if (!uri.startsWith(uriHeader))
			throw new RuntimeException("Decoding error: wrong URI header, expected " + uriHeader);

		String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        String cage = uriParts[1];
        String partNumber = uriParts[2];
        String serial = uriParts[3];

        return fromFields(filter, cage, partNumber, serial);
    }

    public static AdiVar fromEpc(String epc){
        if (epc.length()%2 !=0)
            epc+="0";
        ArrayList<String> a = new ArrayList<String>();
		for (int i = 0; i<epc.length(); i+=2) {
			a.add(epc.substring(i, i+2));
		}
        int epcBitSize= ((epc.length()*4)/8 + ((epc.length()*4)%8))*8;
		ByteBuffer bb = ByteBuffer.allocate(epcBitSize);
		for (int i = a.size() - 1; i>=0;i--)
			bb.put((byte) Integer.parseInt(a.get(i),16));
		bb.rewind();

		BitSet bs = BitSet.valueOf(bb);

		int i;
		long tmp;

		for(tmp = 0, i = epcBitSize; (i = bs.previousSetBit(i-1)) > epcBitSize - 8 - 1;)
			tmp+=1L<<(i-(epcBitSize-8));
		if (tmp != epcHeader)
			throw new RuntimeException("Invalid header"); //maybe the decoder could choose the structure from the header?

        for(tmp = 0, i = epcBitSize - 8; (i = bs.previousSetBit(i-1)) > epcBitSize - 8 - 6 - 1;)
			tmp+=1L<<(i-(epcBitSize-8-6));
		int filter = (int) tmp;

        byte[] tmpba;

        StringBuilder cageBuilder = new StringBuilder("");
		i=epcBitSize-8-6;
        if (bs.get(i-6,i).toByteArray()[0] != 32) // the encoded CAGE starts with a ' ' 
            throw new RuntimeException("CAGE code incorrectly encoded");
        i-=6;  
        for(int j = 0;j < cageSize && (tmpba = bs.get(i-6,i).toByteArray()).length!=0;i-=6,j++)
			cageBuilder.append(getCageCodeChar(tmpba[0]));
        String cage = cageBuilder.toString();

        StringBuilder partNumberBuilder = new StringBuilder();
		for(;(tmpba = bs.get(i-6,i).toByteArray()).length!=0;i-=6){
            if (tmpba[0]>=0b000001 && tmpba[0] <= 0b011010) // Encoded [A-Z] => Table G-1 Characters Permitted in 6-bit Alphanumeric Fields
				tmpba[0]|=0b01000000;	
            partNumberBuilder.append(new String(tmpba));	
        }
		String partNumber = partNumberBuilder.toString();

        StringBuilder serialBuilder = new StringBuilder();
		for(i-=6;(tmpba = bs.get(i-6,i).toByteArray()).length!=0;i-=6){
            if (tmpba[0]>=0b000001 && tmpba[0] <= 0b011010) // Encoded [A-Z] => Table G-1 Characters Permitted in 6-bit Alphanumeric Fields
				tmpba[0]|=0b01000000;
			serialBuilder.append(new String(tmpba));
        }
		String serial = serialBuilder.toString();

        AdiVar adiVar = new AdiVar(filter, cage, partNumber, serial);
        adiVar.setEpc(epc);
        return adiVar;
    }

    enum AdiFilter {
        all_others(0),
        item_1(1),
        carton_2(2),
        reserved_3(3),
        reserved_4(4),
        reserved_5(5),
        pallet_6(6),
        reserved_7(7),
        seat_cushion_8(8),
        seat_cover_9(9),
        seat_belt_10(10),
        galley_car_11(11),
        ULD_12(12),
        security_item_13(13),
        life_vests_14(14),
        oxygen_generator_15(15),
        engine_component_16(15),
        avionics_17(17),
        flight_test_equip_18(18),
        other_emergency_equip_19(19),
        other_rotables_20(20),
        other_repairable_21(21),
        other_cabin_interior_22(22),
        other_repair__23(23),
        passenger_seat_24(24),
        ifes_25(25),
        reserved_26(26),
        reserved_27(27),
        reserved_28(28),
        reserved_29(29),
        reserved_30(30),
        reserved_31(31),
        reserved_32(32),
        reserved_33(33),
        reserved_34(34),
        reserved_35(35),
        reserved_36(36),
        reserved_37(37),
        reserved_38(38),
        reserved_39(39),
        reserved_40(40),
        reserved_41(41),
        reserved_42(42),
        reserved_43(43),
        reserved_44(44),
        reserved_45(45),
        reserved_46(46),
        reserved_47(47),
        reserved_48(48),
        reserved_49(49),
        reserved_50(50),
        reserved_51(51),
        reserved_52(52),
        reserved_53(53),
        reserved_54(54),
        reserved_55(55),
        location_identifier_56(56),
        documentation_57(57),
        tools_58(58),
        ground_support_equipment_59(59),
        other_nonflyable_equipment_60(60),
        reserved_61(61),
        reserved_62(62),
        reserved_63(63);

        private int value;

        AdiFilter(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
