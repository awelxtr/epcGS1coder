package org.java.epcGS1coder.sscc;

import org.junit.Assert;
import org.junit.Test;

public class Sscc96Test {

    @Test
    public void fromEpc(){
        throw new RuntimeException("NOT YET IMPLEMENTED");
    }

    @Test
    public void fromUri(){
        String uri = "urn:epc:tag:sscc-96:6.2345678.1901234567";
        Sscc96 sscc96 = null;
        Assert.assertEquals("31D48F2B3871528987000000",sscc96.getEpc());
    }
}
