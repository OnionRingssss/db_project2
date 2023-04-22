package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MsgType;
import cn.edu.sustech.cs209.chatting.common.OOS_OIS;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class GroupChatFX implements Initializable {

    @FXML
    private TextArea GinputArea;
    @FXML
    ListView<Message> GchatContentList;
    @FXML
    ListView<String> GchatList;
    @FXML
    public Label GcurrentOnlineCnt;
    @FXML
    private VBox rootLout;
    @FXML
    private Label GusernameLl;



    String groupname;

    public OOS_OIS.MyObjectOutputStream Gmoos;
    public Set<String> GuserSet = new HashSet<>();
    ObservableList<Message> messageObservableList = FXCollections.observableArrayList();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
//        Dialog<String> dialog1 = new TextInputDialog();
//        dialog1.setTitle("Group_Name_Set");
//        dialog1.setHeaderText(null);
//        dialog1.setContentText("自定义群聊名称:");
//        Optional<String> input1 = dialog1.showAndWait();
//
//        Dialog<String> dialog = new TextInputDialog();
//        dialog.setTitle("Group Login");
//        dialog.setHeaderText(null);
//        dialog.setContentText("自定义你在群里的id:");
//        Optional<String> input = dialog.showAndWait();
//        //这里可以加个判断
//        username = input.get();
//        belongTo = input1.get()+":"+username;

//        try {
//            Gclient gclient = new Gclient(belongTo, this);
//            Gmoos = gclient.getOs();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        GchatContentList.setCellFactory(new MessageCellFactory());
        GchatContentList.setItems(messageObservableList);
        System.out.println("groupname "+groupname);
    }


    public void setGroupname(String s,OOS_OIS.MyObjectOutputStream moos){
       this.groupname = s;
       this.Gmoos = moos;
    }

    @FXML
    public void doSendMessage() throws IOException {
        //发送一个信息给server
        String inputFromKeyboard = GinputArea.getText();
        //清空
        GinputArea.setText("");
        //message传给server
        //groupname中，”：“前是group的名字，后面是username，可以用username来确定client
        Message message = new Message(System.currentTimeMillis(), groupname, "Server", inputFromKeyboard, MsgType.G_TALK);
        Gmoos.writeObject(message);
        System.out.println("message成功传给server");
        Gmoos.flush();

//        //加入到自己的message显示中
//        Platform.runLater(() -> {
//            messageObservableList.add(message);
//            System.out.println("mesOb" + messageObservableList);
//            GchatContentList.setItems(messageObservableList);
//        });
    }
    public void GsetMsgLV(Message message){
        Platform.runLater(() -> {
            messageObservableList.add(message);
            GchatContentList.setItems(messageObservableList);
            System.out.println("已经加入到自己的显示中");
        });
    }

    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        //阻止切换聊天对象时出现bug
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (groupname.split(":")[1].equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }
}
