package org.java.epcGS1coder.itip;

import org.junit.Assert;
import org.junit.Test;

public class Itip212Test {
    @Test
    public void fromEpc(){
        String epc = "4114F4E4E40C0E4082DBDD8B36600000000000000000000000000000";
        Itip212 itip212 = Itip212.fromEpc(epc);
        Assert.assertEquals(0,itip212.getFilter());
        Assert.assertEquals(4012345, itip212.getCompanyPrefix());
        Assert.assertEquals(12345, itip212.getIndicatorPadDigitItemReference());
        Assert.assertEquals(01, itip212.getPiece());
        Assert.assertEquals(02, itip212.getTotal());  
        Assert.assertEquals("mw133", itip212.getSerial());
        Assert.assertEquals("urn:epc:tag:itip-212:0.4012345.012345.01.02.mw133", itip212.getUri());
    }

    @Test
    public void fromUri(){
        Assert.assertEquals("4114F4E4E40C0E4082DBDD8B36600000000000000000000000000000",Itip212.fromUri("urn:epc:tag:itip-212:0.4012345.012345.01.02.mw133").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("4114F4E4E40C0E4082DBDD8B36600000000000000000000000000000",Itip212.fromFields(0, 7,4012345,12345,(byte)1,(byte)2,"mw133").getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("4114F4E4E40C0E4082DBDD8B36600000000000000000000000000000",Itip212.fromGs1Key(0,7,"040123451234560102","mw133").getEpc().toUpperCase());
    }
}
