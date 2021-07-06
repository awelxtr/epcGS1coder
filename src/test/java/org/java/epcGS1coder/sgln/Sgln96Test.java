package org.java.epcGS1coder.sgln;

import org.junit.Assert;
import org.junit.Test;

public class Sgln96Test {
    
    @Test
    public void fromEpcTest(){
        Sgln96 sgln96 = Sgln96.fromEpc("32140008DEB7680000000001");
        Assert.assertEquals(0, sgln96.getFilter());
        Assert.assertEquals(567, sgln96.getCompanyPrefix());
        Assert.assertEquals(89012, sgln96.getLocationReference());
        Assert.assertEquals(1, sgln96.getExtension());
        Assert.assertEquals("urn:epc:tag:sgln-96:0.0000567.89012.1", sgln96.getUri());
    }

    @Test
    public void fromUriTest(){
        String uri = "urn:epc:tag:sgln-96:0.0000567.89012.1";
        Assert.assertEquals("32140008DEB7680000000001", Sgln96.fromUri(uri).getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("32140008DEB7680000000001", Sgln96.fromFields(0, 7, 567, 89012, 1).getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("32140008DEB7680000000001", Sgln96.fromGs1Key(0, 7, "0000567890123", 1).getEpc().toUpperCase());
    }
}
