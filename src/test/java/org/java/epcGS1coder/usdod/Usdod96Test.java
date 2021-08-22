package org.java.epcGS1coder.usdod;

import org.junit.Assert;
import org.junit.Test;

public class Usdod96Test {
    @Test
    public void fromEpc(){
        String epc = "2F320434147455900000162E";
        Usdod96 usdod96 = Usdod96.fromEpc(epc);
        Assert.assertEquals("CAGEY", usdod96.getGovernmentManagedIdentifier());
        Assert.assertEquals(5678l, usdod96.getSerial());
        Assert.assertEquals("urn:epc:tag:usdod-96:3.CAGEY.5678", usdod96.getUri());
    }

    @Test
    public void fromUri(){
        Assert.assertEquals("2F320434147455900000162E", Usdod96.fromUri("urn:epc:tag:usdod-96:3.CAGEY.5678").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("2F320434147455900000162E", Usdod96.fromFields(3,"CAGEY",5678l).getEpc().toUpperCase());
    }

}
