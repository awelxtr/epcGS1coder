package org.java.epcGS1coder.sgtin;

public enum SgtinFilter {
	all_others_0(0),
	pos_item_1(1),
	case_2(2),
	inner_pack_4(4),
	reserved_3(3),
	reserved_5(5),
	unit_load_6(6),
	component_7(7);
	
	private int value;
	
	SgtinFilter(int value) {
		this.value = value;
	}
	public int getValue(){
		return value;
	}
}
