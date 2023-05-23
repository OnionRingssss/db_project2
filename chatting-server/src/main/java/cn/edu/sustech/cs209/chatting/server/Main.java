package cn.edu.sustech.cs209.chatting.server;




import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MsgType;
import cn.edu.sustech.cs209.chatting.common.OOS_OIS;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
//    public static Map<String, Socket> usersocket_map = new ConcurrentHashMap<>();
    public static Set<OOS_OIS.MyObjectOutputStream> oosSet = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Starting server");
//        Thread exitThread = new Thread(() -> {
//            Scanner inputExit = new Scanner(System.in);
//            while (inputExit.hasNext()){
//                String i = inputExit.next();
//                if(i.equals("exit")){
//                    System.out.println("server exit");
//                    for(OOS_OIS.MyObjectOutputStream o :oosSet){
//                        try {
//                            o.writeObject(new Message(System.currentTimeMillis(),"Server","Server","server exit",MsgType.SERVER_EXIT));
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        });
//        exitThread.start();

        try {
            //1.创建服务器端的ServerSocket对象,等待客户端连接
            ServerSocket serverSocket = new ServerSocket(6666);
            //2.创建线程池,从而可以处理多个客户端
            ExecutorService executorService = Executors.newFixedThreadPool(100);
            for (int i = 1; i < 100; i++) {
                System.out.println("欢迎来到我的聊天室......");
                //3.侦听客户端
                Socket socket = serverSocket.accept();
                System.out.println("有新的朋友加入.....");
//                oosSet.add(new OOS_OIS.MyObjectOutputStream(socket.getOutputStream()));
                //4.启动线程
                executorService.execute(new ServerReader(socket));
            }
            //5.关闭线程池
            executorService.shutdown();
            //6.关闭服务器
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
