package org.java.epcGS1coder.gdti;

import org.junit.Assert;
import org.junit.Test;

public class Gdti174Test {
    @Test
    public void fromEpcTest(){
        Gdti174 gdti174 = Gdti174.fromEpc("3E28499602D219A32EA7E00000000000000000000000");
        Assert.assertEquals(1, gdti174.getFilter());
        Assert.assertEquals(1234567890, gdti174.getCompanyPrefix());
        Assert.assertEquals(12, gdti174.getDocumentType());
        Assert.assertEquals("hej?", gdti174.getSerial());
        Assert.assertEquals("urn:epc:tag:gdti-174:1.1234567890.12.hej%3F", gdti174.getUri());
    }

    @Test
    public void fromUriTest(){
        String uri = "urn:epc:tag:gdti-174:1.1234567890.12.hej%3F";
        Assert.assertEquals("3E28499602D219A32EA7E00000000000000000000000", Gdti174.fromUri(uri).getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("3E28499602D219A32EA7E00000000000000000000000", Gdti174.fromFields(1, 10, 1234567890, 12, "hej?").getEpc().toUpperCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("3E28499602D219A32EA7E00000000000000000000000", Gdti174.fromGs1Key(1, 10, "1234567890128hej?").getEpc().toUpperCase());
    }
}
