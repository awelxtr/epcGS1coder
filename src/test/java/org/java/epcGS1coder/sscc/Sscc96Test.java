package org.java.epcGS1coder.sscc;

import org.junit.Assert;
import org.junit.Test;

public class Sscc96Test {

    @Test
    public void fromEpc(){
        Sscc96 sscc96 = Sscc96.fromEpc("31D48F2B3871528987000000");
        Assert.assertEquals(6, sscc96.getFilter());
        Assert.assertEquals(2345678l, sscc96.getCompanyPrefix());
        Assert.assertEquals(1901234567l, sscc96.getSerialReference());
    }

    @Test
    public void fromUri(){
        String uri = "urn:epc:tag:sscc-96:6.2345678.1901234567";
        Assert.assertEquals("31D48F2B3871528987000000",Sscc96.fromUri(uri).getEpc().toUpperCase());
    }

    @Test
    public void getUri(){
        Assert.assertEquals("urn:epc:tag:sscc-96:6.245678.09012034567",Sscc96.fromEpc("31D8EFEB821928BC07000000").getUri());
    }

    @Test
    public void fromFields(){
        Assert.assertEquals(Sscc96.fromFields(2,11,25697001250l,236970l).getEpc(),"3144BF7523E4439DAA000000");
    }

    @Test
    public void fromKey(){
        Assert.assertEquals(Sscc96.fromGs1Key(2,7,"003456789012345678").getEpc(),"315415193835B7BF87000000");
    }
}
