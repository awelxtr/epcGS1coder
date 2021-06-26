package org.java.epcGS1coder.sgtin;

import org.junit.Assert;
import org.junit.Test;

public class Sgtin96Test {

    @Test
    public void fromEpc(){
        String epc = "3034007D00011C400000000B";
        Sgtin96 sgtin96 = Sgtin96.fromEpc(epc);
        Assert.assertEquals(sgtin96.getFilter(),1);
        Assert.assertEquals(sgtin96.getCompanyPrefix(),8000);
        Assert.assertEquals(sgtin96.getItemReference(),1137);
        Assert.assertEquals(sgtin96.getSerial(),11l);
    }

    @Test
    public void decodeEncodeEpc(){
        String epc = "3034007d00011c400000000b";
        Sgtin96 sgtin96 = Sgtin96.fromEpc(epc);
        Assert.assertEquals(epc.toLowerCase(),sgtin96.getEpc().toLowerCase());
    }

    @Test
    public void encodeUri(){
        String epc = "3034007d00011c400000000b";
        Sgtin96 sgtin96 = Sgtin96.fromEpc(epc);
        Assert.assertEquals("urn:epc:tag:sgtin-96:1.0008000.001137.11",sgtin96.getUri());
    }

    @Test
    public void fromUri(){
        String uri = "urn:epc:tag:sgtin-96:1.0008000.001137.11";
        Sgtin96 sgtin96 = Sgtin96.fromUri(uri);
        Assert.assertEquals("3034007d00011c400000000b",sgtin96.getEpc().toLowerCase());
    }

    @Test
    public void fromFieldsTest(){
        Sgtin96 sgtin96 = Sgtin96.fromFields(1, 7,8000,1137,11l);
        Assert.assertEquals("3034007d00011c400000000b",sgtin96.getEpc().toLowerCase());
    }

    @Test
    public void fromGs1KeyTest(){
        Assert.assertEquals("3036015FFC22928003456588",Sgtin96.fromGs1Key(1,7,"08411135354029",54879624l).getEpc());
    }
}
