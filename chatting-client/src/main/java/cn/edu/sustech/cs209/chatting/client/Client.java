package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MsgType;
import cn.edu.sustech.cs209.chatting.common.OOS_OIS;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Optional;

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
        Thread cr = new Thread(new ClientReader(socket, ois, os, this.controller));
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
                        System.out.println("从server回来的聊天记录message：" + message.getData());
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

                    case G_CREATEGCONTROLLER:
                        //group:username
                        this.controller.createNewGcontroller(message.getSentBy() + ":" + message.getSendTo(), message.getSentBy(),message.getData());
                        System.out.println("client接受创建信息");
                        break;

                    case EXIT:
                        //收到信息后，重新设置一下
                        controller.setCuNum(message.getData());
                        //设置一个弹窗，选择保留或者是清除退出的user
                        controller.Pop_window(message.getSentBy());
                        break;

                    case EXIT_NO_KEEP:
                        String[] reqString1 = message.getData().split("~");
                        String a1 = reqString1[reqString1.length - 1];
                        String[] reqStrin1 = Arrays.copyOf(reqString1, reqString1.length - 1);
                        controller.userSet.remove(message.getSentBy());
                        controller.setLeftLV(reqStrin1);
                        controller.setCuNum(a1);
                        break;

                    case G_TALK:
                        System.out.println("con.client_gcontroler map:" + controller.client_gcontroller_map);
                        this.controller.client_gcontroller_map.get(message.getSendTo()).GsetMsgLV(message);
                        break;



                    case SERVER_EXIT:
                        Platform.runLater(() -> {
                            Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
                            alert1.setTitle("Information Dialog");
                            alert1.setHeaderText("Server exit");
                            alert1.setContentText("Server exit! You will exit in several seconds···");
                            alert1.showAndWait();
                            try {
                                os.writeObject(new Message(System.currentTimeMillis(), controller.username, "Server", "confirm server exit", MsgType.SERVER_EXIT));
                                os.flush();
                                System.exit(0);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        break;
                    case NOT_ALLOW_LOGIN:
                        controller.ntAllowLoginFeedBk();
                        break;
                    case R_FIAL:
                        controller.r_fail();
                        break;
                    case EXIT_FROM_GROUP:
                        String group = message.getSendTo().split(":")[0];
                        String deleteUsername = message.getSendTo().split(":")[1];
                        controller.deleteChatOb(message.getSendTo(),message.getData());
                        break;
                    case FILE:
                        //将byte[]转化回文件
//                        String fileName = message.getData().split("---divide---")[0];
                        String fileString = message.getData();
                        byte[] bytes = fileString.getBytes();
                        //让controller弹出一个弹窗，确定是否接受
                        controller.receiveFile(message.getSentBy(),bytes,message);
                        System.out.println("客户端收到file消息，给出弹窗");
                        break;

                    default:
                        break;
                }

            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

//    private File bytesToFile(byte[] bytes, String outPath, String fileName) {
//        BufferedOutputStream bos = null;
//        FileOutputStream fos = null;
//        File file = null;
//        try {
//            File dir = new File(outPath);
//            if (!dir.exists() && dir.isDirectory()) { //判断文件目录是否存在
//                dir.mkdirs();
//            }
//            file = new File(outPath + File.separator + fileName);
//            fos = new FileOutputStream(file);
//            bos = new BufferedOutputStream(fos);
//            bos.write(bytes);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (bos != null) {
//                try {
//                    bos.close();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//            }
//            if (fos != null) {
//                try {
//                    fos.close();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//            }
//        }
//        return file;
//    }
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
                    Message message = new Message(System.currentTimeMillis(),
                            username, "Server", "login",MsgType.COMMAND);
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
