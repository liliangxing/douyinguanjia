package me.douyin.guanjia.model;
import java.io.Serializable;

import lombok.Data;

@Data
public class VideoVO implements Serializable {
    private String ratio;
    private Integer width;
    private Integer height;
    private Integer duration;
    private UriVO play_addr;
    private UriVO cover;
    private UriVO origin_cover;
    private Boolean has_watermark   ;
}
