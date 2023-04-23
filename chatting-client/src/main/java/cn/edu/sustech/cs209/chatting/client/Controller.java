package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {

    public OOS_OIS.MyObjectOutputStream moos;
    public Map<String, GroupChatFX> group_gcontroller_map = new HashMap<>();
    public Map<String, GroupChatFX> client_gcontroller_map = new HashMap<>();
    public Set<String> userSet = new HashSet<>();
    ObservableList<String> stringObservableList;
    ObservableList<Message> mesObservableList = FXCollections.observableArrayList();

    @FXML
    private TextArea inputArea;

    @FXML
    ListView<Message> chatContentList;


    String username;
    String password;

    @FXML
    private Label currentUsername;
    @FXML
    private Label talkWith;
    private String talkTo = null;

    @FXML
    public ListView<String> chatList;

    @FXML
    public Label currentOnlineCnt;

    @FXML
    public Button emojiBtn;

    @FXML
    public Button fileBtn;

    private static final String beginPath = "C:\\Users\\y1211\\Desktop\\java2_assignment\\CS029A_assignment2\\fileSender";
    private static final String outPath = "C:\\Users\\y1211\\Desktop\\java2_assignment\\CS029A_assignment2\\fileReceiver";

    String registerOr;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Dialog<String> dialog1 = new TextInputDialog();
        dialog1.setTitle("Login-username");
        dialog1.setHeaderText(null);
        dialog1.setContentText("Username:");
        Optional<String> input1 = dialog1.showAndWait();

//        Dialog<String> dialog2 = new TextInputDialog();
//        dialog2.setTitle("Login-password");
//        dialog2.setHeaderText(null);
//        dialog2.setContentText("Password:");
//        Optional<String> input2 = dialog2.showAndWait();


        if (input1.isPresent() && !input1.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            boolean conti = true;
            RLStageOperate();
            username = input1.get();
//            password = input2.get();
            setCurrentUsername(username);
            try {
                //创建一个client，以及其中的读写线程
                Client client = new Client(username, this);
                moos = client.getOs();
                //给提示：
                Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
                alert1.setTitle("Information Dialog");
                alert1.setHeaderText(null);
                alert1.setContentText("请先点击一位朋友表示您已经准备好进入聊天状态，可以接受他人的当前及后台聊天信息\n若不确认，默认进入勿扰状态，不会收到他人的后台消息");
                alert1.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("usm=:" + Users.user_socket_map);


            emojiBtn.setOnAction(actionEvent -> {
                //弹出一个选择弹窗，选择想要的表情
                List<String> choices = new ArrayList<>();
                choices.add("\uD83D\uDE04");
                choices.add("\uD83D\uDE38");
                choices.add("\uD83D\uDE1F");

                ChoiceDialog<String> dialog = new ChoiceDialog<>("\uD83D\uDE04", choices);
                dialog.setTitle("Choice Dialog");
                dialog.setHeaderText("Look, a Choice Dialog");
                dialog.setContentText("Choose your emoji: ");

                Optional<String> result = dialog.showAndWait();
                //添加到输入中去
                try {
                    System.out.println(result.get());
                    inputArea.setText(inputArea.getText() + result.get());
                } catch (NoSuchElementException e) {
                    System.out.println("没有选择emoji,直接关掉选择器了");
                }
            });

            //设置发送文件按钮的监听
            fileBtn.setOnAction(actionEvent -> {
                //将文件转换成byte[]再转换成String
                try {
                    JFileChooser jf = new JFileChooser(beginPath);
                    jf.setFileSelectionMode(JFileChooser.FILES_ONLY);//只选择文件
                    jf.showOpenDialog(null);
                    File selectedFile = jf.getSelectedFile();
                    String fileName = selectedFile.getName();//获得文件名
                    FileInputStream fileInputStream = new FileInputStream(selectedFile);
                    byte[] fileInByte = new byte[(int) selectedFile.length()];
                    fileInputStream.read(fileInByte);
                    fileInputStream.close();
                    StringBuilder sb = new StringBuilder();
                    sb.append(fileName).append("---divide---");
                    for (byte b : fileInByte) {
                        sb.append(b);
                    }
                    //发送给客户端
                    moos.writeObject(new Message(System.currentTimeMillis(), username, talkTo, sb.toString(), MsgType.FILE));
                    moos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }catch (NullPointerException e){
                    System.out.println("no file");
                }


            });
        } else {
            System.out.println("Empty username of password!");
            Platform.exit();
        }
        String displayTalkTo = "talking to: " + talkTo;
        talkWith.setText(displayTalkTo);

        chatList.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                System.out.println(chatList.getSelectionModel().getSelectedItem().getClass());
                System.out.println(chatList.getItems().getClass());
                talkTo = chatList.getSelectionModel().getSelectedItem();
                privateChatHelper();
            }
        });

        chatContentList.setCellFactory(new MessageCellFactory());
        chatContentList.setItems(mesObservableList);
    }

    @FXML
    public void createPrivateChat() {

        AtomicReference<String> user = new AtomicReference<>();

        //该stage为弹出供我们选择私聊对象的stage
        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        //将userset写入usersel
        for (String s : userSet) {
            if (!s.equals(username)) userSel.getItems().add(s);
        }

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            //将选中的聊天对象设置为 talkto
            talkTo = userSel.getSelectionModel().getSelectedItem();
            privateChatHelper();
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

//        // TODO: if the current user already chatted with the selected user, just open the chat with that user
//        if (user.get().equals(talkTo)) {
//
//        }
//        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
//        else {
//        }
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
        if (userSet.size() >= 3) {
            Stage GroupChatChooserStage = new Stage();
            List<CheckBox> chosenUser = new ArrayList<>(); //被选中的user(CheckBox)
            List<String> chosenUser_String = new ArrayList<>();//被选中的user(String)
            Label label = new Label("choose some friend and begin your group chat! ");
            VBox under_vbox = new VBox();
            VBox upper_vbox = new VBox();
            HBox hBox = new HBox();
            Label label1 = new Label("set a name for your group: ");
            TextField textField = new TextField();
            hBox.getChildren().addAll(label1, textField);
            //可选择的用户不包含当前用户，但是在创建完成群后要将当前用户加进去
            for (String s : userSet) {
                if (!s.equals(username)) {
                    chosenUser.add(new CheckBox(s));
//                    chosenUser_String.add(s);
                }
            }
            for (CheckBox checkBox : chosenUser) {
                checkBox.selectedProperty().addListener((observableValue, aBoolean, t1) -> chosenUser_String.add(checkBox.getText()));
            }
            //确定创建群聊的button
            Button okBtn = new Button("OK");
            okBtn.setOnAction(e -> {

//            //创建一个新的Gcontroller，并将他加入到所在controller的list里面
//            createNewGcontroller(textField.getText()+":"+username,textField.getText()+":"+username);
                //将这个新的Gcontroller加入到Users中,向server发送信息
                try {
                    moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", group_create_helper(textField.getText(), chosenUser_String), MsgType.GROUP_CREATE));
                    moos.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.out.println("创建信息从clinet发送到server");

                GroupChatChooserStage.close();
            });
            upper_vbox.getChildren().addAll(chosenUser);
            under_vbox.getChildren().addAll(hBox, label, upper_vbox, okBtn);
            under_vbox.setAlignment(Pos.CENTER);
            under_vbox.setPadding(new Insets(20, 20, 20, 20));
            GroupChatChooserStage.setScene(new Scene(under_vbox));
            GroupChatChooserStage.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("only 2 people");
            alert.setHeaderText("only 2 people!");
            alert.setContentText("we cannot create a group with no more than 2 people");
            alert.showAndWait();
        }
    }

    public void createNewGcontroller(String s, String s2, String s3) {

        Platform.runLater(() -> {
            Stage groupStage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("groupChatFX.fxml"));//链接fxml文件
            try {
                groupStage.setScene(new Scene(loader.load()));
                GroupChatFX groupChatFX = loader.getController();
                //更新一下该controller的表
                this.client_gcontroller_map.put(s, groupChatFX);
                this.group_gcontroller_map.put(s.split(":")[0], groupChatFX);
                //让这个groupChatFX的userset完善
                String[] userStringList = s3.split("~");
                groupChatFX.GuserSet.addAll(Arrays.asList(userStringList));
                groupChatFX.setGusernameLl(Integer.toString(userStringList.length));
                groupChatFX.onlineUserInGroupList = FXCollections.observableArrayList(Arrays.asList(userStringList));
                groupChatFX.GchatList.setItems(groupChatFX.onlineUserInGroupList);

//                this.group_gcontroller_map.put(s.split(":")[0],groupChatFX);
//                moos.writeObject(new Message(System.currentTimeMillis(),s.split(":")[0],"Server","group left lv",MsgType.G_LEFT_LV));
//                moos.flush();
                groupStage.setTitle(s2);
                groupChatFX.setGroupname(s, this.moos);
                groupStage.setOnCloseRequest(windowEvent -> {
                    //发送给server信息，将该用户从group中踢出去,sentby是同时包含了group的名字和username
                    try {
                        moos.writeObject(new Message(System.currentTimeMillis(), s, "Server", "exit the group", MsgType.EXIT_FROM_GROUP));
                        moos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                groupStage.show();
                System.out.println(client_gcontroller_map);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void deleteChatOb(String str, String string) {
        client_gcontroller_map.get(str).GsetLeftLV(string);
    }

    //返回值：第一个是Group的名字，后面是他所有的用户
    private String group_create_helper(String str, List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("~");
        for (String s : list) {
            stringBuilder.append(s).append("~");
        }
        return stringBuilder.append(username).toString();
    }


    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() throws IOException {
        // TODO
        //发送一个信息给server
        if (inputArea.getText() != null) {
            String inputFromKeyBoard = inputArea.getText();
            //清空原本的内容
            inputArea.setText("");
            //将message传给server
            Message message = new Message(System.currentTimeMillis(), username, talkTo, inputFromKeyBoard, MsgType.TALK);
            moos.writeObject(message);
            moos.flush();
            //加入自己的message显示中
            Platform.runLater(() -> {
                mesObservableList.add(message);
                System.out.println(mesObservableList);
                chatContentList.setItems(mesObservableList);
            });
        }

    }

    public void receiveFile(String sender, String fileName, byte[] bytes) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Dialog");
            alert.setHeaderText(sender+" send you a file");
            alert.setContentText(sender+" send you a file, do you want to receive it?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                // ... user chose OK
                JFileChooser jFileChooser = new JFileChooser(outPath);
                jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jFileChooser.showOpenDialog(null);
                String selectedPath = jFileChooser.getSelectedFile().getAbsolutePath();
                bytesToFile(bytes, selectedPath, fileName);
            } else {
                System.out.println(userSet+" refuse the file sent by "+sender);
            }

        });

    }

    private File bytesToFile(byte[] bytes, String outPath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(outPath);
            if (!dir.exists() && dir.isDirectory()) { //判断文件目录是否存在
                dir.mkdirs();
            }
            file = new File(outPath + File.separator + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return file;
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
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

                    if (username.equals(msg.getSentBy())) {
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

    public void ntAllowLoginFeedBk() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("only 2 people");
            alert.setHeaderText("Invalid message");
            alert.setContentText("You entered an invalid username\nplease enter again later");
            alert.showAndWait();
            System.exit(0);
        });
    }

    public void r_fail() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("repetitive username");
            alert.setHeaderText("repetitive username");
            alert.setContentText("repetitive username : try another username!");
            alert.showAndWait();
            System.exit(0);
        });
    }

    public void addEmoji() {
        VBox vbox = new VBox(); // 创建一个垂直箱子
        HBox hbox = new HBox(); // 创建一个水平箱子
        RadioButton rb1 = new RadioButton("笑"); // 创建一个单选按钮
        rb1.setSelected(true); // 设置按钮是否选中
        RadioButton rb2 = new RadioButton("哭"); // 创建一个单选按钮
        RadioButton rb3 = new RadioButton("黑椒牛排饭"); // 创建一个单选按钮
        hbox.getChildren().addAll(rb1, rb2, rb3); // 把三个单选按钮一起加到水平箱子上
        ToggleGroup group = new ToggleGroup(); // 创建一个按钮小组
        rb1.setToggleGroup(group); // 把单选按钮1加入到按钮小组
        rb2.setToggleGroup(group); // 把单选按钮2加入到按钮小组
        rb3.setToggleGroup(group); // 把单选按钮3加入到按钮小组
        Label label = new Label("这里查看点餐结果"); // 创建一个标签
        label.setWrapText(true); // 设置标签文本是否支持自动换行
        vbox.getChildren().addAll(hbox, label); // 把水平箱子和标签一起加到垂直箱子上
        // 设置单选组合的单击监听器
        group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> arg0, Toggle old_toggle, Toggle new_toggle) {
                // 在标签上显示当前选中的单选按钮文本
                label.setText("您点了" + ((RadioButton) new_toggle).getText());
            }
        });

    }

    //左下角当前用户显示
    public void setCurrentUsername(String name) {
        currentUsername.setText("Current User: " + name);
    }

    //在线人数显示
    public void setCuNum(String a) {
        Platform.runLater(() -> currentOnlineCnt.setText("Online:" + a));
    }

    //设置好左侧聊天对象栏
    public void setLeftLV(String[] string) {
        Platform.runLater(() -> {
            ArrayList<String> str = new ArrayList<>();
            for (String s : string) {
                if (!s.equals(username)) {
                    str.add(s);
                }
            }
            String[] sss = new String[str.size()];
            for (int i = 0; i < str.size(); i++) {
                sss[i] = str.get(i);
            }
            stringObservableList = FXCollections.observableArrayList(Arrays.asList(sss));
            chatList.setItems(stringObservableList);
        });
    }

    //用于更新聊天内容
    public void setMsgLV(Message message) {
        Platform.runLater(() -> {
            mesObservableList.add(message);
            chatContentList.setItems(mesObservableList);
            System.out.println("更新聊天内容了");
        });
    }

    //用于在切换聊天对象时重新刷新聊天
    public void reWriteMsgLV() {
        Platform.runLater(() -> {
            mesObservableList = FXCollections.observableArrayList();
            chatContentList.setItems(mesObservableList);
        });
    }

    public void privateChatHelper() {
        //发送给server信息，告诉server该客户端talkTo的对象
        try {
            moos.writeObject(new Message(System.currentTimeMillis(), username, talkTo, "talkingTo", MsgType.TALKINGTO));
            System.out.println("talking to success");
            reWriteMsgLV();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        talkWith.setText("talking to: " + talkTo);
    }


    public void Pop_window(String s) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Someone quit: " + s);
            alert.setHeaderText("Do you wanna keep the chat stage with " + s);
            alert.setContentText("Choose your option");

            ButtonType buttonTypeOne = new ButtonType("Yes");
            ButtonType buttonTypeTwo = new ButtonType("No");
            ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeTwo, buttonTypeCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == buttonTypeOne) {
                alert.close();
            } else if (result.get() == buttonTypeTwo) {
                // ... user chose "Two"
                //发送消息告诉我不要了
                try {
                    moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", "no keep", MsgType.EXIT_NO_KEEP));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                alert.close();
            }
        });

    }

    public void RLStageOperate() {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            VBox vbox = new VBox(); // 创建一个垂直箱子
            HBox hbox = new HBox(); // 创建一个水平箱子
            Button rb1 = new Button("Register"); // 创建一个单选按钮
            rb1.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    registerOr = "register";
                    stage.close();
                }
            });
//        rb1.setSelected(true); // 设置按钮是否选中
            Button rb2 = new Button("Login"); // 创建一个单选按钮
            rb2.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    registerOr = "login";
                    stage.close();
                }
            });
            hbox.getChildren().addAll(rb1, rb2); // 把三个单选按钮一起加到水平箱子上
            ToggleGroup group = new ToggleGroup(); // 创建一个按钮小组
            Label label = new Label("第一次进入请选择“Register”"); // 创建一个标签
            label.setWrapText(true); // 设置标签文本是否支持自动换行
            vbox.getChildren().addAll(label, hbox); // 把水平箱子和标签一起加到垂直箱子上
            stage.setScene(new Scene(vbox));
        });

    }
}
