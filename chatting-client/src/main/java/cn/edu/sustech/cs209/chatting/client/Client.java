package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MsgType;
import cn.edu.sustech.cs209.chatting.common.OOS_OIS;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

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
        Thread cr = new Thread(new ClientReader(socket, ois, os,this.controller));
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
    OOS_OIS.MyObjectOutputStream os;
    Controller controller;


    public ClientReader(Socket socket, OOS_OIS.MyObjectInputStream ois, OOS_OIS.MyObjectOutputStream os, Controller controller) {
        this.socket = socket;
        this.ois = ois;
        this.os = os;
        this.controller = controller;
    }

    @Override
    @FXML
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
                        System.out.println("从server回来的聊天记录message："+message.getData());
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
//                    case GROUP_CREATE://如果收到了来自server的群聊要求
//                        //创建群聊界面
//                        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("groupChatFX.fxml"));
//                        Platform.runLater(() -> {
//                            System.out.println("pola dsaf");
//                            Stage groupStage = new Stage();
////                            //给stage设置一个关闭监听
////                            groupStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
////                                @Override
////                                public void handle(WindowEvent windowEvent) {
////                                    //发送一个关闭的消息给server
////
////                                }
////                            });
//                            try {
//                                groupStage.setScene(new Scene(fxmlLoader.load()));
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                                GroupChatFX groupChatFX = fxmlLoader.getController();
//
//
//                            groupStage.setTitle(message.getData()+":"+message.getSendTo());
//                            groupStage.show();
//
//
//                        });
//                        break;
                    case G_TALK:
                        System.out.println("con.client_gcontroler map:"+controller.client_gcontroller_map);
                        this.controller.client_gcontroller_map.get(message.getSendTo()).GsetMsgLV(message);
                        break;
                    case G_CREATEGCONTROLLER:
                        this.controller.createNewGcontroller(message.getData()+":"+message.getSendTo(),message.getData());
                        System.out.println("client接受创建信息");

//                        //链接fxml文件
//                        Stage groupStage = new Stage();
//                        FXMLLoader loader = new FXMLLoader(getClass().getResource("groupChatFX.fxml"));
//                        Platform.runLater(()->{
//                            try {
//                                groupStage.setScene(new Scene(loader.load()));
//                                groupStage.setTitle(message.getData());
//                                groupStage.show();
//                            } catch (IOException ex) {
//                                ex.printStackTrace();
//                            }
//                        });

                        break;
//                    case CLEAR://清空
//                        controller.mesObservableList = FXCollections.observableArrayList();
//                        controller.chatContentList.setItems(controller.mesObservableList);
//                        break;
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
                }
//                } else {
//                    Scanner scanner = new Scanner(System.in);
//                    String data = scanner.nextLine();
//                    if (data.equals("exit")) {
//                        Message message = new Message(2320L, username, "Server", data, MsgType.TALK);
//                        os.writeObject(message);
//                        socket.getOutputStream().flush();
//                        Platform.exit();
//                    }
//                    Message message;
//                    if (username.equals("a")) {
//                        message = new Message(424234L, username, "b", data, MsgType.TALK);
//                    } else {
//                        message = new Message(424234L, username, "a", data, MsgType.TALK);
//                    }
//                    os.writeObject(message);
//                    System.out.println("第二步完成");
//                    socket.getOutputStream().flush();

//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
