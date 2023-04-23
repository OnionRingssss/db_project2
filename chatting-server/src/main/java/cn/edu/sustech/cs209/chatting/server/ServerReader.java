package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.*;


import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.Socket;
import java.util.*;


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
                        if (!Users.user_socket_map.containsKey(message.getSentBy())) {
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
                        } else {
                            os = new OOS_OIS.MyObjectOutputStream(socket.getOutputStream());
                            os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), "not allow login", MsgType.NOT_ALLOW_LOGIN));
                            os.flush();
                        }
                        break;

                    case EXIT:
                        //将该controller代表的user从userlist中清除出去
                        Users.user_socket_map.remove(message.getSentBy());
                        //向每一个发信息，返回下线的username
                        for (Map.Entry<String, Socket> entry : Users.user_socket_map.entrySet()) {
                            os = new OOS_OIS.MyObjectOutputStream(entry.getValue().getOutputStream());
                            os.writeObject(new Message(System.currentTimeMillis(), message.getSentBy(), entry.getKey(), Integer.toString(Users.user_socket_map.size()), MsgType.EXIT));
                            os.flush();
                        }
                        break;

                    case EXIT_NO_KEEP:
                        //向每一个在线的client发送消息，返回退出的username
                        String userString = getUsers();
                        for (Map.Entry<String, Socket> entry : Users.user_socket_map.entrySet()) {
                            os = new OOS_OIS.MyObjectOutputStream(entry.getValue().getOutputStream());
                            os.writeObject(new Message(System.currentTimeMillis(), message.getSentBy(), entry.getKey(), userString, MsgType.EXIT_NO_KEEP));
                            os.flush();
                        }
                        break;

                    case TALK:
                        //用户发送信息给另一个用户
                        //如果另一个用户在线
//                        if (Users.one_to_one.containsKey(message.getSendTo())) {
                        //另一个用户打开着相对应用户的聊天界面
                        if (Users.one_to_one.get(message.getSendTo()).equals(message.getSentBy())) {
                            //直接给他发送消息
                            Users.user_user_messages.get(message.getSentBy() + "`" + message.getSendTo()).add(message);
                            Users.user_user_messages.get(message.getSendTo() + "`" + message.getSentBy()).add(message);

                            os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSendTo()).getOutputStream());
                            os.writeObject(message);
                            os.flush();
                        } else {
                            //将信息放进该用户的聊天里，等到后续该用户打开时，一次性加载出来
                            System.out.println("该用户在和其他用户聊天哦");
                            Users.user_user_messages.get(message.getSentBy() + "`" + message.getSendTo()).add(message);
                            Users.user_user_messages.get(message.getSendTo() + "`" + message.getSentBy()).add(message);
                            try {
                                MyAudioPlayer.playMusic();
                            } catch (UnsupportedAudioFileException | LineUnavailableException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("丰富了userusermessage给：" + message.getSentBy() + "`" + message.getSendTo());
                        break;

                    case G_TALK:
                        //发送信息给每一个在该群组里面的user的群聊窗口
                        String groupMsg = message.getSentBy().split(":")[0];
                        for (String s : Users.group_users_map.get(groupMsg)) {
                            os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(s).getOutputStream());
                            os.writeObject(new Message(System.currentTimeMillis(), message.getSentBy().split(":")[1], groupMsg + ":" + s, message.getData(), MsgType.G_TALK));
                            os.flush();
                            System.out.println("server成功返回群聊发送的消息");
                        }
                        break;

                    case TALKINGTO:
                        //传入的信息中：sentby是改变talkingto的user，sendto是被改变后的talkingTo
                        System.out.println("收到客户端传来的改talkingto的信息,客户端为：" + message.getSentBy());
                        //更新所有的onetooneMap
                        Users.one_to_one.put(message.getSentBy(), message.getSendTo());

                        String str = message.getSentBy() + "`" + message.getSendTo();//前面是发送方，后面是接收方

                        if (!Users.user_user_messages.containsKey(str)) {
                            System.out.println("str input = " + str);
                            Users.user_user_messages.put(str, new ArrayList<>());
                            Users.user_user_messages.put(message.getSendTo() + "`" + message.getSentBy(), new ArrayList<>());
                        } else {
                            //一个一个返回在存储的聊天记录
                            for (int i = 0; i < Users.user_user_messages.get(str).size(); i++) {
                                os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSentBy()).getOutputStream());
                                os.writeObject(Users.user_user_messages.get(str).get(i));
                                System.out.println("返回存储的聊天记录给" + message.getSentBy());
                                os.flush();
                            }
                        }
                        break;

                    case GROUP_CREATE://如果是一个创建群聊的信息
                        //将这个群组的名字及成员信息加入到Users的相应Map中
                        //解码
                        List<String> gumStr = new ArrayList<>();//group中所有的users
                        String[] GUMStr = message.getData().split("~");//第0位是group的名字
                        String lastOne = GUMStr[GUMStr.length - 1];//当前的这个user
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < GUMStr.length; i++) {
                            gumStr.add(GUMStr[i]);
                            sb.append(GUMStr[i]).append("~");
                        }
                        Users.group_users_map.put(GUMStr[0], gumStr);

                        //让与这个group有关的所有client也创建一个新的Gcontroller
                        for (String s : Users.group_users_map.get(GUMStr[0])) {
                            os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(s).getOutputStream());
                            //s是username，GUMStr【0】是group名字
                            os.writeObject(new Message(System.currentTimeMillis(), GUMStr[0], s, sb.substring(0, sb.length() - 1), MsgType.G_CREATEGCONTROLLER));
                            os.flush();
                            System.out.println("创建信息返回clients");
                        }
                        break;

                    case EXIT_FROM_GROUP:
                        String groupAndName = message.getSentBy();
                        String group = message.getSentBy().split(":")[0];
                        String username = message.getSentBy().split(":")[1];
                        Users.group_users_map.get(group).remove(username);
                        for (String s : Users.group_users_map.get(group)) {
                            os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(s).getOutputStream());
                            os.writeObject(new Message(System.currentTimeMillis(), "Server", group + ":" + s, getGusers(Users.group_users_map.get(group)), MsgType.EXIT_FROM_GROUP));
                            os.flush();
                        }
                        break;

                    case SERVER_EXIT:
                        System.out.println("Confirm server exit");
                        System.exit(0);
                        break;

                    case FILE:
                        os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSendTo()).getOutputStream());
                        os.writeObject(message);
                        os.flush();
                        System.out.println("客户端收到file信息，并且发送回了客户端");
                        break;

                    default:
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private String getGusers(List<String>stringList) {
        StringBuilder stringBuilder = new StringBuilder();
        for(String s: stringList){
            stringBuilder.append(s).append("~");
        }
        return stringBuilder.substring(0,stringBuilder.length()-1);
    }

//    private String GgetUsers() {
//        StringBuilder stringBuilder = new StringBuilder();
//        for (Map.Entry<String,Socket> entry : Users.userGroup_socket_map.entrySet()) {
//            stringBuilder.append(entry.getKey());
//            stringBuilder.append("~");
//        }
//        return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
//    }

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
