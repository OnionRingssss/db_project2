package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MsgType;
import cn.edu.sustech.cs209.chatting.common.OOS_OIS;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class Gclient {
    private Socket socket;
    private String username;
    private OOS_OIS.MyObjectInputStream ois;
    private OOS_OIS.MyObjectOutputStream os;
    private GroupChatFX groupChatFX;


    Gclient(String username, GroupChatFX groupChatFX) throws IOException {
        socket = new Socket("localhost", 6666);
        this.username = username;
        this.groupChatFX = groupChatFX;
        ois = new OOS_OIS.MyObjectInputStream(this.socket.getInputStream());
        os = new OOS_OIS.MyObjectOutputStream(this.socket.getOutputStream());
        Thread gr = new Thread(new GReader(socket, ois, this.groupChatFX));
        gr.start();
        Thread gw = new Thread(new GWriter(socket, username, this.os, this.groupChatFX));
        gw.start();
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

    class GReader implements Runnable {
        private Socket socket;
        OOS_OIS.MyObjectInputStream ois;
        GroupChatFX groupChatFX;


        public GReader(Socket socket, OOS_OIS.MyObjectInputStream ois, GroupChatFX groupChatFX) {
            this.socket = socket;
            this.ois = ois;
            this.groupChatFX = groupChatFX;
        }

        @Override
        public void run() {
            try {
                System.out.println("Greader开始了");
//            OOS_OIS.MyObjectInputStream ois = new OOS_OIS.MyObjectInputStream(socket.getInputStream());
                while (true) {
                    Message message = (Message) ois.readObject();
                    //判断message的type
                    switch (message.getType()) {
                        case COMMAND:
                            break;
                        case G_TALK://收到server的消息
                            System.out.println("shou dao l ");
                            groupChatFX.GsetMsgLV(message);
//                            System.out.println(message.getData());
//                            g.setMsgLV(message);
                            break;
                        case G_REQ:
                            //完善每一个client的userset和此时连接上客户端的user数量
                            String [] reqString = message.getData().split("~");
                            groupChatFX.GuserSet.addAll(Arrays.asList(reqString));
                            break;
//                            String[] reqString = message.getData().split("~");
//                            String a = reqString[reqString.length - 1];
//                            String[] reqStrin = Arrays.copyOf(reqString, reqString.length - 1);
//                            controller.userSet.addAll(Arrays.asList(reqStrin));
//                            controller.setLeftLV(reqStrin);
////                        System.out.println("controlset:"+controller.userSet);
//                            //设置在线人数
//                            controller.setCuNum(a);

                        case GROUP_CREATE://如果收到了来自server的群聊要求
//                            //创建群聊界面
//                            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("groupChatFX.fxml"));
//                            Platform.runLater(() -> {
//                                Stage groupStage = new Stage();
//                                try {
//                                    groupStage.setScene(new Scene(fxmlLoader.load()));
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                groupStage.setTitle(message.getData());
//                                groupStage.show();
//                            });
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

    class GWriter implements Runnable {
        private Socket socket;
        String username;
        OOS_OIS.MyObjectOutputStream os;
        GroupChatFX groupChatFX;


        public GWriter(Socket socket, String username, OOS_OIS.MyObjectOutputStream os, GroupChatFX groupChatFX) {
            this.socket = socket;
            this.username = username;
            this.os = os;

            this.groupChatFX = groupChatFX;
        }

        @Override
        public void run() {
            try {
                System.out.println("线程w开始了");
//            os = new OOS_OIS.MyObjectOutputStream(socket.getOutputStream());
                int controlNum = 0;
                while (true) {
                    if (controlNum == 0) {
                        Message message = new Message(System.currentTimeMillis()
                                , username, "Server", "login", MsgType.G_COMMAND);
                        os.writeObject(message);
                        System.out.println("belongTo:"+username);
                        socket.getOutputStream().flush();
                        controlNum++;
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
