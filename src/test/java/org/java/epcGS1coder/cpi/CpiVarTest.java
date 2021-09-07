package org.java.epcGS1coder.cpi;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class CpiVarTest {
    
    @Test
    public void fromEpc(){
        String epc = "3D1878AC34DB800000D7D829C000";
        CpiVar cpiVar = CpiVar.fromEpc(epc);
        Assert.assertEquals(0,cpiVar.getFilter());
        Assert.assertEquals(123568,cpiVar.getCompanyPrefix());
        Assert.assertEquals("468",cpiVar.getComponentPartReference());
        Assert.assertEquals(56582311l,cpiVar.getSerial());
        Assert.assertEquals("urn:epc:tag:cpi-var:0.123568.468.56582311", cpiVar.getUri());
    }

    @Test
    public void fromUri(){
        Assert.assertTrue(Pattern.matches("3D1878AC34DB800000D7D829C0*", CpiVar.fromUri("urn:epc:tag:cpi-var:0.123568.468.56582311").getEpc().toUpperCase()));
    }

    @Test
    public void fromFieldsTest(){
        Assert.assertTrue(Pattern.matches("3D1878AC34DB800000D7D829C0*", CpiVar.fromFields(0,6,123568,"468",56582311l).getEpc().toUpperCase()));
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("3D03A352943FFF103A352943FFC0",CpiVar.fromGs1Key(0,12,"9999999999991",999999999999l).getEpc().toUpperCase());
        Assert.assertEquals("3D03A352943FFF0C30C30C30C30C30C30C30C30C00E8D4A50FFF",CpiVar.fromGs1Key(0,12,"999999999999000000000000000000",999999999999l).getEpc().toUpperCase());
        Assert.assertEquals("3D1BD08FF9E79E79E79E79E79E79E79E79E79E79E79E40E8D4A50FFF",CpiVar.fromGs1Key(0,6,"999999999999999999999999999999",999999999999l).getEpc().toUpperCase());
        Assert.assertEquals("3D1BD08FF9E79E79E79E79E79E79E79E79E79E7903A352943FFC",CpiVar.fromGs1Key(0,6,"999999999999999999999999999",999999999999l).getEpc().toUpperCase());
        Assert.assertEquals("3D1BD08FF9E79E79E79E79E79E79E79E79E40E8D4A50FFF0",CpiVar.fromGs1Key(0,6,"999999999999999999999999",999999999999l).getEpc().toUpperCase());
        Assert.assertEquals("3D1BD08FF9E79E79E79E79E79E79E79E40E8D4A50FFF",CpiVar.fromGs1Key(0,6,"9999999999999999999999",999999999999l).getEpc().toUpperCase());
        Assert.assertEquals("3D1BD08FF9E79E79E79E79E79E7903A352943FFC",CpiVar.fromGs1Key(0,6,"9999999999999999999",999999999999l).getEpc().toUpperCase());
        Assert.assertEquals("3D1BD08FF9E79E79E79E79E40E8D4A50FFF0",CpiVar.fromGs1Key(0,6,"9999999999999999",999999999999l).getEpc().toUpperCase());
        Assert.assertEquals("3D1BD08FF9E79E79E79E40E8D4A50FFF",CpiVar.fromGs1Key(0,6,"99999999999999",999999999999l).getEpc().toUpperCase());
        Assert.assertEquals("3D1BD08FF9E79E7903A352943FFC",CpiVar.fromGs1Key(0,6,"99999999999",999999999999l).getEpc().toUpperCase());
        Assert.assertEquals("3D1BD08FF9E40E8D4A50FFF0",CpiVar.fromGs1Key(0,6,"99999999",999999999999l).getEpc().toUpperCase());

        Assert.assertTrue(Pattern.matches("3D1878AC34DB800000D7D829C0*", CpiVar.fromGs1Key(0,6,"123568468",56582311l).getEpc().toUpperCase()));
    }

    @Test
    public void docExampleTest(){
        CpiVar cpivar;
        
        cpivar= CpiVar.fromEpc("3D74257BF75411DEF6B4CC00000003039");
        Assert.assertEquals("urn:epc:tag:cpi-var:3.0614141.5PQ7%2FZ43.12345", cpivar.getUri());

        cpivar= CpiVar.fromGs1Key(3,7,"06141415PQ7/Z43",12345l);
        Assert.assertEquals("urn:epc:tag:cpi-var:3.0614141.5PQ7%2FZ43.12345", cpivar.getUri());
        Assert.assertTrue(Pattern.matches("3D74257BF75411DEF6B4CC000000030390*", cpivar.getEpc()));
    }
}
