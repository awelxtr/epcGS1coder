package org.java.epcGS1coder.cpi;

import org.junit.Assert;
import org.junit.Test;

public class Cpi96Test {
    
    @Test
    public void fromEpc(){
        String epc = "3C144B5A1C0000040000000C";
        Cpi96 cpi96 = Cpi96.fromEpc(epc);
        Assert.assertEquals(0,cpi96.getFilter());
        Assert.assertEquals(1234567,cpi96.getCompanyPrefix());
        Assert.assertEquals(8,cpi96.getComponentPartReference());
        Assert.assertEquals(12l,cpi96.getSerial());
        Assert.assertEquals("urn:epc:tag:cpi-96:0.1234567.8.12", cpi96.getUri());
    }

    @Test
    public void fromUri(){
        Assert.assertEquals("3C144B5A1C0000040000000C",Cpi96.fromUri("urn:epc:tag:cpi-96:0.1234567.8.12").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("3C144B5A1C0000040000000C",Cpi96.fromFields(0,7,1234567,8,12l).getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("3C144B5A1C0000040000000C",Cpi96.fromGs1Key(0,7,"12345678",12l).getEpc().toUpperCase());
    }

}
