package org.java.epcGS1coder.giai;

import org.junit.Assert;
import org.junit.Test;

public class Giai96Test {
    @Test
    public void fromEpcTest(){
        Giai96 giai96 = Giai96.fromEpc("341816E80000000000010015");
        Assert.assertEquals(0, giai96.getFilter());
        Assert.assertEquals(23456, giai96.getCompanyPrefix());
        Assert.assertEquals(65557, giai96.getIndividualAssetReference());
        Assert.assertEquals("urn:epc:tag:giai-96:0.023456.65557", giai96.getUri());
    }

    @Test
    public void fromUriTest(){
        String uri = "urn:epc:tag:giai-96:0.023456.65557";
        Assert.assertEquals("341816E80000000000010015", Giai96.fromUri(uri).getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("341816E80000000000010015", Giai96.fromFields(0, 6, 23456, 65557).getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("341816E80000000000010015", Giai96.fromGs1Key(0, 6, "02345665557").getEpc().toUpperCase());
    }
}
