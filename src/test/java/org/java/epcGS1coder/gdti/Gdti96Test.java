package org.java.epcGS1coder.gdti;

import org.junit.Assert;
import org.junit.Test;

public class Gdti96Test {
    @Test
    public void fromEpcTest(){
        Gdti96 gdti96 = Gdti96.fromEpc("2C28499602D2180000000002");
        Assert.assertEquals(1, gdti96.getFilter());
        Assert.assertEquals(1234567890, gdti96.getCompanyPrefix());
        Assert.assertEquals(12, gdti96.getDocumentType());
        Assert.assertEquals(2l, gdti96.getSerial());
        Assert.assertEquals("urn:epc:tag:gdti-96:1.1234567890.12.2", gdti96.getUri());
    }

    @Test
    public void fromUriTest(){
        String uri = "urn:epc:tag:gdti-96:1.1234567890.12.2";
        Assert.assertEquals("2C28499602D2180000000002", Gdti96.fromUri(uri).getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("2C28499602D2180000000002", Gdti96.fromFields(1, 10, 1234567890, 12, 2l).getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("2C28499602D2180000000002", Gdti96.fromGs1Key(1, 10, "12345678901232").getEpc().toUpperCase());
    }
}
