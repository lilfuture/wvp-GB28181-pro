package com.genersoft.iot.vmp.gb28181.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalkUtilsTest {

    @Test
    void buildStreamAndReceiveStream() {
        String stream = TalkUtils.buildStream("34020000001320000001", "34020000001320000002");
        assertEquals("34020000001320000001_34020000001320000002", stream);
        assertEquals("34020000001320000001_34020000001320000002_talk", TalkUtils.buildReceiveStream(stream));
        assertTrue(TalkUtils.isTalkReceiveStream(TalkUtils.buildReceiveStream(stream)));
        assertFalse(TalkUtils.isTalkReceiveStream(stream));
        assertEquals(stream, TalkUtils.removeReceiveStreamSuffix(TalkUtils.buildReceiveStream(stream)));
    }

    @Test
    void parseTalkSdpFromSessionConnection() {
        String sdp = "v=0\r\n"
                + "o=34020000001320000001 0 0 IN IP4 192.168.1.10\r\n"
                + "s=Talk\r\n"
                + "c=IN IP4 192.168.1.10\r\n"
                + "t=0 0\r\n"
                + "m=audio 10000 RTP/AVP 8\r\n"
                + "a=sendrecv\r\n"
                + "a=rtpmap:8 PCMA/8000\r\n";
        TalkUtils.TalkSdpAddress address = TalkUtils.parseTalkSdp(sdp, "10.0.0.1", 5060);
        assertEquals("192.168.1.10", address.getIp());
        assertEquals(Integer.valueOf(10000), address.getPort());
    }

    @Test
    void parseTalkSdpFallsBackWhenMissing() {
        TalkUtils.TalkSdpAddress address = TalkUtils.parseTalkSdp(null, "10.0.0.1", 5060);
        assertEquals("10.0.0.1", address.getIp());
        assertEquals(Integer.valueOf(5060), address.getPort());
    }
}
