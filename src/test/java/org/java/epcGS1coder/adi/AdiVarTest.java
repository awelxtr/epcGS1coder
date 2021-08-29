package org.java.epcGS1coder.adi;

import org.junit.Assert;
import org.junit.Test;

public class AdiVarTest {
    @Test
    public void fromEpc(){
        String epc = "3B0E0CF5E76C9047759AD00373DC7602E7200";
        AdiVar adiVar = AdiVar.fromEpc(epc);
        Assert.assertEquals("35962", adiVar.getCage());
        Assert.assertEquals("PQ7VZ4", adiVar.getPartNumber());
        Assert.assertEquals("M37GXB92", adiVar.getSerial());
        Assert.assertEquals("urn:epc:tag:adi-var:3.35962.PQ7VZ4.M37GXB92", adiVar.getUri());
    }

    @Test
    public void fromUri(){
        Assert.assertEquals("3B0E0CF5E76C9047759AD00373DC7602E7200", AdiVar.fromUri("urn:epc:tag:adi-var:3.35962.PQ7VZ4.M37GXB92").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("3B0E0CF5E76C9047759AD00373DC7602E7200", AdiVar.fromFields(3,"35962","PQ7VZ4","M37GXB92").getEpc().toUpperCase());
    }

    @Test
    public void testLong(){
        long result = 0;
        int offset = 64;
        result+=(long)(0b00111011)<<(offset-=8);
        result+=(long)(3)<<(offset-=6);
        result+=(long)(32)<<(offset-=6);
        result+=(long)(51)<<(offset-=6);
        result+=(long)(53)<<(offset-=6);
        result+=(long)(57)<<(offset-=6);
        result+=(long)(54)<<(offset-=6);
        result+=(long)(50)<<(offset-=6);
        System.out.println(String.format("%X",result));
    }
}
