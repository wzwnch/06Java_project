package com.shortlink.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "访问日志DTO")
public class LinkAccessLogDTO {

    @Schema(description = "短链接码")
    private String shortCode;

    @Schema(description = "分组标识")
    private String gid;

    @Schema(description = "PV计数")
    private Long pv;

    @Schema(description = "UV标识（用户指纹）")
    private String uv;

    @Schema(description = "UIP标识（IP）")
    private String uip;

    @Schema(description = "访问IP")
    private String ip;

    @Schema(description = "地区")
    private String region;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "设备类型")
    private String device;

    @Schema(description = "网络类型")
    private String network;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public static LinkAccessLogDTO create(String shortCode, String gid, String ip, String uv, String uip, String region, String os, String browser, String device, String network) {
        LinkAccessLogDTO dto = new LinkAccessLogDTO();
        dto.setShortCode(shortCode);
        dto.setGid(gid);
        dto.setPv(1L);
        dto.setUv(uv);
        dto.setUip(uip);
        dto.setIp(ip);
        dto.setRegion(region);
        dto.setOs(os);
        dto.setBrowser(browser);
        dto.setDevice(device);
        dto.setNetwork(network);
        dto.setCreateTime(LocalDateTime.now());
        return dto;
    }
}
