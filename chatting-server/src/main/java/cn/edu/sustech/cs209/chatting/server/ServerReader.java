package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MsgType;
import cn.edu.sustech.cs209.chatting.common.OOS_OIS;
import cn.edu.sustech.cs209.chatting.common.Users;


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

                        }
                        break;
                    case G_COMMAND://创建了一个群
//                        //将User.中的Map丰富
//                        Users.userGroup_socket_map.put(message.getSentBy(), socket);
//                        //刷新Gclient的userlist表，向所有连接的发送一个新的userlist
//                        System.out.println("G：发送新的userlist");
//                        System.out.println(Users.userGroup_socket_map);
//                        String userString = GgetUsers();
//                        for (Map.Entry<String, Socket> entry : Users.userGroup_socket_map.entrySet()) {
//                            os = new OOS_OIS.MyObjectOutputStream(entry.getValue().getOutputStream());
//                            os.writeObject(new Message(System.currentTimeMillis(), "Server", entry.getKey(), userString, MsgType.G_REQ));
//                            System.out.println("新的userlist返回成功");
//                            os.flush();
//                        }
                        break;
                    case TALK:
                        //用户发送信息给另一个用户
                        //如果另一个用户在线
//                        if (Users.one_to_one.containsKey(message.getSendTo())) {
                            //另一个用户打开着相对应用户的聊天界面
                            if (Users.one_to_one.get(message.getSendTo()).equals(message.getSentBy())) {
                                //直接给他发送消息
                                Users.user_user_messages.get(message.getSentBy()+"`"+message.getSendTo()).add(message);
                                Users.user_user_messages.get(message.getSendTo()+"`"+message.getSentBy()).add(message);

                                os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSendTo()).getOutputStream());
                                os.writeObject(message);
                                os.flush();
                            } else {
                                //将信息放进该用户的聊天里，等到后续该用户打开时，一次性加载出来
                                System.out.println("该用户在和其他用户聊天哦");
                                Users.user_user_messages.get(message.getSentBy()+"`"+message.getSendTo()).add(message);
                                Users.user_user_messages.get(message.getSendTo()+"`"+message.getSentBy()).add(message);
                            }
                        System.out.println("丰富了userusermessage给："+ message.getSentBy()+"`"+message.getSendTo());

//                        //如果另一个用户
//                        else {
//                            System.out.println("该用户还没开始聊天哦");
//                            Users.user_user_messages.get(message.getSentBy()+"`"+message.getSendTo()).add(message);
//                            Users.user_user_messages.get(message.getSendTo()+"`"+message.getSentBy()).add(message);
//                        }
                        //发送给特定的人
//                        os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSendTo()).getOutputStream());
//                        os.writeObject(message);
//                        os.flush();
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
                        System.out.println("收到客户端传来的改talkingto的信息,客户端为："+message.getSentBy());
                        //更新所有的onetooneMap
                        Users.one_to_one.put(message.getSentBy(), message.getSendTo());

                        String str = message.getSentBy()+"`"+message.getSendTo();//前面是发送方，后面是接收方
//                        //如果不存在
//                        if (!Users.user_user_messages.containsKey(message.getSentBy())) {
//                            //更新userusermsg
////                            Users.user_user_messages.put(message.getSentBy(), new HashMap<>());
////                            Users.user_user_messages.get(message.getSentBy()).put(message.getSendTo(), new ArrayList<>());
////                            Users.user_user_messages.put(message.getSendTo(), new HashMap<>());
////                            Users.user_user_messages.get(message.getSendTo()).put(message.getSentBy(), new ArrayList<>());
//
//
//                        }
//                        else if(Users.user_user_messages.containsKey(message.getSentBy())&&!Users.user_user_messages.containsKey(message.getSendTo())){
////                            Users.user_user_messages.put(message.getSendTo(),new HashMap<>());
////                            Users.user_user_messages.get(message.getSendTo()).put(message.getSentBy(),new ArrayList<>());
//                        }
//                        else {
////                            while (Users.user_user_messages.get(message.getSentBy()).get(message.getSendTo()).size() != 0) {
////                                os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSentBy()).getOutputStream());
////                                os.writeObject(Users.user_user_messages.get(message.getSentBy()).get(message.getSendTo()).get(0));
////                                os.flush();
////                                Users.user_user_messages.get(message.getSentBy()).get(message.getSendTo()).remove(0);
////                            }
//                        }
                        //完善聊天保存语句
                        //在这个map中的保存方式为：前对后说话
                        //如果此时map中还没有这个对话
//                        //先清空
//                        os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSentBy()).getOutputStream());
//                        os.writeObject(new Message(System.currentTimeMillis(),"Server",message.getSentBy(),"clear",MsgType.CLEAR));
                        if(!Users.user_user_messages.containsKey(str)){
                            System.out.println("str input = "+str);
                            Users.user_user_messages.put(str,new ArrayList<>());
                            Users.user_user_messages.put(message.getSendTo()+"`"+message.getSentBy(),new ArrayList<>());
                        }else {
                            //一个一个返回在存储的聊天记录
                            for(int i = 0;i<Users.user_user_messages.get(str).size();i++){
                                os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSentBy()).getOutputStream());
                                os.writeObject(Users.user_user_messages.get(str).get(i));
                                System.out.println("返回存储的聊天记录给"+message.getSentBy());
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
                        for (int i = 1; i < GUMStr.length; i++) {
                            gumStr.add(GUMStr[i]);
                        }
                        Users.group_users_map.put(GUMStr[0], gumStr);

                        //让与这个group有关的所有client也创建一个新的Gcontroller
                        for (String s : Users.group_users_map.get(GUMStr[0])) {
                            os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(s).getOutputStream());
                            os.writeObject(new Message(System.currentTimeMillis(), "Server", s, GUMStr[0], MsgType.G_CREATEGCONTROLLER));
                            os.flush();
                            System.out.println("创建信息返回clients");
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

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
