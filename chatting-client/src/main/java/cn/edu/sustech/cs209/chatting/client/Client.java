package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private String username;

    Client(String username) throws IOException {
        socket = new Socket("localhost",6666);
        this.username= username;
        Thread cw = new Thread(new ClientWriter(socket,username));
        Thread cr = new Thread(new ClientReader(socket));
        cw.start();
        cr.start();
    }

    public Socket getSocket() {
        return socket;
    }
    public String getUsername(){
        return username;
    }
}

class ClientReader implements Runnable{
    private Socket socket;

    public ClientReader(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            System.out.println("线程r开始了" );
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Message message = (Message) ois.readObject();
            System.out.println(message.getData());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
class ClientWriter implements Runnable{
    private Socket socket;
    String username;

    public ClientWriter(Socket socket,String username) {
        this.socket = socket;
        this.username = username;
    }

    @Override
    public void run() {
        try {
            System.out.println("线程w开始了");
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            Scanner scanner = new Scanner(System.in);
            String data = scanner.nextLine();
            Message message = new Message(424234L,username,"me",data);
            os.writeObject(message);
            socket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
