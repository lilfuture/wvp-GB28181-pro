package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 语音广播/对讲会话状态。
 */
@Schema(description = "语音广播/对讲会话状态")
public class AudioBroadcastStatus {

    @Schema(description = "状态：Idle/Ready/WaiteInvite/Ok/Failed")
    private String status;

    @Schema(description = "模式：broadcast/talk")
    private String mode;

    @Schema(description = "推流应用名")
    private String app;

    @Schema(description = "推流流ID")
    private String stream;

    @Schema(description = "设备回传流ID")
    private String receiveStream;

    @Schema(description = "设备回传流是否就绪")
    private Boolean receiveStreamReady;

    @Schema(description = "错误信息")
    private String error;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public String getReceiveStream() {
        return receiveStream;
    }

    public void setReceiveStream(String receiveStream) {
        this.receiveStream = receiveStream;
    }

    public Boolean getReceiveStreamReady() {
        return receiveStreamReady;
    }

    public void setReceiveStreamReady(Boolean receiveStreamReady) {
        this.receiveStreamReady = receiveStreamReady;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
