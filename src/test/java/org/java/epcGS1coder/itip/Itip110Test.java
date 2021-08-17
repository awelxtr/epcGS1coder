package org.java.epcGS1coder.itip;

import org.junit.Assert;
import org.junit.Test;

public class Itip110Test {
    @Test
    public void fromEpc(){
        String epc = "4019D9DC81F4EEC0820000079134";
        Itip110 itip110 = Itip110.fromEpc(epc);
        Assert.assertEquals(0,itip110.getFilter());
        Assert.assertEquals(485234, itip110.getCompanyPrefix());
        Assert.assertEquals(512955, itip110.getIndicatorPadDigitItemReference());
        Assert.assertEquals(01, itip110.getPiece());
        Assert.assertEquals(02, itip110.getTotal());  
        Assert.assertEquals(123981l, itip110.getSerial());
        Assert.assertEquals("urn:epc:tag:itip-110:0.485234.0512955.01.02.123981", itip110.getUri());
    }

    @Test
    public void fromUri(){
        Assert.assertEquals("4019D9DC81F4EEC0820000079134",Itip110.fromUri("urn:epc:tag:itip-110:0.485234.0512955.01.02.123981").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("4019D9DC81F4EEC0820000079134",Itip110.fromFields(0, 6,485234,512955,(byte)1,(byte)2,123981l).getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("4019D9DC81F4EEC0820000079134",Itip110.fromGs1Key(0,6,"048523451295560102",123981l).getEpc().toUpperCase());
    }
}
