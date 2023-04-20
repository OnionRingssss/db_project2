package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MsgType;
import cn.edu.sustech.cs209.chatting.common.OOS_OIS;
import cn.edu.sustech.cs209.chatting.common.Users;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


class ServerReader implements Runnable {

    private Socket socket;

    public ServerReader(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            OOS_OIS.MyObjectInputStream ois = new OOS_OIS.MyObjectInputStream(socket.getInputStream());
            OOS_OIS.MyObjectOutputStream os = new OOS_OIS.MyObjectOutputStream(socket.getOutputStream());
            //read message and send it
            while (true) {
                Message message = (Message) ois.readObject();
                switch (message.getType()) {
                    case COMMAND:
                        //用户退出
                        if (message.getData().equals("exit")) {
                            Users.user_socket_map.remove(message.getSentBy());
                            System.out.println(message.getSentBy() + "exit");
                        } else if (message.getData().equals("login")) {
                            Users.user_socket_map.put(message.getSentBy(), socket);
                            //刷新client的userlist表，向所有链接的发送一个新的userlist
                            System.out.println("发送新的userlist");
                            //向所有client发送当前的userlist,和当前用户数量
                            String userString = getUsers();
                            for (Map.Entry<String, Socket> entry : Users.user_socket_map.entrySet()) {
                                os = new OOS_OIS.MyObjectOutputStream(entry.getValue().getOutputStream());
                                os.writeObject(
                                        new Message(System.currentTimeMillis(), "Server", entry.getKey(), userString, MsgType.REQ)
                                );
                                os.flush();
                            }

                            System.out.println("fan hui cheng gong");
                        }
                        break;
                    case TALK:
                        //用户发送信息给另一个用户
                        System.out.println(Users.user_socket_map);
//                        System.out.println(message.getSentBy() + " told " + message.getSendTo() + ":" + message.getData());

                        //如果另一个用户在线
                        if (Users.one_to_one.containsKey(message.getSendTo())) {
                            //另一个用户打开着相对应用户的聊天界面
                            if (Users.one_to_one.get(message.getSendTo()).equals(message.getSentBy())) {
                                //直接给他发送消息
                                os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSendTo()).getOutputStream());
                                os.writeObject(message);
                                os.flush();
                            } else {
                                //将信息放进该用户的聊天里，等到后续该用户打开时，一次性加载出来
                                System.out.println("该用户在和其他用户聊天哦");
                                Users.user_user_messages.get(message.getSendTo()).get(message.getSentBy()).add(message);
                            }
                        }
                        //如果另一个用户
                        else {
                            System.out.println("该用户还没开始聊天哦");
                            Users.user_user_messages.get(message.getSendTo()).get(message.getSentBy()).add(message);
                        }


                        //发送给特定的人
//                        os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSendTo()).getOutputStream());
//                        os.writeObject(message);
//                        os.flush();
                        break;
                    case TALKINGTO:
                        System.out.println("收到客户端传来的改talkingto的信息");
                        //更新所有的onetooneMap
                        Users.one_to_one.put(message.getSentBy(), message.getSendTo());

                        //如果不存在
                        if (!Users.user_user_messages.containsKey(message.getSentBy())) {
                            //更新userusermsg
                            Users.user_user_messages.put(message.getSentBy(), new HashMap<>());
                            Users.user_user_messages.get(message.getSentBy()).put(message.getSendTo(), new ArrayList<>());
                            Users.user_user_messages.put(message.getSendTo(), new HashMap<>());
                            Users.user_user_messages.get(message.getSendTo()).put(message.getSentBy(), new ArrayList<>());
                        }
                        //如果存在,依次取出每一个message发送过去
                        else {
                            while (Users.user_user_messages.get(message.getSentBy()).get(message.getSendTo()).size() != 0) {
                                os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSentBy()).getOutputStream());
                                os.writeObject(Users.user_user_messages.get(message.getSentBy()).get(message.getSendTo()).get(0));
                                os.flush();
                                Users.user_user_messages.get(message.getSentBy()).get(message.getSendTo()).remove(0);
                            }
                        }
                    default:
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public String getUsers() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Socket> entry : Users.user_socket_map.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append("~");
        }
//        return new Message(System.currentTimeMillis(),"Server",m.getSentBy(),stringBuilder.substring(0,stringBuilder.length()-1),MsgType.REQ);
        return stringBuilder.append(Users.user_socket_map.size()).toString();
    }


}
//}
