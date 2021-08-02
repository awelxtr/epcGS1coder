package org.java.epcGS1coder.cpi;

class Cpi {
    protected enum CpiFilter{
        all_others_0(0),
        reserved_1(1),
        reserved_2(2),
        reserved_3(3),
        reserved_4(4),
        reserved_5(5),
        reserved_6(6),
        reserved_7(7);

        protected int value;

        CpiFilter(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
