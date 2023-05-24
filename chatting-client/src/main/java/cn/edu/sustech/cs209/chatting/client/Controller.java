package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller implements Initializable {

    public OOS_OIS.MyObjectOutputStream moos;
    public Map<String, GroupChatFX> group_gcontroller_map = new HashMap<>();
    public Map<String, GroupChatFX> client_gcontroller_map = new HashMap<>();


    String username;//author_id
    String user_author_name;//author_name
    String user_author_phone;//author_phone
    Timestamp user_author_time;//author_time

    /**
     * 在javafx中的组件
     */
    @FXML
    public javafx.scene.control.TextArea hotSearchArea;
    @FXML
    public javafx.scene.control.Label idShower;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
//    Dialog<String> dialog1 = new TextInputDialog();
//    dialog1.setTitle("Login-author_id");
//    dialog1.setHeaderText(null);
//    dialog1.setContentText("Author_id:");
//    Optional<String> input1 = dialog1.showAndWait();

//    Dialog<Pair<String,String>> dialog2 = new Dialog<>();
//    dialog2.setTitle("Login-author_name and author_phone");
//    dialog2.setHeaderText(null);
//    dialog2.setContentText();

        /**
         * create a popup window, get id,name,phone
         */
        // Create the custom dialog.
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login Dialog");
        dialog.setHeaderText("Login");

// Set the button types.
        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        ButtonType visitorButtonType = new ButtonType("Visitor", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(visitorButtonType, registerButtonType, loginButtonType, ButtonType.CANCEL);

// Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField user_author_id_Field = new TextField();
        user_author_id_Field.setPromptText("author_id");
        TextField user_author_name_Field = new TextField();
        user_author_name_Field.setPromptText("author_name");
        TextField user_author_phone_Field = new TextField();
        user_author_phone_Field.setPromptText("author_phone");

        grid.add(new Label("author_id:"), 0, 0);
        grid.add(user_author_id_Field, 1, 0);
        grid.add(new Label("author_name:"), 0, 1);
        grid.add(user_author_name_Field, 1, 1);
        grid.add(new Label("author:phone:"), 0, 2);
        grid.add(user_author_phone_Field, 1, 2);

// Enable/Disable login button depending on whether a username was entered.
        Node registerbutton = dialog.getDialogPane().lookupButton(registerButtonType);
        Node loginbutton = dialog.getDialogPane().lookupButton(loginButtonType);
        Node visitorbutton = dialog.getDialogPane().lookupButton(visitorButtonType);
        registerbutton.setDisable(true);
        loginbutton.setDisable(true);
        visitorbutton.setDisable(true);

// Do some validation (using the Java 8 lambda syntax).
        user_author_id_Field.textProperty().addListener((observable, oldValue, newValue) -> {
            loginbutton.setDisable(newValue.trim().isEmpty());
            registerbutton.setDisable(newValue.trim().isEmpty());
            visitorbutton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

// Request focus on the username field by default.
        Platform.runLater(() -> user_author_id_Field.requestFocus());

// Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                Map<String, String> result = new HashMap<>();
                result.put("id", user_author_id_Field.getText());
                result.put("name", user_author_name_Field.getText());
                result.put("phone", user_author_phone_Field.getText());
                result.put("type", "login");
                return result;
            } else if (dialogButton == registerButtonType) {
                Map<String, String> result = new HashMap<>();
                result.put("id", user_author_id_Field.getText());
                result.put("name", user_author_name_Field.getText());
                result.put("phone", user_author_phone_Field.getText());
                result.put("type", "register");
                return result;
            } else if (dialogButton == visitorButtonType) {
                Map<String, String> result = new HashMap<>();
                result.put("id", "you_ke");
                result.put("name", "you_ke");
                result.put("phone", "you_ke");
                result.put("type", "visitor");
                return result;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(value -> {
            System.out.println("id: " + value.get("id"));
            System.out.println("name: " + value.get("name"));
            System.out.println("phone: " + value.get("phone"));
            System.out.println("type: " + value.get("type"));
        });

//    if (input1.isPresent() && !input1.get().isEmpty()) {
        if (result.isPresent() && !result.get().isEmpty() && result.get().get("id") != null && result.get().get("name") != null && result.get().get("phone") != null) {
            username = result.get().get("id");
            user_author_name = result.get().get("name");
            user_author_phone = result.get().get("phone");
            user_author_time = new Timestamp(System.currentTimeMillis());
            try {
                //创建一个client，以及其中的读写线程
                Client client = new Client(username, user_author_name, user_author_phone, user_author_time,
                        result.get().get("type"), this);
                moos = client.getOs();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            pop_error("你已经取消登录/注册", true);
            System.out.println("Empty username of password!");
//            Platform.exit();
        }

    }


    /**
     * 组件的相应方法
     */

    //这个方法创造一个可以输入的框，返回输入的内容,如果没输入就是null
    public String textInDialog(String notice) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(notice);
        dialog.setHeaderText("Please input id: ");
        dialog.setContentText(notice);

        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    @FXML //点赞
    public void click_dianZan() throws IOException {
        if (!username.equals("you_ke")) {
            String str = textInDialog("输入你想点赞的帖子id：");
            moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", str, MsgType.dian_zan));
            moos.flush();
        } else {
            pop_error("现在是游客模式，无法点赞", false);
        }
    }

    @FXML //收藏
    public void click_shouCang() throws IOException {
        if (!username.equals("you_ke")) {
            String str = textInDialog("输入你想收藏的帖子id： ");
            moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", str, MsgType.shou_cang));
            moos.flush();
        } else {
            pop_error("现在是游客模式，无法收藏", false);
        }
    }

    @FXML //转发
    public void click_zhuanFa() throws IOException {
        if (!username.equals("you_ke")) {
            String str = textInDialog("输入你想转发的帖子id： ");
            moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", str, MsgType.zhuan_fa));
            moos.flush();
        } else {
            pop_error("现在是游客模式，无法转发", false);
        }
    }

    @FXML //查看点赞
    public void click_chaKanDianZan() throws IOException {
        if (!username.equals("you_ke")) {
            moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", username, MsgType.cha_kan_dian_zan));
            moos.flush();
        } else {
            pop_error("现在是游客模式，你没有任何点赞记录", false);
        }
    }

    @FXML //查看收藏
    public void click_chaKanShouCang() throws IOException {
        if (!username.equals("you_ke")) {
            moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", username, MsgType.cha_kan_shou_cang));
            moos.flush();
        } else {
            pop_error("现在是游客模式，你没有任何收藏记录", false);
        }
    }

    @FXML //查看转发
    public void click_chaKanZhuanFa() throws IOException {
        if (!username.equals("you_ke")) {
            moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", username, MsgType.cha_kan_zhuan_fa));
            moos.flush();
        } else {
            pop_error("现在是游客模式，你没有任何转发记录", false);
        }
    }

    @FXML // 关注
    public void click_guanZhu() throws IOException {
        if (!username.equals("you_ke")) {
            String str = textInDialog("输入你想关注的作者id： ");
            moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", str, MsgType.guan_zhu));
            moos.flush();
        } else {
            pop_error("现在是游客模式，无法关注", false);
        }
    }

    @FXML //取消关注
    public void click_quXiaoGuanZhu() throws IOException {
        if (!username.equals("you_ke")) {
            String str = textInDialog("输入你想取消关注的作者id： ");
            moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", str, MsgType.qu_xiao_guan_zhu));
            moos.flush();
        } else {
            pop_error("现在是游客模式，你没有关注记录", false);
        }
    }

    @FXML //查看关注
    public void click_chaKanGuanZhu() throws IOException {
        if (!username.equals("you_ke")) {
            moos.writeObject(new Message(System.currentTimeMillis(), username, "Server", username, MsgType.cha_kan_guan_zhu));
            moos.flush();
        } else {
            pop_error("现在是游客模式，你没有关注记录", false);
        }
    }

    @FXML //发布帖子
    public void click_faBuTieZi() throws IOException {
        if (!username.equals("you_ke")) {
            String str = textInDialog("输入你想发布的帖子的标题（title）");
            String anony = choiceAnonyDialog();
            if (anony == null) {
                pop_error("请再次尝试", false);
            } else {
                moos.writeObject(new Message(System.currentTimeMillis(), username, anony, str, MsgType.fa_bu_tie_zi));
                moos.flush();
            }
        } else {
            pop_error("现在是游客模式，无法发布帖子", false);
        }
    }


    @FXML //回复帖子
    public void click_huiFuTieZi() throws IOException {
        if (!username.equals("you_ke")) {
            String str = textInDialog("输入你想回复的帖子的id：");
            //reply content
            String content = null;
            JTextArea contentArea = new JTextArea(10, 30);
            JScrollPane contentPane = new JScrollPane(contentArea);
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);
            int ContentResult = JOptionPane.showConfirmDialog(null, contentPane, "请输入content:", JOptionPane.OK_CANCEL_OPTION);
            if (ContentResult == JOptionPane.OK_OPTION) {
                content = contentArea.getText();
            }
            moos.writeObject(new Message(null, username, str, content, MsgType.hui_fu_tie_zi));
            moos.flush();
        } else {
            pop_error("现在是游客模式，无法发布回复", false);
        }
    }

    @FXML //回复回复
    public void click_huiFuHuiFu() throws IOException {
        if (!username.equals("you_ke")) {
            String str = textInDialog("输入你想回复的回复的id：");
            //reply content
            String content = null;
            JTextArea contentArea = new JTextArea(10, 30);
            JScrollPane contentPane = new JScrollPane(contentArea);
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);
            int ContentResult = JOptionPane.showConfirmDialog(null, contentPane, "请输入content:", JOptionPane.OK_CANCEL_OPTION);
            if (ContentResult == JOptionPane.OK_OPTION) {
                content = contentArea.getText();
            }
            moos.writeObject(new Message(null, username, str, content, MsgType.hui_fu_hui_fu));
            moos.flush();
        } else {
            pop_error("现在是游客模式，无法发布二级回复", false);
        }
    }


    @FXML//查看别人发布的帖子
    public void click_chaKanOthersPost() throws IOException {
        String str = textInDialog("输入你想查看到作者id：");
        moos.writeObject(new Message(null, username, null, str, MsgType.cha_kan_ta_ren_fa_bu));
        moos.flush();
    }

    @FXML //查看自己发布的帖子
    public void click_chaKanFaBu() throws IOException {
        if (!username.equals("you_ke")) {
            moos.writeObject(new Message(null, username, null, null, MsgType.cha_kan_fa_bu));
            moos.flush();
        } else {
            pop_error("现在是游客模式，你没有发布任何帖子", false);
        }
    }

    @FXML //查看自己回复的帖子
    public void click_chaKanHuiFu() throws IOException {
        if (!username.equals("you_ke")) {
            moos.writeObject(new Message(null, username, null, null, MsgType.cha_kan_hui_fu));
            moos.flush();
        } else {
            pop_error("现在是游客模式，你没有发布任何回复", false);
        }
    }

    @FXML //查看自己的二级回复
    public void click_chaKanErJiHuiFu() throws IOException {
        if (!username.equals("you_ke")) {
            moos.writeObject(new Message(null, username, null, null, MsgType.cha_kan_er_ji_hui_fu));
            moos.flush();
        } else {
            pop_error("现在是游客模式，你没有发布任何二级回复", false);
        }
    }

    @FXML //类型搜索
    public void click_cateSearch() throws IOException {
        String str = textInDialog("请输入筛选类型的id或名称：");
        if (str != null) {
            if (str.split(" ").length >= 2) {
                pop_error("请只输入一种形式", false);
            } else {
                moos.writeObject(new Message(null, null, null, str, MsgType.lei_xing_sou_suo));
                moos.flush();
            }
        }
    }

    @FXML //多参数搜索帖子
    public void click_multiSearch() throws IOException {
        String[] recorder = multiSearchInput();
        //by: key~key  to:start time  data : end time
        moos.writeObject(new Message(null, recorder[0], recorder[1], recorder[2], MsgType.multi_search));
        moos.flush();
    }

    @FXML //屏蔽
    public void click_pingBi() throws IOException {
        if (!username.equals("you_ke")) {
            String str = textInDialog("输入你想屏蔽的作者id：");
            moos.writeObject(new Message(null, username, null, str, MsgType.ping_bi));
            moos.flush();
        } else {
            pop_error("现在是游客模式，无法使用屏蔽功能", false);
        }
    }

    @FXML //收听音频
    public void click_yinPin() throws IOException {
        moos.writeObject(new Message(null, username, null, null, MsgType.yin_pin));
        moos.flush();
    }

    public void morePostInfo(String str) throws IOException {
        if (isNumeric(str.split(" ")[0])) {
            moos.writeObject(new Message(null, username, null, str, MsgType.more_post_information));
            moos.flush();
        }
    }

    public static boolean isNumeric(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }


    private String[] multiSearchInput() {
        String[] answer = new String[3];
        answer[0] = createInputDialog("关键词搜索", "输入关键词，多关键词请用“~”隔开", "输入关键词");
//        answer[1] = createInputDialog("类型搜索","输入类型，多类型搜索请用“~”隔开","输入类型");
        answer[1] = createInputDialog("时间搜索", "输入起始时间", "输入起始时间");
        answer[2] = createInputDialog("时间搜索", "输入终止时间", "输入终止时间");

        //对每一种类型进行判断
//        if(answer[1]!=null && !isPositiveIntegerArray(answer[1].split("~"))){
//            pop_error("筛选的类型只能是正整数",false);
//        }
        if (answer[1] != null && !isTimestamp(answer[1])) {
            pop_error("起始时间格式错误", false);
        }
        if (answer[2] != null && !isTimestamp(answer[2])) {
            pop_error("终止时间格式错误", false);
        }

        return answer;
    }

    public static boolean isPositiveIntegerArray(String[] arr) {
        for (String s : arr) {
            if (!s.matches("\\d+")) {
                return false;
            }
        }
        return true;
    }

    public static boolean isTimestamp(String s) {
        try {
            Timestamp.valueOf(s);
            return true;
        } catch (Exception e) {
            String regex = "^\\d{4}-\\d{2}-\\d{2}$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(s);
            return matcher.matches();
        }
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

    private String createInputDialog(String s1, String s2, String s3) {
        TextInputDialog dialog = new TextInputDialog("输入null，表示对此不加限制");
        dialog.setTitle(s1);
        dialog.setHeaderText(s2);
        dialog.setContentText(s3);

// Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().equals("null")) {
            return result.get();
        }
        return null;
    }

    //弹出一个选择对话框
    private String choiceAnonyDialog() {

        List<String> choices = new ArrayList<>();
        choices.add("no");
        choices.add("yes");

        ChoiceDialog<String> dialog = new ChoiceDialog<>("no", choices);
        dialog.setTitle("是否匿名发布？");
        dialog.setHeaderText("是否匿名发布？");
        dialog.setContentText("选择:");

// Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private String[] createMultiInput() {

        // Create the custom dialog.
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("多参数搜索帖子");
        dialog.setHeaderText("搜索");

// Set the button types.
        ButtonType loginButtonType = new ButtonType("搜索", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

// Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField key_words_Field = new TextField();
        key_words_Field.setPromptText("key_words:");
        TextField category_Field = new TextField();
        category_Field.setPromptText("category:");
        TextField time_Field = new TextField();
        time_Field.setPromptText("time_begin");
        TextField time2_Fieled = new TextField();
        time2_Fieled.setPromptText("time_end");


        grid.add(new Label("key_words:"), 0, 0);
        grid.add(key_words_Field, 1, 0);
        grid.add(new Label("category:"), 0, 1);
        grid.add(category_Field, 1, 1);
        grid.add(new Label("time_begin:"), 0, 2);
        grid.add(time_Field, 1, 2);
        grid.add(new Label("time_end:"), 0, 3);
        grid.add(time2_Fieled, 1, 3);


// Enable/Disable login button depending on whether a username was entered.
        Node loginbutton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginbutton.setDisable(true);

// Do some validation (using the Java 8 lambda syntax).
        key_words_Field.textProperty().addListener((observable, oldValue, newValue) -> {
            loginbutton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

// Request focus on the username field by default.
        Platform.runLater(() -> key_words_Field.requestFocus());

// Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                Map<String, String> result = new HashMap<>();
                result.put("key_words", key_words_Field.getText());
                result.put("category", category_Field.getText());
                result.put("time_begin", time_Field.getText());
                result.put("time_end", time2_Fieled.getText());
                return result;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(value -> {
            System.out.println("多参数搜索：");
            System.out.println("key_words: " + value.get("key_words"));
            System.out.println("category: " + value.get("category"));
            System.out.println("time_begin: " + value.get("time_begin"));
            System.out.println("time_end: " + value.get("time_end"));
        });

        String[] answer = {"", "", ""};
        if (result.isPresent()) {
            answer[0] = result.get().get("key_words");
            answer[1] = result.get().get("category");
            answer[2] = result.get().get("time_begin") + "~" + result.get().get("time_end");
        }
        return answer;
    }


    /**
     * 用于收集发布post的其他信息
     */
    public void getPostContent(String title, String anony) throws IOException {
        //弹窗，得到content
        String content = "the author is lazy, he didn't put a content into database.";
        JTextArea contentArea = new JTextArea(10, 30);
        JScrollPane contentPane = new JScrollPane(contentArea);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        int ContentResult = JOptionPane.showConfirmDialog(null, contentPane, "请输入content:", JOptionPane.OK_CANCEL_OPTION);
        if (ContentResult == JOptionPane.OK_OPTION) {
            content = contentArea.getText();
        }
        System.out.println("content: " + content);

        moos.writeObject(new Message(null, title, anony, content, MsgType.re_fa_bu_tie_zi_content));
        moos.flush();
    }

    public void getPostCity(String title, String content, String[] cities, String anony) throws IOException {

        //title 已获得
        //content
        //posting_time已获得(not author_reg_time)
        //author_id
        String author_id = username;

        //city
        JFrame cityFrame = new JFrame("为你的post选择city");
        cityFrame.setLocationRelativeTo(null);
        cityFrame.setSize(300, 70);
        JPanel cityPanel = new JPanel();
        JComboBox<String> cityComboBox = new JComboBox<>(cities);
        cityComboBox.setMaximumRowCount(12);
        cityPanel.add(cityComboBox);
        cityFrame.add(cityPanel);
        JButton cityOkButton = new JButton("确定");
        cityOkButton.addActionListener(e -> {
            String selectCity = (String) cityComboBox.getSelectedItem();
            try {
                //sendTo为title+content，data为city
                moos.writeObject(new Message(System.currentTimeMillis(), username + "``" + anony, title + "``" + content,
                        selectCity, MsgType.re_fa_bu_tie_zi_city));
                moos.flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            cityFrame.dispose();
        });
        cityPanel.add(cityOkButton);
//        cityFrame.pack();
        cityFrame.setVisible(true);
    }


    //category
    public void getPostCategories(int pid, String[] categories) {
        Set<String> cates = new HashSet<>();
        JFrame cateFrame = new JFrame("为你的post选择category（可多选）");
        cateFrame.setLocationRelativeTo(null);
        cateFrame.setSize(400, 600);
        JPanel catePanel = new JPanel();
        catePanel.setLayout(new BoxLayout(catePanel, BoxLayout.Y_AXIS));
        JCheckBox[] checkBoxes = new JCheckBox[categories.length];
        for (int i = 0; i < categories.length; i++) {
            checkBoxes[i] = new JCheckBox(categories[i]);
            catePanel.add(checkBoxes[i]);
        }
        JScrollPane scrollPane = new JScrollPane(catePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JButton okButton = new JButton("确定");
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.addActionListener(e -> {
            for (int i = 0; i < categories.length; i++) {
                if (checkBoxes[i].isSelected()) {
                    cates.add(checkBoxes[i].getText());
                }
            }
            try {
                //sendTo为title，data为“``”加密categories
                moos.writeObject(new Message(System.currentTimeMillis(), username, String.valueOf(pid), reSendCategory(cates), MsgType.re_fa_bu_tie_zi_category));
                moos.flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            cateFrame.dispose();
        });
        Box box = Box.createVerticalBox();
        box.add(okButton);
        box.add(Box.createVerticalStrut(10));
        box.add(scrollPane);
        cateFrame.add(box);
        cateFrame.setVisible(true);

    }


    private String reSendCategory(Set<String> set) {
        StringBuilder sb = new StringBuilder();
        for (String s : set) {
            sb.append(s).append("``");
        }
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
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

    //创造一个用于选择音频的框
    public String chooseAudioDialog(String[] paths) {
        List<String> choices = new ArrayList<>();
        choices.addAll(Arrays.asList(paths));

        ChoiceDialog<String> dialog = new ChoiceDialog<>(paths[0], choices);
        dialog.setTitle("Choice Dialog");
        dialog.setHeaderText("选择你想听的音乐");
        dialog.setContentText("选择音乐:");

// Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public void chooseAudio(String [] paths) throws IOException {
        String title = chooseAudioDialog(paths);
        moos.writeObject(new Message(null,username,null,title,MsgType.yin_pin_again));
        moos.flush();
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


}
