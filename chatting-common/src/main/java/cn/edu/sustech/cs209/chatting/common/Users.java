package cn.edu.sustech.cs209.chatting.common;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Users {
    public static Map<String, Socket> user_socket_map = new HashMap<>();
}
