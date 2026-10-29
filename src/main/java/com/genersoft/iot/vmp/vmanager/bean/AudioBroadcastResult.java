package com.genersoft.iot.vmp.vmanager.bean;

/**
 * @author lin
 */
public class AudioBroadcastResult {
    /**
     * 推流的各个方式流地址
     */
    private StreamContent streamInfo;

    /**
     * 编码格式
     */
    private String codec;

    /**
     * 向zlm推流的应用名
     */
    private String app;

    /**
     * 向zlm推流的流ID
     */
    private String stream;

    /**
     * 设备回传音频流的播放地址，仅 Talk 模式返回
     */
    private StreamContent receiveStreamInfo;

    /**
     * 通道是否开启音频能力
     */
    private Boolean audioCapability;


    public StreamContent getStreamInfo() {
        return streamInfo;
    }

    public void setStreamInfo(StreamContent streamInfo) {
        this.streamInfo = streamInfo;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public StreamContent getReceiveStreamInfo() {
        return receiveStreamInfo;
    }

    public void setReceiveStreamInfo(StreamContent receiveStreamInfo) {
        this.receiveStreamInfo = receiveStreamInfo;
    }

    public Boolean getAudioCapability() {
        return audioCapability;
    }

    public void setAudioCapability(Boolean audioCapability) {
        this.audioCapability = audioCapability;
    }
}
