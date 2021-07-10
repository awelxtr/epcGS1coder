package org.java.epcGS1coder.giai;

import org.junit.Assert;
import org.junit.Test;

public class Giai202Test {

    @Test
    public void fromEpcTest(){
        Giai202 giai202 = Giai202.fromEpc("381816E81B356AD5B80000000000000000000000000000000000");
        Assert.assertEquals(0, giai202.getFilter());
        Assert.assertEquals(23456, giai202.getCompanyPrefix());
        Assert.assertEquals("65557", giai202.getIndividualAssetReference());
        Assert.assertEquals("urn:epc:tag:giai-202:0.023456.65557", giai202.getUri());
    }

    @Test
    public void fromUriTest(){
        String uri = "urn:epc:tag:giai-202:0.023456.65557%22hola%22";
        Assert.assertEquals("381816E81B356AD5BA2D1BF66144000000000000000000000000", Giai202.fromUri(uri).getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("381816E81B356AD5B80000000000000000000000000000000000", Giai202.fromFields(0, 6, 23456, "65557").getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("381816E81B356AD5B80000000000000000000000000000000000", Giai202.fromGs1Key(0, 6, "02345665557").getEpc().toUpperCase());
    }
    
}
