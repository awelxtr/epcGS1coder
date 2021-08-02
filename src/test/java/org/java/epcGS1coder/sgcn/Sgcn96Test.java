package org.java.epcGS1coder.sgcn;

import org.junit.Assert;
import org.junit.Test;

public class Sgcn96Test {
    
    @Test
    public void fromEpcTest(){
        Sgcn96 sgcn96 = Sgcn96.fromEpc("3F28499602D218000098EA52");
        Assert.assertEquals(1, sgcn96.getFilter());
        Assert.assertEquals(1234567890, sgcn96.getCompanyPrefix());
        Assert.assertEquals(12, sgcn96.getCouponReference());
        Assert.assertEquals("0021458", sgcn96.getSerial());
        Assert.assertEquals("urn:epc:tag:sgcn-96:1.1234567890.12.0021458", sgcn96.getUri());
    }

    @Test
    public void fromUriTest(){
        Assert.assertEquals("3F28499602D218000098EA52", Sgcn96.fromUri("urn:epc:tag:sgcn-96:1.1234567890.12.0021458").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("3F28499602D218000098EA52", Sgcn96.fromFields(1, 10, 1234567890, 12, "0021458").getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("3F28499602D218000098EA52", Sgcn96.fromGs1Key(1, 10, "12345678901280021458").getEpc().toUpperCase());
    }
}
