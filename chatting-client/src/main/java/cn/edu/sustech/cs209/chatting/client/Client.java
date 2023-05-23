package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MsgType;
import cn.edu.sustech.cs209.chatting.common.OOS_OIS;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;


import javax.sql.rowset.serial.SerialStruct;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class Client {
    private Socket socket;
    private String username;//user_id
    private String author_name;
    private String author_phone;
    private Timestamp author_time;
    private String type;
    private OOS_OIS.MyObjectInputStream ois;
    private OOS_OIS.MyObjectOutputStream os;
    private Controller controller;


    Client(String username, String author_name, String author_phone, Timestamp author_time, String type, Controller controller) throws IOException {
        socket = new Socket("localhost", 6666);
        this.username = username;
        this.author_name = author_name;
        this.author_phone = author_phone;
        this.author_time = author_time;
        this.type = type;
        this.controller = controller;
        ois = new OOS_OIS.MyObjectInputStream(this.socket.getInputStream());
        os = new OOS_OIS.MyObjectOutputStream(this.socket.getOutputStream());
        Thread cw = new Thread(new ClientWriter(socket, username, author_name, author_phone, author_time, type, os, this.controller));
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

                    case G_CREATEGCONTROLLER:
                        //group:username
                        this.controller.createNewGcontroller(message.getSentBy() + ":" + message.getSendTo(), message.getSentBy(), message.getData());
                        System.out.println("client接受创建信息");
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


                    case client_register_reject:
                        System.out.println("register reject, " + controller.username + " has existed");
                        pop_error("register reject, " + controller.username + "has existed", true);
                        break;
                    case client_register_success:
                        System.out.println("register successfully");
                        hotSearch_decode(message.getData());//热搜榜
                        break;
                    case client_login_reject:
                        System.out.println("login reject, wrong input");
                        pop_error("login reject, wrong input", true);
                        break;
                    case client_login_success:
                        System.out.println("login successfully");
                        hotSearch_decode(message.getData());
                        break;

                    case dian_zan:
                        if (message.getData().equals("fail")) {
                            pop_error("点赞失败，可能是id有误", false);
                        } else if (message.getData().equals("have")) {
                            pop_error("已经点过赞了", false);
                        } else {
                            pop_information("点赞成功");
                        }
                        break;
                    case shou_cang:
                        if (message.getData().equals("fail")) {
                            pop_error("收藏失败，可能是id有误", false);
                        } else if (message.getData().equals("have")) {
                            pop_error("已经收藏过了", false);
                        } else {
                            pop_information("收藏成功");
                        }
                        break;
                    case zhuan_fa:
                        if (message.getData().equals("fail")) {
                            pop_error("转发失败，可能是id有误", false);
                        } else if (message.getData().equals("have")) {
                            pop_error("已经转发过了", false);
                        } else {
                            pop_information("转发成功");
                        }
                        break;

                    case cha_kan_dian_zan:
                        System.out.println("点赞已显示");
                        showTextDialog("查看点赞", message.getData().split("~"));
                        break;
                    case cha_kan_shou_cang:
                        System.out.println("收藏已显示");
                        showTextDialog("查看收藏", message.getData().split("~"));
                        break;
                    case cha_kan_zhuan_fa:
                        System.out.println("转发已显示");
                        showTextDialog("查看转发", message.getData().split("~"));
                        break;

                    case guan_zhu:
                        if (message.getData().equals("fail")) {
                            pop_error("关注失败，可能是id有误", false);
                        } else if (message.getData().equals("have")) {
                            pop_error("已经关注", false);
                        } else {
                            pop_information("关注成功");
                        }
                        break;
                    case qu_xiao_guan_zhu:
                        if(message.getData().equals("fail")){
                            pop_error("取消关注失败，可能是id有误",false);
                        }else if(message.getData().equals("havent")){
                            pop_error("你还没有关注该帖子",false);
                        }else {
                            pop_information("取消关注成功");
                        }
                        break;

                    case cha_kan_guan_zhu:
                        System.out.println("关注已显示");
                        showTextDialog("查看关注",message.getData().split("~"));
                        break;

                    case fa_bu_tie_zi:
                        if(message.getData().equals("repetitive!!!")){
                            pop_error("你发布的帖子题目重复啦",false);
                        }else {
                            //调用controller中的方法，收集信息传给server,此处传递content
                            this.controller.getPostContent(message.getData(),message.getSentBy());
                        }
                        break;

                    case re_fa_bu_tie_zi_content:
                        String title = message.getSentBy().split("~~")[0];
                        String anony = message.getSentBy().split("~~")[1];
                        String content = message.getSendTo();
                        String[] cities = message.getData().split("``");
                        this.controller.getPostCity(title,content,cities,anony);
                        break;
                    case re_fa_bu_tie_zi_city:
                        int pid = Integer.parseInt(message.getSendTo());
                        String[] categories = message.getData().split("``");
                        this.controller.getPostCategories(pid,categories);
                        break;
                    case re_fa_bu_tie_zi_category:
                        pop_information("发布完成");
                        break;

                    case hui_fu_tie_zi:
                        if(message.getData().equals("success")){
                            pop_information("回复帖子成功");
                        }else {
                            pop_error("回复失败（id不正确）",false);
                        }
                        break;

                    case hui_fu_hui_fu:
                        if(message.getData().equals("success")){
                            pop_information("回复回复成功");
                        }else {
                            pop_error("回复回复失败（id不正确）",false);
                        }
                        break;


                    case cha_kan_ta_ren_fa_bu:
                        System.out.println("已查看他人: "+message.getSentBy()+"的发布");
                        showTextDialog("查看他人发布",message.getData().split("~~"));
                        break;

                    case cha_kan_fa_bu:
                        System.out.println("该用户发布的帖子已显示");
                        showTextDialog("查看发布",message.getData().split("~~"));
                        break;

                    case cha_kan_hui_fu:
                        System.out.println("该用户发布的回复已显示");
                        showTextDialog("查看回复",message.getData().split("~~"));
                        break;

                    case cha_kan_er_ji_hui_fu:
                        System.out.println("该用户发布的二级回复已显示");
                        showTextDialog("查看二级回复",message.getData().split("~~"));
                        break;


                    case multi_search:
                        showTextDialog("多参数搜索结果",message.getData().split("~~"));
                        break;

                    case ping_bi:
                        if(message.getData().equals("fail")){
                            pop_error("输入的author_id有误",false);
                        }else {
                            pop_information("屏蔽成功");
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

    private void hotSearch_decode(String str) {
        StringBuilder sb = new StringBuilder("热搜榜:\n");
        for(String s : str.split("~~")){
            sb.append(s).append("\n");
        }
        controller.hotSearchArea.setText(sb.toString());
    }

    private void pop_information(String s) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(s);
            alert.showAndWait();
        });
    }

    private void pop_error(String s, boolean exit) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(s);
            alert.showAndWait();
            if (exit) {
                System.exit(0);
            }
        });

    }

    private void showTextDialog(String m, String[] strings) {
        JFrame helper = new JFrame(m);
        StringBuilder helpText = new StringBuilder();
        for (String s : strings) {
            helpText.append(s).append("\n");
        }
        helper.setSize(800, 700);
        helper.setLocationRelativeTo(null);
        helper.setResizable(false);
        helper.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // 创建文本区域组件
        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);// 自动换行
        textArea.setEditable(false);
        textArea.setFont(new Font("楷体", Font.BOLD, 18));
        textArea.setText(helpText.toString());
        // 设置字体

        // 创建滚动面板, 指定滚动显示的视图组件(textArea), 垂直滚动条一直显示, 水平滚动条从不显示
        JScrollPane scrollPane = new JScrollPane(
                textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        helper.setContentPane(scrollPane);
        helper.setVisible(true);
    }
}

class ClientWriter implements Runnable {
    private Socket socket;
    String username;
    private String author_name;
    private String author_phone;
    private Timestamp author_time;
    private String type;
    OOS_OIS.MyObjectOutputStream os;
    Controller controller;


    public ClientWriter(Socket socket, String username, String author_name, String author_phone, Timestamp author_time, String type, OOS_OIS.MyObjectOutputStream os, Controller controller) {
        this.socket = socket;
        this.username = username;
        this.author_name = author_name;
        this.author_phone = author_phone;
        this.author_time = author_time;
        this.type = type;
        this.os = os;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            System.out.println("线程w开始了");
//            os = new OOS_OIS.MyObjectOutputStream(socket.getOutputStream());
            int controlNum = 0;
            while (controlNum == 0) {

                /**
                 * 输出的格式为：
                 * sentby： username也就是id，
                 * data：name~~~~phone~~~~time(String)~~~~type
                 */
                if (type.equals("login")) {
                    Message message = new Message(System.currentTimeMillis(),
                            username, "Server", loginSent(), MsgType.client_login);
                    os.writeObject(message);
                    socket.getOutputStream().flush();
                    controlNum++;
                } else if (type.equals("register")) {
                    Message message = new Message(System.currentTimeMillis(),
                            username, "Server", loginSent(), MsgType.client_register);
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

    private String loginSent() {
        StringBuilder sb = new StringBuilder();
        String strn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(this.author_time);
        sb.append(this.author_name).append("~~~~").append(this.author_phone).append("~~~~").append(strn);
        return sb.toString();
    }
}
