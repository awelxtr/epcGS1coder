package org.java.epcGS1coder.sgtin;

import org.junit.Assert;
import org.junit.Test;

public class Sgtin96Test {

    @Test
    public void fromEpc(){
        String epc = "3034007D00011C400000000B";
        Sgtin96 sgtin96 = SgtinBuilder.sgtin96FromEpc(epc);
        Assert.assertEquals(sgtin96.getFilter(),SgtinFilter.pos_item_1);
        Assert.assertEquals(sgtin96.getPartition(),5);
        Assert.assertEquals(sgtin96.getCompanyPrefix(),8000);
        Assert.assertEquals(sgtin96.getItemReference(),1137);
        Assert.assertEquals(sgtin96.getSerial(),11l);
    }

    @Test
    public void decodeEncodeEpc(){
        String epc = "3034007d00011c400000000b";
        Sgtin96 sgtin96 = SgtinBuilder.sgtin96FromEpc(epc);
        Assert.assertEquals(epc.toLowerCase(),sgtin96.getEpc().toLowerCase());
    }

    @Test
    public void encodeUri(){
        String epc = "3034007d00011c400000000b";
        Sgtin96 sgtin96 = SgtinBuilder.sgtin96FromEpc(epc);
        Assert.assertEquals("urn:epc:tag:sgtin-96:1.0008000.001137.11",sgtin96.getUri());
    }

    @Test
    public void decodeUri(){
        String uri = "urn:epc:tag:sgtin-96:1.0008000.001137.11";
        Sgtin96 sgtin96 = SgtinBuilder.fromUri(uri);
        Assert.assertEquals("3034007d00011c400000000b",sgtin96.getEpc().toLowerCase());
    }
}
