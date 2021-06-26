package org.java.epcGS1coder.sgtin;

import org.junit.Assert;
import org.junit.Test;

public class Sgtin198Test {

    @Test
    public void fromEpcTest(){
        Sgtin198 sgtin198 = Sgtin198.fromEpc("3634007D00011C7A68D3CE4F383532F3E8000000000000000000");
        Assert.assertEquals(1, sgtin198.getFilter());
        Assert.assertEquals(8000, sgtin198.getCompanyPrefix());
        Assert.assertEquals(1137, sgtin198.getItemReference());
        Assert.assertEquals("thisIsATest", sgtin198.getSerial());
        Assert.assertEquals("urn:epc:tag:sgtin-198:1.0008000.001137.thisIsATest", sgtin198.getUri());
    }

    @Test
    public void fromUriFieldsTest(){
        Sgtin198 sgtin198 = Sgtin198.fromUri("urn:epc:tag:sgtin-198:1.0008000.001137.thisIs%26Test");
        Assert.assertEquals(1, sgtin198.getFilter());
        Assert.assertEquals(8000, sgtin198.getCompanyPrefix());
        Assert.assertEquals(1137, sgtin198.getItemReference());
        Assert.assertEquals("thisIs&Test", sgtin198.getSerial());
    }

    @Test
    public void fromUriEpcTest(){
        Assert.assertEquals("3634007D00011C7A68D3CE4F383532F3E8000000000000000000",Sgtin198.fromUri("urn:epc:tag:sgtin-198:1.0008000.001137.thisIsATest").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("urn:epc:tag:sgtin-198:1.0008000.001137.thisIs%26Test", Sgtin198.fromFields(1,7,8000l,1137,"thisIs&Test").getUri());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("3636015FFC2292B0F3C9879E4CB9323F00000000000000000000", Sgtin198.fromGs1Key(1,7,"08411135354029","asdasdedd?").getEpc().toUpperCase());
    }
}
