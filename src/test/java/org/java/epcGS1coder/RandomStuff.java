package org.java.epcGS1coder;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

public class RandomStuff {

	@Test
	public void toStringTest() {
		ByteBuffer bb = ByteBuffer.allocate(12); //303612345678901234567890
		bb.put((byte) 0x30);
		bb.put((byte) 0x36);
		bb.put((byte) 0x12);
		bb.put((byte) 0x34);
		bb.put((byte) 0x56);
		bb.put((byte) 0x78);
		bb.put((byte) 0x90);
		bb.put((byte) 0x12);
		bb.put((byte) 0x34);
		bb.put((byte) 0x56);
		bb.put((byte) 0x78);
		bb.put((byte) 0x90);
		bb.rewind();
		
		BitSet bs = BitSet.valueOf(bb);
		for (byte b : bs.toByteArray())
			System.out.print(String.format("%02X",b));
		System.out.println();
	}
	
	@Test
	public void toBitSet() {
		String epc = "303612345678901234567890";
		
		ArrayList<String> a = new ArrayList<String>();
		for (int i = 0; i<epc.length(); i+=2) {
			a.add(epc.substring(i, i+2));
		}
		
		ByteBuffer bb = ByteBuffer.allocate(12);
		a.stream().map(s -> Integer.parseInt(s, 16)).map(Integer::byteValue).forEach(bb::put);
		bb.rewind();
		
		BitSet bs = BitSet.valueOf(bb);
		for (byte b : bs.toByteArray())
			System.out.print(String.format("%02X",b));
		System.out.println();
		
	}
}
