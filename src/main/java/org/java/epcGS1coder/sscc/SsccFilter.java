package org.java.epcGS1coder.sscc;

public enum SsccFilter {
    all_others_0(0),
    reserved_1(1),
    case_2(2),
    reserved_3(3),
    reserved_4(4),
    reserved_5(5),
    unit_load_6(6),
    reserved_7(7);

    private int value;

    SsccFilter(int value) {
        this.value = value;
    }
    public int getValue(){
        return value;
    }
}
