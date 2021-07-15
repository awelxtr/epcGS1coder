package org.java.epcGS1coder.gdti;

import org.junit.Assert;
import org.junit.Test;

public class Gdti113Test {

    @Test
    public void fromEpcTestSuperEasyDebugging(){
        Gdti113 gdti113 = Gdti113.fromEpc("3A28499602D218000000000000050000");
        Assert.assertEquals(1, gdti113.getFilter());
        Assert.assertEquals(1234567890, gdti113.getCompanyPrefix());
        Assert.assertEquals(12, gdti113.getDocumentType());
        Assert.assertEquals("0", gdti113.getSerial());
        Assert.assertEquals("urn:epc:tag:gdti-113:1.1234567890.12.0", gdti113.getUri());
    }

    @Test
    public void fromEpcTest(){
        Gdti113 gdti113 = Gdti113.fromEpc("3A28499602D218000000004C75290000");
        Assert.assertEquals(1, gdti113.getFilter());
        Assert.assertEquals(1234567890, gdti113.getCompanyPrefix());
        Assert.assertEquals(12, gdti113.getDocumentType());
        Assert.assertEquals("0021458", gdti113.getSerial());
        Assert.assertEquals("urn:epc:tag:gdti-113:1.1234567890.12.0021458", gdti113.getUri());
    }

    @Test
    public void fromUriTest(){
        String uri = "urn:epc:tag:gdti-113:1.1234567890.12.0021458";
        Assert.assertEquals("3A28499602D218000000004C75290000", Gdti113.fromUri(uri).getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("3A28499602D218000000004C75290000", Gdti113.fromFields(1, 10, 1234567890, 12, "0021458").getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("3A28499602D218000000004C75290000", Gdti113.fromGs1Key(1, 10, "12345678901280021458").getEpc().toUpperCase());
    }
}
