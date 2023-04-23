package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MsgType;
import cn.edu.sustech.cs209.chatting.common.Users;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));

        stage.setScene(new Scene(fxmlLoader.load()));
        Controller ctlr = fxmlLoader.getController();
        stage.setTitle("Chatting Client");
        //设置关闭事件
        stage.setOnCloseRequest(windowEvent -> {
            try {
                ctlr.moos.writeObject(new Message(System.currentTimeMillis(),ctlr.username,"Server","exit", MsgType.EXIT));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        stage.show();


    }
}
