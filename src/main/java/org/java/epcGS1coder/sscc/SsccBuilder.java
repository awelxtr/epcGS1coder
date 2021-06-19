package org.java.epcGS1coder.sscc;

public class SsccBuilder {

    public static Sscc96 sscc96FromFields(int filter,
                                            byte partition,
                                            long companyPrefix,
                                            int itemReference,
                                            long serial){
        return sscc96FromFields(SsccFilter.values()[filter],partition,companyPrefix,itemReference,serial);
    }

    public static Sscc96 sscc96FromFields(SsccFilter filter,
                                            byte partition,
                                            long companyPrefix,
                                            int itemReference,
                                            long serial){
        Sscc96 sscc96= new Sscc96(filter,partition,companyPrefix,itemReference,serial);
//        sscc96.setEpc(encode(sgtin96));
        return sscc96;
    }
}
