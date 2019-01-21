package jp.co.shiratsuki.walkietalkie.bean;

import java.io.Serializable;

/**
 * 联系人实体类
 * Created at 2019/1/19 13:16
 *
 * @author Li Yuliang
 * @version 1.0
 */

public class Contact implements Serializable {

    private String userIP;

    private String userName;

    private String iconUrl;

    public Contact(String userIP, String userName, String iconUrl) {
        this.userIP = userIP;
        this.userName = userName;
        this.iconUrl = iconUrl;
    }

    public String getUserIP() {
        return userIP;
    }

    public void setUserIP(String userIP) {
        this.userIP = userIP;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}