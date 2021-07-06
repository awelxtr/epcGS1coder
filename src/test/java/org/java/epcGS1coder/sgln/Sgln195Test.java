package org.java.epcGS1coder.sgln;

import org.junit.Assert;
import org.junit.Test;

public class Sgln195Test {
    
    @Test
    public void fromEpcTest(){
        Sgln195 sgln195 = Sgln195.fromEpc("39140008DEB769A32ECD9BD2A17E000000000000000000000000");
        Assert.assertEquals(0, sgln195.getFilter());
        Assert.assertEquals(567, sgln195.getCompanyPrefix());
        Assert.assertEquals(89012, sgln195.getLocationReference());
        Assert.assertEquals("hello%!?", sgln195.getExtension());
        Assert.assertEquals("urn:epc:tag:sgln-195:0.0000567.89012.hello%25!%3F", sgln195.getUri());
    }

    @Test
    public void fromUriTest(){
        String uri = "urn:epc:tag:sgln-195:0.0000567.89012.hello%25!%3F";
        Assert.assertEquals("39140008DEB769A32ECD9BD2A17E000000000000000000000000", Sgln195.fromUri(uri).getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("39140008DEB769A32ECD9BD2A17E000000000000000000000000", Sgln195.fromFields(0, 7, 567, 89012, "hello%!?").getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("39140008DEB769A32ECD9BD2A17E000000000000000000000000", Sgln195.fromGs1Key(0, 7, "0000567890123", "hello%!?").getEpc().toUpperCase());
    }
}
