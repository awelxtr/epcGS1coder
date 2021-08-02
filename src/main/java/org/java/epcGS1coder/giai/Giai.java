package org.java.epcGS1coder.giai;

class Giai {

    protected enum GiaiFilter{
        all_others_0(0),
        rail_vehicle_1(1),
        reserved_2(2),
        reserved_3(3),
        reserved_4(4),
        reserved_5(5),
        reserved_6(6),
        reserved_7(7);

        protected int value;

        GiaiFilter(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
