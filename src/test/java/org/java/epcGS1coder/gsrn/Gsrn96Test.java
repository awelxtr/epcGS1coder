package org.java.epcGS1coder.gsrn;

import org.junit.Assert;
import org.junit.Test;

public class Gsrn96Test {

    @Test
    public void fromEpc(){
        String epc = "2D1602CE1E128E0F87000000";
        Gsrn96 gsrn96 = Gsrn96.fromEpc(epc);
        Assert.assertEquals(0,gsrn96.getFilter());
        Assert.assertEquals(8434567,gsrn96.getCompanyPrefix());
        Assert.assertEquals(8901234567l,gsrn96.getServiceReference());
        Assert.assertEquals("urn:epc:tag:gsrn-96:0.8434567.8901234567",gsrn96.getUri());
    }

    @Test
    public void fromUri(){
        Assert.assertEquals("2D1602CE1E128E0F87000000",Gsrn96.fromUri("urn:epc:tag:gsrn-96:0.8434567.8901234567").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("2D1602CE1E128E0F87000000",Gsrn96.fromFields(0, 7,8434567,8901234567l).getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("2D1602CE1E128E0F87000000",Gsrn96.fromGs1Key(0,7,"843456789012345678").getEpc().toUpperCase());
    }
}
