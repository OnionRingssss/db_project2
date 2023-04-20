package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MsgType;
import cn.edu.sustech.cs209.chatting.common.OOS_OIS;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private String username;
    private OOS_OIS.MyObjectInputStream ois;
    private OOS_OIS.MyObjectOutputStream os;
    private Controller controller;

    Client(String username, Controller controller) throws IOException {
        socket = new Socket("localhost", 6666);
        this.username = username;
        this.controller = controller;
        ois = new OOS_OIS.MyObjectInputStream(this.socket.getInputStream());
        os = new OOS_OIS.MyObjectOutputStream(this.socket.getOutputStream());
        Thread cw = new Thread(new ClientWriter(socket, username, os, this.controller));
        Thread cr = new Thread(new ClientReader(socket, ois, this.controller));
        cw.start();
        cr.start();
    }

    public OOS_OIS.MyObjectInputStream getOis() {
        return ois;
    }

    public OOS_OIS.MyObjectOutputStream getOs() {
        return os;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getUsername() {
        return username;
    }
}

class ClientReader implements Runnable {
    private Socket socket;
    OOS_OIS.MyObjectInputStream ois;
    Controller controller;


    public ClientReader(Socket socket, OOS_OIS.MyObjectInputStream ois, Controller controller) {
        this.socket = socket;
        this.ois = ois;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            System.out.println("线程r开始了");
//            OOS_OIS.MyObjectInputStream ois = new OOS_OIS.MyObjectInputStream(socket.getInputStream());
            while (true) {
                Message message = (Message) ois.readObject();
                //判断message的type
                switch (message.getType()) {
                    case COMMAND:
                        break;
                    case TALK:
                        System.out.println(message.getData());
                        controller.setMsgLV(message);
                        break;
                    case REQ:
//                        System.out.println(message.getData());
                        //完善每一个client的userset和此时连接上客户端的user数量
                        String[] reqString = message.getData().split("~");
                        String a = reqString[reqString.length - 1];
                        String[] reqStrin = Arrays.copyOf(reqString, reqString.length - 1);
                        controller.userSet.addAll(Arrays.asList(reqStrin));
                        controller.setLeftLV(reqStrin);
//                        System.out.println("controlset:"+controller.userSet);
                        //设置在线人数
                        controller.setCuNum(a);
                        break;
                    default:
                        break;
                }

            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

class ClientWriter implements Runnable {
    private Socket socket;
    String username;
    OOS_OIS.MyObjectOutputStream os;
    Controller controller;

    public ClientWriter(Socket socket, String username, OOS_OIS.MyObjectOutputStream os, Controller controller) {
        this.socket = socket;
        this.username = username;
        this.os = os;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            System.out.println("线程w开始了");
//            os = new OOS_OIS.MyObjectOutputStream(socket.getOutputStream());
            int controlNum = 0;
            while (true) {
                if (controlNum == 0) {
                    Message message = new Message(232L, username, "Server", "login", MsgType.COMMAND);
                    os.writeObject(message);
                    socket.getOutputStream().flush();
                    controlNum++;
                } else {
                    Scanner scanner = new Scanner(System.in);
                    String data = scanner.nextLine();
                    if (data.equals("exit")) {
                        Message message = new Message(2320L, username, "Server", data, MsgType.TALK);
                        os.writeObject(message);
                        socket.getOutputStream().flush();
                        Platform.exit();
                    }
                    Message message;
                    if (username.equals("a")) {
                        message = new Message(424234L, username, "b", data, MsgType.TALK);
                    } else {
                        message = new Message(424234L, username, "a", data, MsgType.TALK);
                    }
                    os.writeObject(message);
                    System.out.println("第二步完成");
                    socket.getOutputStream().flush();

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
