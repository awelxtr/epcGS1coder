package org.java.epcGS1coder.grai;

import org.junit.Assert;
import org.junit.Test;

public class Grai96Test {

    @Test
    public void fromEpcTest(){
        Grai96 grai96 = Grai96.fromEpc("33140E511C56ED0000000001");
        Assert.assertEquals(0, grai96.getFilter());
        Assert.assertEquals(234567, grai96.getCompanyPrefix());
        Assert.assertEquals(89012, grai96.getAssetType());
        Assert.assertEquals(1, grai96.getSerial());
        Assert.assertEquals("urn:epc:tag:grai-96:0.0234567.89012.1", grai96.getUri());
    }

    @Test
    public void fromUriTest(){
        String uri = "urn:epc:tag:grai-96:0.0234567.89012.1";
        Assert.assertEquals("33140E511C56ED0000000001", Grai96.fromUri(uri).getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("33140E511C56ED0000000001", Grai96.fromFields(0, 7, 234567, 89012, 1).getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("33140E511C56ED0000000001", Grai96.fromGs1Key(0, 7, "02345678901231").getEpc().toUpperCase());
    }
    
}
