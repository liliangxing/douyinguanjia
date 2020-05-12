package me.douyin.guanjia.model;
import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class UriVO implements Serializable {
    private String uri;
    private List<String> url_list;
}
