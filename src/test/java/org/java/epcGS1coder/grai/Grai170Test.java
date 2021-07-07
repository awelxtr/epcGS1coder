package org.java.epcGS1coder.grai;

import org.junit.Assert;
import org.junit.Test;

public class Grai170Test {
    @Test
    public void fromEpcTest(){
        Grai170 grai170 = Grai170.fromEpc("37140E511C56ED3461D9B37A17E00000000000000000");
        Assert.assertEquals(0, grai170.getFilter());
        Assert.assertEquals(234567, grai170.getCompanyPrefix());
        Assert.assertEquals(89012, grai170.getAssetType());
        Assert.assertEquals("hallo!?", grai170.getSerial());
        Assert.assertEquals("urn:epc:tag:grai-170:0.0234567.89012.hallo!%3F", grai170.getUri());
    }

    @Test
    public void fromUriTest(){
        String uri = "urn:epc:tag:grai-170:0.0234567.89012.hallo!%3F";
        Assert.assertEquals("37140E511C56ED3461D9B37A17E00000000000000000", Grai170.fromUri(uri).getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("37140E511C56ED3461D9B37A17E00000000000000000", Grai170.fromFields(0, 7, 234567, 89012, "hallo!?").getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("37140E511C56ED3461D9B37A17E00000000000000000", Grai170.fromGs1Key(0, 7, "0234567890129hallo!?").getEpc().toUpperCase());
    }
}
