package org.java.epcGS1coder.gid;

import org.junit.Assert;
import org.junit.Test;

public class Gid96Test {
    @Test
    public void fromEpc(){
        String epc = "350007AB70425D4000000586";
        Gid96 gid96 = Gid96.fromEpc(epc);
        Assert.assertEquals(31415,gid96.getGeneralManagerNumber());
        Assert.assertEquals(271828, gid96.getObjectClass());
        Assert.assertEquals(1414l, gid96.getSerial());
        Assert.assertEquals("urn:epc:tag:gid-96:31415.271828.1414", gid96.getUri());
    }

    @Test
    public void fromUri(){
        Assert.assertEquals("350007AB70425D4000000586", Gid96.fromUri("urn:epc:tag:gid-96:31415.271828.1414").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("350007AB70425D4000000586", Gid96.fromFields(31415,271828,1414l).getEpc().toUpperCase());
    }

}
