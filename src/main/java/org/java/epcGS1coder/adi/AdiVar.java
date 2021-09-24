package org.java.epcGS1coder.adi;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.stream.Collectors;

/**
 * <p>The variable-length Aerospace and Defense EPC identifier is designed for use by the aerospace and  defense sector for the unique identification of parts or items. The existing unique identifier  constructs are defined in the Air Transport Association (ATA) Spec 2000 standard [SPEC2000], and  the US Department of Defense Guide to Uniquely Identifying items [UID]. The ADI EPC construct  provides a mechanism to directly encode such unique identifiers in RFID tags and to use the URI  representations at other layers of the EPCglobal architecture. </p>
 * <p>Within the Aerospace & Defense sector identification constructs supported by the ADI EPC,  companies are uniquely identified by their Commercial And Government Entity (CAGE) code or by  their Department of Defense Activity Address Code (DODAAC). The NATO CAGE (NCAGE) code is  issued by NATO / Allied Committee 135 and is structurally equivalent to a CAGE code (five character  uppercase alphanumeric excluding capital letters I and O) and is non-colliding with CAGE codes  issued by the US Defense Logistics Information Service (DLIS). Note that in the remainder of this  section, all references to CAGE apply equally to NCAGE. </p>
 * <p>ATA Spec 2000 defines that a unique identifier may be constructed through the combination of the  CAGE code or DODAAC together with either: 
 * <ul>
 *  <li> A serial number (SER) that is assigned uniquely within the CAGE code or DODAAC; or </li>
 *  <li> An original part number (PNO) that is unique within the CAGE code or DODAAC and a sequential  serial number (SEQ) that is uniquely assigned within that original part number. </li>
 * </ul>
 * The US DoD Guide to Uniquely Identifying Items defines a number of acceptable methods for  constructing unique item identifiers (UIIs). The UIIs that can be represented using the Aerospace  and Defense EPC identifier are those that are constructed through the combination of a CAGE code  or DODAAC together with either: 
 * <ul>
 *  <li> a serial number that is unique within the enterprise identifier. (UII Construct #1) </li>
 *  <li> an original part number and a serial number that is unique within the original part number (a  subset of UII Construct #2) </li>
 * </ul>
 * <p>Note that the US DoD UID guidelines recognise a number of unique identifiers based on GS1  identifier keys as being valid UIDs. In particular, the SGTIN (GTIN + Serial Number), GIAI, and  GRAI with full serialisation are recognised as valid UIDs. These may be represented in EPC form  using the SGTIN, GIAI, and GRAI EPC schemes as specified in Sections 6.3.1, 6.3.5, and 6.3.4,  respectively; the ADI EPC scheme is not used for this purpose. Conversely, the US DoD UID  guidelines also recognise a wide range of enterprise identifiers issued by various issuing agencies  other than those described above; such UIDs do not have a corresponding EPC representation. </p>
 * <p>For purposes of identification via RFID of those aircraft parts that are traditionally not serialised or  not required to be serialised for other purposes, the ADI EPC scheme may be used for assigning a  unique identifier to a part. In this situation, the first character of the serial number component of  the ADI EPC SHALL be a single '#' character. This is used to indicate that the serial number does not  correspond to the serial number of a traditionally serialised part because the '#' character is not  permitted to appear within the values associated with either the SER or SEQ text element identifiers  in ATA Spec 2000 standard. </p>
 * <p>For parts that are traditionally serialised / required to be serialised for purposes other than having a  unique RFID identifier, and for all usage within US DoD UID guidelines, the '#' character SHALL NOT  appear within the serial number element. </p>
 * <p>The ATA Spec 2000 standard recommends that companies serialise uniquely within their CAGE code.  For companies who do serialise uniquely within their CAGE code or DODAAC, a zero-length string  SHALL be used in place of the Original Part Number element when constructing an EPC.</p>
 */

public final class AdiVar {
    private final static byte epcHeader = 0b00111011;
    private final static byte cageSize = 5;
    private static final String uriHeader = "urn:epc:tag:adi-var:";

    private AdiFilter filter;
    private String cage;
    private String partNumber;
    private String serial;

    private String epc = null;
    private String uri = null;

    private AdiVar(int filter,
            String cage,
            String partNumber,
            String serial){
        this.filter = AdiFilter.values()[filter];
        if (cage.length() != cageSize)
            throw new IllegalArgumentException("CAGE code must be 5 characters long");
        for (char ch : cage.toCharArray())
            getCageCodeByte(ch); // Will throw an exception if the CAGE character is invalid.
        this.cage = cage;
        if (partNumber.length() > 32)
            throw new IllegalArgumentException("Part number must be between 0 and 32 characters long");
        for (char ch : partNumber.toCharArray())
            if (!((ch>='0' && ch <='9') || (ch>='A' && ch<='Z') || ch == '-' || ch == '/')) //Table G-1 (part number can't contain '#'')
                throw new IllegalArgumentException("Port number contains invalid character: " + ch);
        this.partNumber = partNumber;
        if (serial.length() == 0 || serial.length() > 30)
            throw new IllegalArgumentException("Serial number must be between 1 and 30 characters long");
        for (char ch : serial.toCharArray())
            if (!((ch>='0' && ch <='9') || (ch>='A' && ch<='Z') || ch == '#' || ch == '-' || ch == '/')) //Table G-1
                throw new IllegalArgumentException("Serial contains invalid character: " + ch);
        if (serial.indexOf('#') > 0)
            throw new IllegalArgumentException("'#' can only appear at the beggining of the serial");
        this.serial = serial;
    }

    private static byte getCageCodeByte(char cageChar){
        if ((cageChar >= 'A' && cageChar <= 'Z' && cageChar != 'I' && cageChar != 'O') || cageChar == ' ')
            return (byte) (cageChar & 0b111111);
        if (cageChar >= '0' && cageChar <= '9')
            return (byte) cageChar;
        throw new IllegalArgumentException("Invalid CAGE code character"); // [a-zIO] & the rest of possible chars
    }

    private static char getCageCodeChar(byte cageByte){
        if (cageByte >= 0b00110000 && cageByte <= 0b00111001) // [0-9] (0x30 <-> 0x39)
            return (char) cageByte;
        return (char) (cageByte|0b01000000);
    }

    @Override
    public String toString(){
        return getUri();
    }

    public String getEpc() {
        if (epc == null){
            int epcBitSize = 8+6+36+(partNumber.length()+1+serial.length()+1)*6;
            epcBitSize+=epcBitSize%8;
            
            BitSet epc = new BitSet(); 
            for (int j = 0,i=epcBitSize-8; j < 8; j++,i++)
                epc.set(i, ((epcHeader >> j) & 1)==1);
            
            for (int j = 0,i=epcBitSize-(8+6); j < 6; j++,i++)
                epc.set(i, ((filter.getValue() >> j) & 1)==1);

            int i = epcBitSize-(8+6+1);

            for (int t : (" " + cage).chars().map(c -> getCageCodeByte((char) c)).toArray()){
                byte b = (byte) t;
                for (int j = 5; j >= 0; j--,i--)
                    epc.set(i, ((b >> j) & 1) == 1);
            }

            for(int t = 0; t<partNumber.length();t++){
                byte b = partNumber.getBytes()[t];
                for (int j = 5; j >= 0; j--,i--)
                    epc.set(i, ((b >> j) & 1) == 1);
            }

            for (int j = 0;j<6;i--,j++)
                epc.clear(i);

            for(int t = 0; t<serial.length();t++){
                byte b = serial.getBytes()[t];
                for (int j = 5; j >= 0; j--,i--)
                    epc.set(i, ((b >> j) & 1) == 1);
            }

            for (int j = 0;j<6;i--,j++) 
                epc.clear(i);
            
            byte[] epcba = epc.toByteArray();
            StringBuffer sb = new StringBuffer(epcba.length*2);
            for (i = epcba.length - 1; i>=0; i--)
                sb.append(String.format("%02X",epcba[i]));

            this.epc = sb.toString().substring(0,(int) Math.ceil((8+6+36+(partNumber.length()+1+serial.length()+1)*6)/4f)); //this op is to get the minimum size epc
        }
        return epc;
    }
    private void setEpc(String epc){
        this.epc = epc;
    }

    public String getUri() {
        if (uri == null)
            uri = uriHeader + filter.getValue() + "." + cage + "." + partNumber + "." + serial.chars().mapToObj(c -> getUriSerialChar((char) c)).collect(Collectors.joining());
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
            throw new IllegalArgumentException("Decoding error: wrong URI header, expected " + uriHeader);

        String uriParts[] = uri.substring(uriHeader.length()).split("\\.");
        int filter = Integer.parseInt(uriParts[0]);
        String cage = uriParts[1];
        String partNumber = uriParts[2];
        String serial = uriParts[3];
        StringBuilder sb = new StringBuilder();
        String[] serialSplit = serial.split("%");
        sb.append(serialSplit[0]);
        for (int i = 1; i < serialSplit.length; i++){
            sb.append((char) Integer.parseInt(serialSplit[i].substring(0,2),16));
            sb.append(serialSplit[i].substring(2));
        }

        return fromFields(filter, cage, partNumber, sb.toString());
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
            throw new IllegalArgumentException("Invalid header"); //maybe the decoder could choose the structure from the header?

        for(tmp = 0, i = epcBitSize - 8; (i = bs.previousSetBit(i-1)) > epcBitSize - 8 - 6 - 1;)
            tmp+=1L<<(i-(epcBitSize-8-6));
        int filter = (int) tmp;

        byte[] tmpba;

        StringBuilder cageBuilder = new StringBuilder("");
        i=epcBitSize-8-6;
        if (bs.get(i-6,i).toByteArray()[0] != 32) // the encoded CAGE starts with a ' ' 
            throw new IllegalArgumentException("CAGE code incorrectly encoded");
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

        try{
            AdiVar adiVar = new AdiVar(filter, cage, partNumber, serial);
            adiVar.setEpc(epc);
            return adiVar;
        } catch (RuntimeException e){
            throw new IllegalArgumentException("Invalid EPC: " + e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof AdiVar))
            return false;
        return ((AdiVar) o).getUri().equals(getUri());
    }

    /**
     * Table G-1 for the encoding
     */
    private String getUriSerialChar(char ch){
        if (!((ch>='0' && ch <='9') || (ch>='A' && ch<='Z') || ch == '-' || ch == '/'))
            throw new IllegalArgumentException("Wrong char");
        if (ch == '#')
            return "%23";
        else 
            return String.valueOf(ch);
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
