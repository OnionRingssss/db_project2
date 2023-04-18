package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


class ServerReader implements Runnable {

        private Socket socket;

        public ServerReader(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Message message = (Message) ois.readObject();
                System.out.println(message.getSentBy()+" told "+message.getSendTo()+":"+message.getData());

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

        public void userLogin(){

        }
    }
//}
