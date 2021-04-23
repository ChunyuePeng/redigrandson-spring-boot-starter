package com.pcy.redigrandson;

/**
 * @description: 用来存放Redis连接信息
 * @author: 彭椿悦
 * @data: 2021/4/23 9:34
 */
public class Config {
    private boolean useSingleServer = true;
    private ServerInfo singleServerInfo;
    public void useSingleServer(ServerInfo serverInfo){
        if (serverInfo == null){
            singleServerInfo = new ServerInfo();
        }else {
            singleServerInfo = serverInfo;
        }
    }

    public ServerInfo getSingleServerInfo(){
        return singleServerInfo;
    }

    public boolean isUseSingleServer() {
        return useSingleServer;
    }

    public void setUseSingleServer(boolean useSingleServer) {
        this.useSingleServer = useSingleServer;
    }
}
