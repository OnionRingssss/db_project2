package cn.edu.sustech.cs209.chatting.common;



import java.io.Serializable;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Users implements Serializable {
    public static Map<String, Socket> user_socket_map = new ConcurrentHashMap<>();
    public static Map<String,List<Message>> user_user_messages = new ConcurrentHashMap<>();
    public static Map<String,List<Boolean>> user_user_messages_bolean = new HashMap<>();

    public static Map<String,List<Message>>USERtoUSERMsgMap = new HashMap<>();
    public static Map<String,String>one_to_one = new HashMap<>();//实时显示当前正在向其他user发送信息的client，比如a的talkingto是b

    //group:user1,user2,user3''''
    public static Map<String,List<String>> group_users_map = new HashMap<>();

    public static Map<String,Socket> userGroup_socket_map = new ConcurrentHashMap<>();

}
