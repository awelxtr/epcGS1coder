package org.java.epcGS1coder.sgtin;

import org.junit.Assert;
import org.junit.Test;

public class Sgtin198Test {

    @Test
    public void fromEpcTest(){
        String epc = "3634007D00011C7A68D3CE4F383532F3E8000000000000000000";
        Sgtin198 sgtin198 = Sgtin198.fromEpc(epc);
        Assert.assertEquals(1, sgtin198.getFilter());
        Assert.assertEquals(8000, sgtin198.getCompanyPrefix());
        Assert.assertEquals(1137, sgtin198.getItemReference());
        Assert.assertEquals("thisIsATest", sgtin198.getSerial());
        Assert.assertEquals("urn:epc:tag:sgtin-198:1.0008000.001137.thisIsATest", sgtin198.getUri());
    }

    @Test
    public void fromUriFieldsTest(){
        String uri = "urn:epc:tag:sgtin-198:1.0008000.001137.thisIs%26Test";
        Sgtin198 sgtin198 = Sgtin198.fromUri(uri);
        Assert.assertEquals(1, sgtin198.getFilter());
        Assert.assertEquals(8000, sgtin198.getCompanyPrefix());
        Assert.assertEquals(1137, sgtin198.getItemReference());
        Assert.assertEquals("thisIs&Test", sgtin198.getSerial());
    }

    @Test
    public void fromUriEpcTest(){
        String uri = "urn:epc:tag:sgtin-198:1.0008000.001137.thisIsATest";
        Sgtin198 sgtin198 = Sgtin198.fromUri(uri);
        Assert.assertEquals("3634007D00011C7A68D3CE4F383532F3E8000000000000000000",sgtin198.getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Sgtin198 sgtin198 = Sgtin198.fromFields(1,7,8000l,1137,"thisIs&Test");
        Assert.assertEquals("urn:epc:tag:sgtin-198:1.0008000.001137.thisIs%26Test", sgtin198.getUri());
    }
}
