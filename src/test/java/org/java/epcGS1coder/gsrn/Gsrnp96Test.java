package org.java.epcGS1coder.gsrn;

import org.junit.Assert;
import org.junit.Test;

public class Gsrnp96Test {

    @Test
    public void fromEpc(){
        String epc = "2E1602CE1E128E0F87000000";
        Gsrnp96 gsrnp96 = Gsrnp96.fromEpc(epc);
        Assert.assertEquals(0,gsrnp96.getFilter());
        Assert.assertEquals(8434567,gsrnp96.getCompanyPrefix());
        Assert.assertEquals(8901234567l,gsrnp96.getServiceReference());
        Assert.assertEquals("urn:epc:tag:gsrnp-96:0.8434567.8901234567",gsrnp96.getUri());
    }

    @Test
    public void fromUri(){
        Assert.assertEquals("2E1602CE1E128E0F87000000",Gsrnp96.fromUri("urn:epc:tag:gsrnp-96:0.8434567.8901234567").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("2E1602CE1E128E0F87000000",Gsrnp96.fromFields(0, 7,8434567,8901234567l).getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("2E1602CE1E128E0F87000000",Gsrnp96.fromGs1Key(0,7,"843456789012345678").getEpc().toUpperCase());
    }
}
