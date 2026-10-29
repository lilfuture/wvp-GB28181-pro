package com.genersoft.iot.vmp.gb28181.utils;

import com.genersoft.iot.vmp.gb28181.bean.Gb28181Sdp;
import lombok.extern.slf4j.Slf4j;

import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 语音对讲命名与 SDP 解析工具。
 */
@Slf4j
public final class TalkUtils {

    public static final String APP_TALK = "talk";

    public static final String APP_BROADCAST = "broadcast";

    public static final String RECEIVE_STREAM_SUFFIX = "_talk";

    /**
     * PCMA/8000 的载荷类型
     */
    public static final int PT_PCMA = 8;

    /**
     * PS/90000 的载荷类型
     */
    public static final int PT_PS = 96;

    public static final String CODEC_PCMA = "PCMA";

    public static final String CODEC_PS = "PS";

    private TalkUtils() {
    }

    public static String buildStream(String deviceId, String channelId) {
        return deviceId + "_" + channelId;
    }

    public static String buildReceiveStream(String stream) {
        return stream + RECEIVE_STREAM_SUFFIX;
    }

    public static boolean isTalkReceiveStream(String stream) {
        return stream != null && stream.endsWith(RECEIVE_STREAM_SUFFIX);
    }

    public static String removeReceiveStreamSuffix(String stream) {
        if (!isTalkReceiveStream(stream)) {
            return stream;
        }
        return stream.substring(0, stream.length() - RECEIVE_STREAM_SUFFIX.length());
    }

    /**
     * 解析设备 200 OK 中的 SDP，提取音频发送目标与协商出的音频编码。
     * 地址缺失时回退到设备 IP 与设备端口；未提供任何音频媒体段时按 PCMA/8 兼容处理。
     */
    public static TalkSdpAddress parseTalkSdp(String sdp, String fallbackIp, Integer fallbackPort) {
        String ip = null;
        Integer port = null;
        boolean hasAudio = false;
        TalkCodec codec = null;
        if (sdp != null && !sdp.isEmpty()) {
            try {
                Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(sdp);
                SessionDescription baseSdb = gb28181Sdp == null ? null : gb28181Sdp.getBaseSdb();
                if (baseSdb != null) {
                    Connection sessionConnection = baseSdb.getConnection();
                    if (sessionConnection != null) {
                        ip = sessionConnection.getAddress();
                    }
                    List<MediaDescription> audioDescriptions = audioDescriptions(baseSdb);
                    hasAudio = !audioDescriptions.isEmpty();
                    MediaDescription sendTarget = selectSendTarget(audioDescriptions);
                    if (sendTarget != null) {
                        Media media = sendTarget.getMedia();
                        Connection mediaConnection = sendTarget.getConnection();
                        if (mediaConnection != null && mediaConnection.getAddress() != null) {
                            ip = mediaConnection.getAddress();
                        }
                        if (media.getMediaPort() > 0) {
                            port = media.getMediaPort();
                        }
                        // 部分设备回的是非标准协议字段（如 "tcp"），仅记录，不影响地址解析
                        if (!"RTP/AVP".equalsIgnoreCase(media.getProtocol())) {
                            log.warn("[语音对讲] 设备 SDP 音频媒体协议非标准：{}，端口：{}", media.getProtocol(), port);
                        }
                    }
                    codec = findCodec(audioDescriptions);
                }
            } catch (Exception e) {
                log.warn("[语音对讲] 解析设备 SDP 失败，回退设备地址: {}", e.getMessage());
            }
        }
        if (ip == null || ip.isEmpty()) {
            ip = fallbackIp;
        } else if (isPrivateAddress(ip) && fallbackIp != null && !isPrivateAddress(fallbackIp)) {
            // 设备处于 NAT 之后时，SDP 中的私网地址平台不可达，改用信令侧公网地址配合设备声明端口
            log.info("[语音对讲] 设备 SDP 为私网地址 {}，改用信令地址 {}:{}", ip, fallbackIp, port);
            ip = fallbackIp;
        }
        if (port == null || port <= 0) {
            port = fallbackPort;
        }
        if (codec == null) {
            // 设备完全没有音频媒体段时保持原有 PCMA/8 行为；声明了音频媒体段却没有 PCMA/PS 的，
            // 判定为不可协商，由调用方立即终止会话
            return new TalkSdpAddress(ip, port, PT_PCMA, false, null, !hasAudio);
        }
        return new TalkSdpAddress(ip, port, codec.payloadType, codec.psMode, codec.name, true);
    }

    private static List<MediaDescription> audioDescriptions(SessionDescription baseSdb) throws SdpException {
        List<MediaDescription> result = new ArrayList<>();
        Vector mediaDescriptions = baseSdb.getMediaDescriptions(false);
        if (mediaDescriptions == null) {
            return result;
        }
        for (Object item : mediaDescriptions) {
            MediaDescription mediaDescription = (MediaDescription) item;
            Media media = mediaDescription.getMedia();
            if (media != null && "audio".equalsIgnoreCase(media.getMediaType())) {
                result.add(mediaDescription);
            }
        }
        return result;
    }

    /**
     * 优先选择设备可接收音频的媒体段（recvonly / sendrecv），平台向该端口发送对讲音频。
     */
    private static MediaDescription selectSendTarget(List<MediaDescription> audioDescriptions) throws SdpException {
        for (MediaDescription mediaDescription : audioDescriptions) {
            if (mediaDescription.getAttribute("recvonly") != null
                    || mediaDescription.getAttribute("sendrecv") != null) {
                return mediaDescription;
            }
        }
        return audioDescriptions.isEmpty() ? null : audioDescriptions.get(0);
    }

    /**
     * 从音频媒体段中识别平台可用的编码，优先 PCMA，其次 PS。
     */
    private static TalkCodec findCodec(List<MediaDescription> audioDescriptions) throws SdpException {
        TalkCodec pcma = null;
        TalkCodec ps = null;
        for (MediaDescription mediaDescription : audioDescriptions) {
            Vector attributes = mediaDescription.getAttributes(false);
            if (attributes != null) {
                for (Object item : attributes) {
                    Attribute attribute = (Attribute) item;
                    if (attribute.getValue() == null || !"rtpmap".equalsIgnoreCase(attribute.getName())) {
                        continue;
                    }
                    String[] payloadAndEncoding = attribute.getValue().trim().split("\\s+", 2);
                    if (payloadAndEncoding.length < 2) {
                        continue;
                    }
                    Integer payloadType = parsePayloadType(payloadAndEncoding[0]);
                    String encoding = payloadAndEncoding[1].split("/")[0].toUpperCase();
                    if (payloadType == null) {
                        continue;
                    }
                    if (CODEC_PCMA.equals(encoding) && pcma == null) {
                        pcma = new TalkCodec(payloadType, false, CODEC_PCMA);
                    } else if (CODEC_PS.equals(encoding) && ps == null) {
                        ps = new TalkCodec(payloadType, true, CODEC_PS);
                    }
                }
            }
            Vector formats = mediaDescription.getMedia() == null ? null : mediaDescription.getMedia().getMediaFormats(false);
            if (pcma == null && ps == null && formats != null) {
                // 未声明 rtpmap 时按 m= 的载荷列表兜底
                for (Object format : formats) {
                    String value = String.valueOf(format).trim();
                    if (String.valueOf(PT_PCMA).equals(value)) {
                        pcma = new TalkCodec(PT_PCMA, false, CODEC_PCMA);
                    } else if (String.valueOf(PT_PS).equals(value)) {
                        ps = new TalkCodec(PT_PS, true, CODEC_PS);
                    }
                }
            }
        }
        return pcma != null ? pcma : ps;
    }

    private static Integer parsePayloadType(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class TalkCodec {
        private final int payloadType;
        private final boolean psMode;
        private final String name;

        private TalkCodec(int payloadType, boolean psMode, String name) {
            this.payloadType = payloadType;
            this.psMode = psMode;
            this.name = name;
        }
    }

    /**
     * 判断是否为 RFC1918 私网地址或回环地址。
     */
    private static boolean isPrivateAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }
        if ("127.0.0.1".equals(ip) || ip.startsWith("127.") || "0.0.0.0".equals(ip)) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 设备 200 OK 解析结果：音频发送目标 + 协商出的音频编码。
     */
    public static class TalkSdpAddress {
        private final String ip;
        private final Integer port;
        private final Integer payloadType;
        private final boolean usePs;
        private final String codec;
        private final boolean codecSupported;

        public TalkSdpAddress(String ip, Integer port, Integer payloadType, boolean usePs,
                              String codec, boolean codecSupported) {
            this.ip = ip;
            this.port = port;
            this.payloadType = payloadType;
            this.usePs = usePs;
            this.codec = codec;
            this.codecSupported = codecSupported;
        }

        public String getIp() {
            return ip;
        }

        public Integer getPort() {
            return port;
        }

        /**
         * 平台向设备发送音频时使用的载荷类型
         */
        public Integer getPayloadType() {
            return payloadType;
        }

        /**
         * 平台向设备发送音频时是否需要 PS 封装
         */
        public boolean isUsePs() {
            return usePs;
        }

        /**
         * 协商出的编码名称（PCMA / PS），无法识别时为 null
         */
        public String getCodec() {
            return codec;
        }

        /**
         * 设备返回的音频编码是否可与平台协商
         */
        public boolean isCodecSupported() {
            return codecSupported;
        }
    }
}
