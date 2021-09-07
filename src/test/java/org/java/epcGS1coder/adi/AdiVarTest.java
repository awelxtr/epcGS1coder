package org.java.epcGS1coder.adi;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class AdiVarTest {
    @Test
    public void fromEpc(){
        String epc = "3B0E0CF5E76C9047759AD00373DC7602E7200";
        AdiVar adiVar = AdiVar.fromEpc(epc);
        Assert.assertEquals("35962", adiVar.getCage());
        Assert.assertEquals("PQ7VZ4", adiVar.getPartNumber());
        Assert.assertEquals("M37GXB92", adiVar.getSerial());
        Assert.assertEquals("urn:epc:tag:adi-var:3.35962.PQ7VZ4.M37GXB92", adiVar.getUri());
    }

    @Test
    public void fromUri(){
        Assert.assertEquals("3B0E0CF5E76C9047759AD00373DC7602E7200", AdiVar.fromUri("urn:epc:tag:adi-var:3.35962.PQ7VZ4.M37GXB92").getEpc().toUpperCase());
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertEquals("3B0E0CF5E76C9047759AD00373DC7602E7200", AdiVar.fromFields(3,"35962","PQ7VZ4","M37GXB92").getEpc().toUpperCase());
    }

    @Test
    public void moarTest(){
        Assert.assertEquals("urn:epc:tag:adi-var:3.35962.PQ7VZ4.M37GXB9243G", AdiVar.fromEpc("3B0E0CF5E76C9047759AD00373DC7602E72D331C0000").getUri());
        Assert.assertTrue(Pattern.matches("3B0E0CF5E76C9047759AD00373DC7602E72D331C0*", AdiVar.fromUri("urn:epc:tag:adi-var:3.35962.PQ7VZ4.M37GXB9243G").getEpc().toUpperCase()));
    }
}
