package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.*;


import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sql.DataSource;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import com.alibaba.druid.pool.DruidDataSourceFactory;


class ServerReader implements Runnable {

    private Socket socket;
    private static Connection con = null;

    public ServerReader(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
//        Properties prop = loadDBUser();
//        openDB(prop);
        Properties pro = new Properties();
        try {
            pro.load(this.getClass().getClassLoader().getResourceAsStream("druid.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            DataSource ds = DruidDataSourceFactory.createDataSource(pro);
            openDB(ds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            OOS_OIS.MyObjectInputStream ois = new OOS_OIS.MyObjectInputStream(socket.getInputStream());
            OOS_OIS.MyObjectOutputStream os = new OOS_OIS.MyObjectOutputStream(socket.getOutputStream());
            //read message and send it
            while (true) {
                Message message = (Message) ois.readObject();
                switch (message.getType()) {
                    case COMMAND:
                        if (!Users.user_socket_map.containsKey(message.getSentBy())) {
                            Users.user_socket_map.put(message.getSentBy(), socket);
                            //刷新client的userlist表，向所有链接的发送一个新的userlist
                            System.out.println("发送新的userlist");
                            //向所有client发送当前的userlist,和当前用户数量
                            String userString = getUsers();
                            for (Map.Entry<String, Socket> entry : Users.user_socket_map.entrySet()) {
                                os = new OOS_OIS.MyObjectOutputStream(entry.getValue().getOutputStream());
                                os.writeObject(
                                        new Message(System.currentTimeMillis(), "Server", entry.getKey(), userString, MsgType.REQ)
                                );
                                os.flush();
                            }
                        } else {
                            os = new OOS_OIS.MyObjectOutputStream(socket.getOutputStream());
                            os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), "not allow login", MsgType.NOT_ALLOW_LOGIN));
                            os.flush();
                        }
                        break;

                    case EXIT:
                        //将该controller代表的user从userlist中清除出去
                        Users.user_socket_map.remove(message.getSentBy());
                        //向每一个发信息，返回下线的username
                        for (Map.Entry<String, Socket> entry : Users.user_socket_map.entrySet()) {
                            os = new OOS_OIS.MyObjectOutputStream(entry.getValue().getOutputStream());
                            os.writeObject(new Message(System.currentTimeMillis(), message.getSentBy(), entry.getKey(), Integer.toString(Users.user_socket_map.size()), MsgType.EXIT));
                            os.flush();
                        }
                        break;

                    case EXIT_NO_KEEP:
                        //向每一个在线的client发送消息，返回退出的username
                        String userString = getUsers();
                        for (Map.Entry<String, Socket> entry : Users.user_socket_map.entrySet()) {
                            os = new OOS_OIS.MyObjectOutputStream(entry.getValue().getOutputStream());
                            os.writeObject(new Message(System.currentTimeMillis(), message.getSentBy(), entry.getKey(), userString, MsgType.EXIT_NO_KEEP));
                            os.flush();
                        }
                        break;


                    case TALKINGTO:
                        //传入的信息中：sentby是改变talkingto的user，sendto是被改变后的talkingTo
                        System.out.println("收到客户端传来的改talkingto的信息,客户端为：" + message.getSentBy());
                        //更新所有的onetooneMap
                        Users.one_to_one.put(message.getSentBy(), message.getSendTo());

                        String str = message.getSentBy() + "`" + message.getSendTo();//前面是发送方，后面是接收方

                        if (!Users.user_user_messages.containsKey(str)) {
                            System.out.println("str input = " + str);
                            Users.user_user_messages.put(str, new ArrayList<>());
                            Users.user_user_messages.put(message.getSendTo() + "`" + message.getSentBy(), new ArrayList<>());
                        } else {
                            //一个一个返回在存储的聊天记录
                            for (int i = 0; i < Users.user_user_messages.get(str).size(); i++) {
                                os = new OOS_OIS.MyObjectOutputStream(Users.user_socket_map.get(message.getSentBy()).getOutputStream());
                                os.writeObject(Users.user_user_messages.get(str).get(i));
                                System.out.println("返回存储的聊天记录给" + message.getSentBy());
                                os.flush();
                            }
                        }
                        break;

                    case SERVER_EXIT:
                        System.out.println("Confirm server exit");
                        System.exit(0);
                        break;

                    case client_visitor:
                        String SQL3 = "select *\n" +
                                "from post join (select count(post_id) as c,post_id as p\n" +
                                "from post_likes group by post_id order by c desc limit 10) x\n" +
                                "on post_id = x.p where post_id = x.p order by x.c desc";
                        PreparedStatement ppsm3 = con.prepareStatement(SQL3);
                        ResultSet rsrs3 = ppsm3.executeQuery();
                        StringBuilder sbsb3 = new StringBuilder();
                        while (rsrs3.next()) {
                            sbsb3.append(rsrs3.getInt(1)).append(" ").append(rsrs3.getString(2)).append("~~");
                        }
                        if (sbsb3.toString().endsWith("~~")) {
                            sbsb3.deleteCharAt(sbsb3.length() - 1);
                            sbsb3.deleteCharAt(sbsb3.length() - 1);
                        }
                        con.commit();
                        System.out.println("execute ok");
                        os.writeObject(new Message(System.currentTimeMillis(), "Server", null, sbsb3.toString(), MsgType.client_visitor));
                        os.flush();
                        break;

                    case client_register:
                        /**
                         * 用sql语句判断是否已经存在有这个用户，如果有，让他重新登录
                         */
                        String sendBy = message.getSentBy();
                        String[] decodeResult = cl_decoder(message.getData());//0:name 1:phone 2:time(String)
                        Timestamp ttp = Timestamp.valueOf(decodeResult[2]);//ttp:time

                        String sql1 = "select count(*) from author where author_id = ? or author_name = ?";
                        PreparedStatement statement1 = con.prepareStatement(sql1);
                        statement1.setString(1, sendBy);
                        statement1.setString(2, decodeResult[0]);
//                        statement1.addBatch();
//                        statement1.executeBatch();
                        ResultSet rs1 = statement1.executeQuery();
                        rs1.next();
                        int count1 = rs1.getInt(1);
                        if (count1 != 0) {//在注册时有重复
                            os.writeObject(new Message(System.currentTimeMillis(), "Server", sendBy, "1", MsgType.client_register_reject));
                            os.flush();
                            rs1.close();
                            statement1.close();
                        } else {
                            //将author加入map和数据库中
                            Users.user_socket_map.put(sendBy, socket);
                            String SQL1 = "insert into author (author_id, author_name, author_reg_time, author_phone) values (?,?,?,?)";
                            PreparedStatement ppsm = con.prepareStatement(SQL1);
                            ppsm.setString(1, sendBy);
                            ppsm.setString(2, decodeResult[0]);
                            ppsm.setTimestamp(3, ttp);
                            ppsm.setString(4, decodeResult[1]);
                            ppsm.executeUpdate();
                            ppsm.close();

                            //SQL2 to get hot search list
                            String SQL2 = "select *\n" +
                                    "from post join (select count(post_id) as c,post_id as p\n" +
                                    "from post_likes group by post_id order by c desc limit 10) x\n" +
                                    "on post_id = x.p where post_id = x.p order by x.c desc";
                            PreparedStatement ppsm2 = con.prepareStatement(SQL2);
                            ResultSet rsrs = ppsm2.executeQuery();
                            StringBuilder sbsb = new StringBuilder();
                            while (rsrs.next()) {
                                sbsb.append(rsrs.getInt(1)).append(" ").append(rsrs.getString(2)).append("~~");
                            }
                            if (sbsb.toString().endsWith("~~")) {
                                sbsb.deleteCharAt(sbsb.length() - 1);
                                sbsb.deleteCharAt(sbsb.length() - 1);
                            }
                            con.commit();
                            System.out.println("execute ok");
                            os.writeObject(new Message(System.currentTimeMillis(), "Server", sendBy, sbsb.toString(), MsgType.client_register_success));
                            os.flush();
                        }
                        break;
                    case client_login:
                        String sendByLogin = message.getSentBy();
                        String[] decodeLogin = cl_decoder(message.getData());
                        String sql2 = "select count(*) from author where author_id = ? and author_name = ? and author_phone = ?";
                        PreparedStatement statement2 = con.prepareStatement(sql2);
                        statement2.setString(1, sendByLogin);
                        statement2.setString(2, decodeLogin[0]);
                        statement2.setString(3, decodeLogin[1]);
                        ResultSet rs2 = statement2.executeQuery();
                        rs2.next();
                        int count2 = rs2.getInt(1);
                        if (count2 != 0) {//登录已有账户
                            /**
                             * 加入map？没做
                             */
                            //SQL2 to get hot search list
                            String SQL4 = "select *\n" +
                                    "from post join (select count(post_id) as c,post_id as p\n" +
                                    "from post_likes group by post_id order by c desc limit 10) x\n" +
                                    "on post_id = x.p where post_id = x.p order by x.c desc";
                            PreparedStatement ppsm4 = con.prepareStatement(SQL4);
                            ResultSet rsrs4 = ppsm4.executeQuery();
                            StringBuilder sbsb4 = new StringBuilder();
                            while (rsrs4.next()) {
                                sbsb4.append(rsrs4.getInt(1)).append(" ").append(rsrs4.getString(2)).append("~~");
                            }
                            if (sbsb4.toString().endsWith("~~")) {
                                sbsb4.deleteCharAt(sbsb4.length() - 1);
                                sbsb4.deleteCharAt(sbsb4.length() - 1);
                            }
                            os.writeObject(new Message(System.currentTimeMillis(), "Server", sendByLogin, sbsb4.toString(), MsgType.client_login_success));
                            os.flush();
                        } else {//某一项不正确
                            os.writeObject(new Message(System.currentTimeMillis(), "Server", sendByLogin, "login Failed", MsgType.client_login_reject));
                            os.flush();
                        }
                        con.commit();
                        rs2.close();
                        statement2.close();
                        break;

                    case dian_zan: //点赞
                        if (message.getData() != null) {
                            int post_id3 = Integer.parseInt(message.getData());
                            if (judge_post_exist(post_id3)) {
                                //判断是否已经点过赞了
                                if (!judge_post_operations("post_likes", post_id3, message.getSentBy())) {
                                    do_post_operation("post_likes", post_id3, message.getSentBy());
                                    os.writeObject(new Message(null, "Server", message.getSentBy(), "success", MsgType.dian_zan));
                                    os.flush();
                                } else {
                                    os.writeObject(new Message(null, "Server", message.getSentBy(), "have", MsgType.dian_zan));
                                    os.flush();
                                }
                            } else {
                                os.writeObject(new Message(null, "Server", message.getSentBy(), "fail", MsgType.dian_zan));
                                os.flush();
                            }
                        }
                        break;

                    case shou_cang:
                        if (message.getData() != null) {
                            int post_id4 = Integer.parseInt(message.getData());
                            if (judge_post_exist(post_id4)) {
                                if (!judge_post_operations("post_favorites", post_id4, message.getSentBy())) {
                                    do_post_operation("post_favorites", post_id4, message.getSentBy());
                                    os.writeObject(new Message(null, "Server", message.getSentBy(), "success", MsgType.shou_cang));
                                    os.flush();
                                } else {
                                    os.writeObject(new Message(null, "Server", message.getSentBy(), "have", MsgType.shou_cang));
                                    os.flush();
                                }
                            } else {
                                os.writeObject(new Message(null, "Server", message.getSentBy(), "fail", MsgType.shou_cang));
                                os.flush();
                            }
                        }
                        break;

                    case zhuan_fa:
                        if (message.getData() != null) {
                            int post_id5 = Integer.parseInt(message.getData());
                            if (judge_post_exist(post_id5)) {
                                if (!judge_post_operations("post_shares", post_id5, message.getSentBy())) {
                                    do_post_operation("post_shares", post_id5, message.getSentBy());
                                    os.writeObject(new Message(null, "Server", message.getSentBy(), "success", MsgType.zhuan_fa));
                                    os.flush();
                                } else {
                                    os.writeObject(new Message(null, "Server", message.getSentBy(), "have", MsgType.zhuan_fa));
                                    os.flush();
                                }
                            } else {
                                os.writeObject(new Message(null, "Server", message.getSentBy(), "fail", MsgType.zhuan_fa));
                                os.flush();
                            }
                        }
                        break;

                    case cha_kan_dian_zan: //查看点赞
                        String sql6 = "select post_id from post_likes where author_id = ?";
                        PreparedStatement ps6 = con.prepareStatement(sql6);
                        ps6.setString(1, message.getSentBy());
                        ResultSet rs6 = ps6.executeQuery();
                        StringBuilder sb6 = new StringBuilder();
                        while (rs6.next()) {
                            sb6.append(rs6.getString(1)).append("~");
                        }
                        if (sb6.length() > 0) sb6.deleteCharAt(sb6.length() - 1);
                        os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), sb6.toString(), MsgType.cha_kan_dian_zan));
                        os.flush();
                        con.commit();
                        ps6.close();
                        break;

                    case cha_kan_shou_cang://查看收藏
                        String sql7 = "select post_id from post_favorites where author_id = ?";
                        PreparedStatement ps7 = con.prepareStatement(sql7);
                        ps7.setString(1, message.getSentBy());
                        ResultSet rs7 = ps7.executeQuery();
                        StringBuilder sb7 = new StringBuilder();
                        while (rs7.next()) {
                            sb7.append(rs7.getString(1)).append("~");
                        }
                        if (sb7.length() > 0) sb7.deleteCharAt(sb7.length() - 1);
                        os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), sb7.toString(), MsgType.cha_kan_shou_cang));
                        os.flush();
                        con.commit();
                        ps7.close();
                        break;

                    case cha_kan_zhuan_fa: //查看转发
                        String sql8 = "select post_id from post_shares where author_id = ?";
                        PreparedStatement ps8 = con.prepareStatement(sql8);
                        ps8.setString(1, message.getSentBy());
                        ResultSet rs8 = ps8.executeQuery();
                        StringBuilder sb8 = new StringBuilder();
                        while (rs8.next()) {
                            sb8.append(rs8.getString(1)).append("~");
                        }
                        if (sb8.length() > 0) sb8.deleteCharAt(sb8.length() - 1);
                        os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), sb8.toString(), MsgType.cha_kan_zhuan_fa));
                        os.flush();
                        con.commit();
                        ps8.close();
                        break;

                    case guan_zhu://关注
                        if (message.getSentBy() != null) {
                            //判断是否有这个authorId
                            if (judge_author_exist(message.getSentBy())) {
                                //判断是否已经关注了
                                if (!judge_author_operation("post_followers", message.getData(), message.getSentBy())) {//如果还没有关注
                                    do_author_operation("post_followers", message.getData(), message.getSentBy());
                                    os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), "success", MsgType.guan_zhu));
                                    os.flush();
                                } else {
                                    os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), "have", MsgType.guan_zhu));
                                    os.flush();
                                }
                            } else {//没有这个id
                                os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), "fail", MsgType.guan_zhu));
                                os.flush();
                            }
                        }
                        break;

                    case qu_xiao_guan_zhu://取消关注
                        if (message.getSentBy() != null) {
                            if (judge_author_exist(message.getSentBy())) {
                                if (!judge_author_operation("post_followers", message.getData(), message.getSentBy())) {//如果还没有关注
                                    os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), "havent", MsgType.qu_xiao_guan_zhu));
                                    os.flush();
                                } else {
                                    String sql10 = "delete from post_followers where follow_id = ? and author_id = ?";
                                    PreparedStatement ps10 = con.prepareStatement(sql10);
                                    ps10.setString(1, message.getSentBy());
                                    ps10.setString(2, message.getData());
                                    ps10.executeUpdate();
                                    con.commit();
                                    os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), "success", MsgType.qu_xiao_guan_zhu));
                                    os.flush();
                                }
                            } else {//没有这个id
                                os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), "fail", MsgType.qu_xiao_guan_zhu));
                                os.flush();
                            }
                        }
                        break;

                    case cha_kan_guan_zhu:
                        String sql11 = "select author_id from post_followers where follow_id = ?";
                        PreparedStatement ps11 = con.prepareStatement(sql11);
                        ps11.setString(1, message.getSentBy());
                        ResultSet rs11 = ps11.executeQuery();
                        StringBuilder sb11 = new StringBuilder();
                        while (rs11.next()) {
                            sb11.append(rs11.getString(1)).append("~");
                        }
                        if (sb11.length() > 0) sb11.deleteCharAt(sb11.length() - 1);
                        os.writeObject(new Message(System.currentTimeMillis(), "Server", message.getSentBy(), sb11.toString(), MsgType.cha_kan_guan_zhu));
                        os.flush();
                        con.commit();
                        ps11.close();
                        break;

                    case fa_bu_tie_zi:
                        if (message.getData() != null) {
                            if (judge_post_exist(message.getData())) { //如果title重复了
                                os.writeObject(new Message(null, "Server", message.getSentBy(), "repetitive!!!", MsgType.fa_bu_tie_zi));
                                os.flush();
                            } else {
                                //getdata ->title ,by-> anony
                                os.writeObject(new Message(System.currentTimeMillis(), message.getSendTo(), message.getSentBy(), message.getData(), MsgType.fa_bu_tie_zi));
                                os.flush();
                            }
                        }
                        break;

                    case re_fa_bu_tie_zi_content://接受发送回来的content,发送回去city列表
                        //sentby: title~~anony  sendTo: content   data: city
                        if (message.getSendTo() != null) {
                            os.writeObject(new Message(null, message.getSentBy() + "~~" + message.getSendTo(), message.getData(),
                                    cityToString(getCity()), MsgType.re_fa_bu_tie_zi_content));
                            os.flush();
                        }
                        break;

                    case re_fa_bu_tie_zi_city://接受发送回来的city
                        if (message.getData() != null) {
                            int pid = do_post_city(message.getSendTo().split("``")[0], message.getSendTo().split("``")[1],
                                    System.currentTimeMillis(), message.getSentBy().split("``")[0], message.getData(), message.getSentBy().split("``")[1]);
                            //将postid和category列表发送回去 sentBy:username sendTo:post_id data:categories(String)
                            os.writeObject(new Message(null, message.getSentBy(), String.valueOf(pid), categoryToString(getCateGory()), MsgType.re_fa_bu_tie_zi_city));
                            os.flush();
                        }
                        break;

                    case re_fa_bu_tie_zi_category://接受发送回来的categories
                        if (message.getData() != null) {
                            do_post_categories(Integer.parseInt(message.getSendTo()), message.getData());
                            os.writeObject(new Message(null, "Server", message.getSentBy(), "success", MsgType.re_fa_bu_tie_zi_category));
                            os.flush();
                        }
                        break;

                    case hui_fu_tie_zi: //sentBy: username  sendTo:post_id  data: reply_content
                        if (message.getSendTo() != null && message.getData() != null) {
                            if (!judge_post_exist(Integer.parseInt(message.getSendTo()))) { // post_id not exist
                                os.writeObject(new Message(null, "Server", message.getSentBy(), "fail", MsgType.hui_fu_tie_zi));
                                os.flush();
                            } else {
                                do_reply_add(Integer.parseInt(message.getSendTo()), message.getData(), message.getSentBy());
                                os.writeObject(new Message(null, "Server", message.getSentBy(), "success", MsgType.hui_fu_tie_zi));
                                os.flush();
                            }
                        }
                        break;

                    case hui_fu_hui_fu:
                        if (message.getSendTo() != null && message.getData() != null) {
                            if (!judge_reply_exist(Integer.parseInt(message.getSendTo()))) { //reply not exist
                                os.writeObject(new Message(null, "Server", message.getSentBy(), "fail", MsgType.hui_fu_hui_fu));
                                os.flush();
                            } else {
                                do_secondary_reply_add(Integer.parseInt(message.getSendTo()), message.getData(), message.getSentBy());
                                os.writeObject(new Message(null, "Server", message.getSentBy(), "success", MsgType.hui_fu_hui_fu));
                                os.flush();
                            }
                        }
                        break;

                    case cha_kan_ta_ren_fa_bu:
                        String sql15;
                        sql15 = "select * from post where author_id = ?";

                        PreparedStatement ps15 = con.prepareStatement(sql15);
                        ps15.setString(1, message.getData());
                        ResultSet rs15 = ps15.executeQuery();
                        StringBuilder sb15 = new StringBuilder("左边为post_id, 右边为title~~");
                        while (rs15.next()) {
                            if (!rs15.getBoolean(7) || rs15.getString(5).equals(message.getSentBy())) {
                                sb15.append(rs15.getInt(1)).append(" ").append(rs15.getString(2)).append(" ").append(rs15.getString(5)).append("~~");
                            } else {
                                sb15.append(rs15.getInt(1)).append(" ").append(rs15.getString(2)).append(" ").append("(匿名)").append("~~");
                            }
                        }
                        if (sb15.toString().endsWith("~~")) {
                            sb15.deleteCharAt(sb15.length() - 1);
                            sb15.deleteCharAt(sb15.length() - 1);
                        }

                        String sql16 = "select * from author_ignore where author_id = ? and ignored_author_id = ?";
                        PreparedStatement ps16 = con.prepareStatement(sql16);
                        ps16.setString(1, message.getSentBy());
                        ps16.setString(2, message.getData());
                        if (ps16.executeQuery().next()) {
                            sb15 = new StringBuilder();
                        }

                        os.writeObject(new Message(null, message.getData(), message.getSentBy(), sb15.toString(), MsgType.cha_kan_ta_ren_fa_bu));
                        os.flush();
                        break;

                    case cha_kan_fa_bu:
                        String sql12 = "select * from post where author_id = ?";
                        PreparedStatement ps12 = con.prepareStatement(sql12);
                        ps12.setString(1, message.getSentBy());
                        ResultSet rs12 = ps12.executeQuery();
                        StringBuilder sb12 = new StringBuilder();
                        sb12.append("左边为post_id，右边为title~~");
                        while (rs12.next()) {
                            sb12.append(rs12.getInt(1)).append(" ").append(rs12.getString(2)).append("~~");
                        }
                        if (sb12.toString().endsWith("~~")) {
                            sb12.deleteCharAt(sb12.length() - 1);
                            sb12.deleteCharAt(sb12.length() - 1);
                        }

                        os.writeObject(new Message(null, null, message.getSentBy(), sb12.toString(), MsgType.cha_kan_fa_bu));
                        os.flush();
                        break;

                    case cha_kan_hui_fu:
                        String sql13 = "select * from reply where reply_author_id = ?";
                        PreparedStatement ps13 = con.prepareStatement(sql13);
                        ps13.setString(1, message.getSentBy());
                        ResultSet rs13 = ps13.executeQuery();
                        StringBuilder sb13 = new StringBuilder();
                        sb13.append("左边为reply_id，右边为post_id~~");
                        while (rs13.next()) {
                            sb13.append(rs13.getInt(1)).append(" ").append(rs13.getInt(2)).append("~~");
                        }
                        if (sb13.toString().endsWith("~~")) {
                            sb13.deleteCharAt(sb13.length() - 1);
                            sb13.deleteCharAt(sb13.length() - 1);
                        }

                        os.writeObject(new Message(null, null, message.getSentBy(), sb13.toString(), MsgType.cha_kan_hui_fu));
                        os.flush();
                        break;

                    case cha_kan_er_ji_hui_fu:
                        String sql14 = "select * from secondary_reply where secondary_reply_author_id = ?";
                        PreparedStatement ps14 = con.prepareStatement(sql14);
                        ps14.setString(1, message.getSentBy());
                        ResultSet rs14 = ps14.executeQuery();
                        StringBuilder sb14 = new StringBuilder();
                        sb14.append("左边为secondary_reply_id，右边为reply_id~~");
                        while (rs14.next()) {
                            sb14.append(rs14.getInt(1)).append(" ").append(rs14.getInt(2)).append("~~");
                        }
                        if (sb14.toString().endsWith("~~")) {
                            sb14.deleteCharAt(sb14.length() - 1);
                            sb14.deleteCharAt(sb14.length() - 1);
                        }

                        os.writeObject(new Message(null, null, message.getSentBy(), sb14.toString(), MsgType.cha_kan_er_ji_hui_fu));
                        os.flush();
                        break;

                    case lei_xing_sou_suo:
                        os.writeObject(new Message(null, null, null, do_category_search(message.getData()), MsgType.lei_xing_sou_suo));
                        os.flush();
                        break;

                    case multi_search:
                        os.writeObject(new Message(null, null, null,
                                do_multi_search(message.getSentBy(), message.getSendTo(), message.getData()), MsgType.multi_search));
                        os.flush();
                        break;

                    case ping_bi:
                        if (message.getData() != null) {
                            //检查是否存在这个用户
                            if (!judge_author_exist(message.getData())) {
                                os.writeObject(new Message(null, null, null, "fail", MsgType.ping_bi));
                                os.flush();
                            } else {
                                do_ignore_add(message.getSentBy(), message.getData());
                                //去掉点赞
                                remove_like(message.getData(), message.getSentBy());
                                //取消收藏
                                remove_favorite(message.getData(), message.getSentBy());
                                //取消关注
                                remove_follow(message.getData(), message.getSentBy());
                                os.writeObject(new Message(null, null, null, "success", MsgType.ping_bi));
                                os.flush();
                            }
                        }
                        break;


                    case more_post_information:
                        os.writeObject(new Message(null, null, null,
                                do_post_info_req(Integer.parseInt(message.getData())), MsgType.more_post_information));
                        os.flush();
                        break;

                    case yin_pin:
                        //发送回去所有音频文件(以 .wav 结尾的content)
                        os.writeObject(new Message(null,null,null,sendBackAudioList(),MsgType.yin_pin));
                        os.flush();
                        break;

                    case yin_pin_again:
                        do_play_audio(message.getData());
                        break;

                    default:
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("null");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private void do_play_audio(String title) throws SQLException, UnsupportedAudioFileException, LineUnavailableException, IOException {
        String sql = "select *\n" +
                "from post where title = ?";
        PreparedStatement ps  =con.prepareStatement(sql);
        ps.setString(1,title);
        ResultSet rs = ps.executeQuery();
        rs.next();
        String path = rs.getString(3);
        MyAudioPlayer.playMusic(path);
    }

    private String sendBackAudioList() throws SQLException {
        String sql = "select *\n" +
                "from post where content like '%.wav'";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            sb.append(rs.getString(2)).append("~");
        }
        if(sb.length()>1){
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }


    private String do_post_info_req(int post_id) throws SQLException {
        String sql = "select * from post where post_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, post_id);
        ResultSet rs = ps.executeQuery();
        con.commit();
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            sb.append("post_id  : ").append(rs.getInt(1)).append("\n")
                    .append("title    : ").append(rs.getString(2)).append("\n")
                    .append("content  : ").append(rs.getString(3)).append("\n")
                    .append("post_time: ").append(rs.getTimestamp(4)).append("\n")
                    .append("author_id: ").append(rs.getString(5)).append("\n")
                    .append("city_id  : ").append(rs.getInt(6)).append("\n").append("~");
        }
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private void remove_follow(String author_id, String follower) throws SQLException {
        String sql = "delete\n" +
                "from post_followers\n" +
                "where author_id = ? and follow_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, author_id);
        ps.setString(2, follower);
        ps.executeUpdate();
    }

    private void remove_favorite(String author_id, String liker) throws SQLException {
        String sql = "delete from post_favorites where post_id = (select post.post_id from post where post.author_id = ?) and author_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, author_id);
        ps.setString(2, liker);
        ps.executeUpdate();
    }

    private void remove_like(String author_id, String liker) throws SQLException {
        String sql = "delete from post_likes where post_id = (select post.post_id from post where post.author_id = ?) and author_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, author_id);
        ps.setString(2, liker);
        ps.executeUpdate();
    }

    private void do_ignore_add(String author_id, String ignored_id) throws SQLException {
        String sql = "insert into author_ignore (author_id, ignored_author_id) VALUES (?,?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, author_id);
        ps.setString(2, ignored_id);
        ps.executeUpdate();
        con.commit();
    }

    //类型搜索
    private String do_category_search(String str) throws SQLException {
        if (isNumeric(str)) {
            String sql = "select *\n" +
                    "from post p join (select * from post_category where category_id = ?) pc on p.post_id = pc.post_id ";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(str));
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(rs.getInt(1)).append(" ").append(rs.getString(2)).append("~");
            }
            if (sb.length() > 1) {
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        } else {
            if (!checkAlpha(str).equals("wrongSymbol")) {
                String sql = "with x as(select *\n" +
                        "from post_category pc join (select *\n" +
                        "from category where name = ?) c on c.category_id = pc.category_id)\n" +
                        "select distinct p.post_id,title\n" +
                        "from post p join x on p.post_id = x.post_id order by post_id";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, checkAlpha(str));
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    sb.append(rs.getInt(1)).append(" ").append(rs.getString(2)).append("~");
                }
                if (sb.length() > 1) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                return sb.toString();
            } else {
                return "fail";
            }
        }
    }

    public static String checkAlpha(String s) {
        if (s.matches("[a-zA-Z]+")) {
            return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
        } else {
            return "wrongSymbol";
        }
    }

    private String do_multi_search(String k, String st, String et) throws SQLException {
        if (k != null) {
            String[] keys = k.split("~");
            StringBuilder sqlSB = new StringBuilder("select *\n" +
                    "from post\n" +
                    "where ");
            for (String s : keys) {
                sqlSB.append("title like '%").append(s).append("%' and ");
            }

            if (st == null && et == null) {//如果没有对时间加限制
                for (int i = 0; i < 4; i++) {
                    sqlSB.deleteCharAt(sqlSB.length() - 1);
                }
            } else if (st == null) {
                sqlSB.append("posting_time between '1000-01-01' and '").append(et).append("'");
            } else if (et == null) {
                sqlSB.append("posting_time between '").append(st).append("' and '4000-01-01'");
            } else {
                sqlSB.append("posting_time between '").append(st).append("' and '").append(et).append("'");
            }

            String sql = sqlSB.toString();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            con.commit();
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(rs.getInt(1)).append(" ").append(rs.getString(2)).append("~~");
            }
            if (sb.length() >= 2) {
                sb.deleteCharAt(sb.length() - 1);
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();

        } else {//k is null
            StringBuilder sqlSB = new StringBuilder("select *\n" +
                    "from post\n" +
                    "where ");
            if (st == null && et == null) {//如果没有对时间加限制
                for (int i = 0; i < 4; i++) {
                    sqlSB.deleteCharAt(sqlSB.length() - 1);
                }
            } else if (st == null) {
                sqlSB.append("posting_time between '1000-01-01' and '").append(et).append("'");
            } else if (et == null) {
                sqlSB.append("posting_time between '").append(st).append("' and '4000-01-01'");
            } else {
                sqlSB.append("posting_time between '").append(st).append("' and '").append(et).append("'");
            }

            String sql = sqlSB.toString();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            con.commit();
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(rs.getInt(1)).append(" ").append(rs.getString(2)).append("~~");
            }
            if (sb.length() >= 2) {
                sb.deleteCharAt(sb.length() - 1);
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        }

    }

    //category is a String[]
    private void multiSearch(String key, String category, Timestamp time1, Timestamp time2) {

        String[] keys = key.split("~");

        String[] categories = category.split("~");
        int[] cates = new int[categories.length];
        for (int i = 0; i < cates.length; i++) {
            cates[i] = Integer.parseInt(categories[i]);
        }


    }

    //输入的str为全是数字或者为""，返回true
    public static boolean isNumeric(String str) {
        if (!str.equals("")) {
            for (int i = str.length(); --i >= 0; ) {
                if (!Character.isDigit(str.charAt(i)) && str.charAt(i) != '~') {
                    return false;
                }
            }
        }
        return true;
    }


    private void do_secondary_reply_add(int reply_id, String secondary_reply_content, String secondary_reply_author_id) throws SQLException {
        String sql1 = "select max(secondary_reply_id) from secondary_reply";
        PreparedStatement ps1 = con.prepareStatement(sql1);
        ResultSet rs = ps1.executeQuery();
        rs.next();
        int secondary_reply_id = rs.getInt(1) + 1;
        rs.close();
        ps1.close();

        String sql2 = "insert into secondary_reply (secondary_reply_id,reply_id, secondary_reply_content, secondary_reply_stars, secondary_reply_author_id)\n" +
                "values (?,?,?,?,?)";
        PreparedStatement ps2 = con.prepareStatement(sql2);
        ps2.setInt(1, secondary_reply_id);
        ps2.setInt(2, reply_id);
        ps2.setString(3, secondary_reply_content);
        ps2.setInt(4, 0);
        ps2.setString(5, secondary_reply_author_id);
        ps2.executeUpdate();
        con.commit();
        ps2.close();
    }


    private void do_reply_add(int post_id, String reply_content, String reply_author_id) throws SQLException {
        String sql1 = "select max(reply_id) from reply";
        PreparedStatement ps1 = con.prepareStatement(sql1);
        ResultSet rs = ps1.executeQuery();
        rs.next();
        int reply_id = rs.getInt(1) + 1;
        rs.close();
        ps1.close();

        String sql2 = "insert into reply (reply_id,post_id, reply_content, reply_stars, reply_author_id) values (?,?,?,?,?)";
        PreparedStatement ps2 = con.prepareStatement(sql2);
        ps2.setInt(1, reply_id);
        ps2.setInt(2, post_id);
        ps2.setString(3, reply_content);
        ps2.setInt(4, 0);
        ps2.setString(5, reply_author_id);
        ps2.executeUpdate();
        con.commit();
        ps2.close();
    }

    private void do_post_categories(int pid, String cate) throws SQLException {

        String[] categories = cate.split("``"); //get categories
        for (String s : categories) {
            String sql1 = "select category_id from category where name = ?";
            PreparedStatement ps1 = con.prepareStatement(sql1);
            ps1.setString(1, s);
            ResultSet resultSet1 = ps1.executeQuery();
            resultSet1.next();
            int cate_id = resultSet1.getInt(1);


            String sql2 = "insert into post_category (post_id, category_id) VALUES (?,?) on conflict do nothing";
            PreparedStatement ps2 = con.prepareStatement(sql2);
            ps2.setInt(1, pid);
            ps2.setInt(2, cate_id);
            ps2.executeUpdate();
            con.commit();
            resultSet1.close();
            ps1.close();
            ps2.close();

        }
    }

    private int do_post_city(String title, String content, Long time, String author_id, String noCity_id, String anony) throws SQLException {
        String sql = "select max(post_id) from post";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        int post_id = rs.getInt(1) + 1;

        int city_id = Integer.parseInt(noCity_id.split(" ")[0]);
        Timestamp timestamp = new Timestamp(time);
        String sql2 = "insert into post (post_id, title, content, posting_time, author_id, city_id,anonymity) values (?,?,?,?,?,?,?)";
        PreparedStatement ps2 = con.prepareStatement(sql2);
        ps2.setInt(1, post_id);
        ps2.setString(2, title);
        ps2.setString(3, content);
        ps2.setTimestamp(4, timestamp);
        ps2.setString(5, author_id);
        ps2.setInt(6, city_id);
        ps2.setBoolean(7, anony.equals("yes"));
        ps2.executeUpdate();
        con.commit();
        rs.close();
        ps.close();
        ps2.close();
        return post_id;
    }


    private String cityToString(List<String> cities) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cities.size(); i++) {
            sb.append(cities.get(i)).append("``");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private List<String> getCity() throws SQLException {
        String sql = "select c.city_id||' '||c.city_name from city c order by c.city_id";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        con.commit();
        List<String> list = new ArrayList<>();
        while (rs.next()) {
            list.add(rs.getString(1));
        }
        return list;
    }

    //这个方法用于把List category 加密成String
    private String categoryToString(List<String> categories) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            sb.append(categories.get(i)).append("``");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private List<String> getCateGory() throws SQLException {
        String sql = "select name from category order by category_id";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        con.commit();
        List<String> list = new ArrayList<>();
        while (rs.next()) {
            list.add(rs.getString(1));
        }
        return list;
    }

    private void do_author_operation(String tableName, String followedId, String followerId) throws SQLException {
        String sql = "insert into " + tableName + "(follow_id, author_id) values (?,?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, followerId);
        ps.setString(2, followedId);
        ps.executeUpdate();
        System.out.println(tableName + " insert new element");
        con.commit();
    }

    private void do_post_operation(String tableName, int postId, String authorId) throws SQLException {
        String sql = "insert into " + tableName + " (post_id, author_id) values (?,?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, postId);
        ps.setString(2, authorId);
        ps.executeUpdate();
        System.out.println(tableName + " insert new element");
        con.commit();
    }

    private boolean judge_post_operations(String tableName, int postId, String authorId) throws SQLException {
        String sql = "select * from " + tableName + " where post_id = ? and author_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, postId);
        ps.setString(2, authorId);
        ResultSet rs = ps.executeQuery();
        con.commit();
        return rs.next();
    }

    private boolean judge_author_operation(String tableName, String followedId, String followerId) throws SQLException {
        String sql = "select * from " + tableName + " where author_id = ? and follow_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, followedId);
        ps.setString(2, followerId);
        ResultSet rs = ps.executeQuery();
        con.commit();
        return rs.next();
    }

    private boolean judge_reply_exist(int reply_id) throws SQLException {
        String sql = "select * from reply where reply_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, reply_id);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    private boolean judge_post_exist(int postId) throws SQLException {
        String sql = "select from post where post_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, postId);
        ResultSet rs = ps.executeQuery();
        con.commit();
        return rs.next();
    }

    private boolean judge_post_exist(String postTitle) throws SQLException {
        String sql = "select * from post p where p.title = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, postTitle);
        ResultSet rs = ps.executeQuery();
        con.commit();
        return rs.next();
    }

    private boolean judge_author_exist(String authorId) throws SQLException {
        String sql = "select * from author where author_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, authorId);
        ResultSet rs = ps.executeQuery();
        con.commit();
        return rs.next();
    }

    /**
     * 解码client login传入的信息
     *
     * @param data
     */
    private String[] cl_decoder(String data) {
        return data.split("~~~~");
    }


    private Properties loadDBUser() {
        Properties properties = new Properties();
        try {
            properties.load(
                    new InputStreamReader(Files.newInputStream(Paths.get("chatting-server/src/main/resources/dbUser.properties"))));
            return properties;
        } catch (IOException e) {
            System.err.println("can not find db user file");
            throw new RuntimeException(e);
        }
    }

    private void openDB(Properties prop) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            System.err.println("Cannot find the Postgres driver. Check CLASSPATH.");
            System.exit(1);
        }
        String url =
                "jdbc:postgresql://" + prop.getProperty("host") + ":" + prop.getProperty("port")
                        + "/" + prop.getProperty("database");
        try {
            con = DriverManager.getConnection(url, prop);
            if (con != null) {
                System.out.println("Successfully connected to the database.");
                con.setAutoCommit(false);
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private void openDB(DataSource ds) throws SQLException {
        try {
            con = ds.getConnection();
            if (con != null) {
                System.out.println("Successfully connected to the database.");
                con.setAutoCommit(false);
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed");
            System.err.println(e.getMessage());
            System.exit(1);
        }

    }

    private static void clearDataInTable() {
        Statement stmt0;
        if (con != null) {
            try {
                stmt0 = con.createStatement();
                stmt0.executeUpdate("DROP TABLE IF EXISTS Secondary_Reply;\n" +
                        "DROP TABLE IF EXISTS Reply;\n" +
                        "DROP TABLE IF EXISTS Post_Likes;\n" +
                        "DROP TABLE IF EXISTS Post_Shares;\n" +
                        "DROP TABLE IF EXISTS Post_Followers;\n" +
                        "DROP TABLE IF EXISTS Post_Favorites;\n" +
                        "DROP TABLE IF EXISTS Post_Category;\n" +
                        "DROP TABLE IF EXISTS Category;\n" +
                        "DROP TABLE IF EXISTS Post;\n" +
                        "DROP TABLE IF EXISTS Author;\n" +
                        "DROP TABLE IF EXISTS City;\n"
                );

                con.commit();
                String sql1 = "CREATE TABLE if not exists Author (" +
                        "author_id TEXT PRIMARY KEY," +
                        "author_name TEXT NOT NULL UNIQUE," +
                        "author_reg_time TIMESTAMP NOT NULL," +
                        "author_phone TEXT" +
                        ")";
                stmt0.executeUpdate(sql1);
                con.commit();
                String sql2 = "CREATE TABLE if not exists City (" +
                        "city_id SERIAL PRIMARY KEY," +
                        "city_name TEXT NOT NULL," +
                        "country_name TEXT NOT NULL," +
                        "CONSTRAINT uc_city_country UNIQUE (city_name, country_name)" +
                        ")";
                stmt0.executeUpdate(sql2);
                con.commit();
                String sql3 = "CREATE TABLE if not exists Post (" +
                        "post_id SERIAL PRIMARY KEY," +
                        "title TEXT NOT NULL," +
                        "content TEXT NOT NULL," +
                        "posting_time TIMESTAMP NOT NULL," +
                        "author_id TEXT NOT NULL," +
                        "city_id INTEGER NOT NULL," +
                        "FOREIGN KEY (author_id) REFERENCES Author(author_id)," +
                        "FOREIGN KEY (city_id) REFERENCES City(city_id)" +
                        ")";
                stmt0.executeUpdate(sql3);
                con.commit();
                String sql4 = "CREATE TABLE if not exists Post_Followers (" +
                        "follow_id TEXT    NOT NULL," +
                        "author_id TEXT NOT NULL," +
                        "PRIMARY KEY (follow_id, author_id)," +
                        "FOREIGN KEY (follow_id) REFERENCES Author(author_id)," +
                        "FOREIGN KEY (author_id) REFERENCES Author(author_id)" +
                        ")";
                stmt0.executeUpdate(sql4);
                con.commit();
                String sql5 = "CREATE TABLE if not exists Post_Favorites (" +
                        "post_id INTEGER NOT NULL," +
                        "author_id TEXT NOT NULL," +
                        "PRIMARY KEY (post_id, author_id)," +
                        "FOREIGN KEY (post_id) REFERENCES Post(post_id)," +
                        "FOREIGN KEY (author_id) REFERENCES Author(author_id)" +
                        ")";
                stmt0.executeUpdate(sql5);
                con.commit();
                String sql6 = "CREATE TABLE if not exists Post_Shares (" +
                        "post_id INTEGER NOT NULL," +
                        "author_id TEXT NOT NULL," +
                        "PRIMARY KEY (post_id, author_id)," +
                        "FOREIGN KEY (post_id) REFERENCES Post(post_id)," +
                        "FOREIGN KEY (author_id) REFERENCES Author(author_id)" +
                        ")";
                stmt0.executeUpdate(sql6);
                con.commit();
                String sql7 = "CREATE TABLE  if not exists Post_Likes (" +
                        "post_id INTEGER NOT NULL," +
                        "author_id TEXT NOT NULL," +
                        "PRIMARY KEY (post_id, author_id)," +
                        "FOREIGN KEY (post_id) REFERENCES Post(post_id)," +
                        "FOREIGN KEY (author_id) REFERENCES Author(author_id)" +
                        ")";
                stmt0.executeUpdate(sql7);
                con.commit();
                String sql8 = "CREATE TABLE if not exists Reply (" +
                        "reply_id SERIAL PRIMARY KEY," +
                        "post_id INTEGER NOT NULL," +
                        "reply_content TEXT NOT NULL," +
                        "reply_stars INTEGER NOT NULL," +
                        "reply_author_id TEXT NOT NULL," +
                        "FOREIGN KEY (post_id) REFERENCES Post(post_id)," +
                        "FOREIGN KEY (reply_author_id) REFERENCES Author(author_id)," +
                        " CONSTRAINT rep UNIQUE (post_id, reply_content, reply_stars, reply_author_id)" +
                        ")";
                stmt0.executeUpdate(sql8);
                con.commit();
                String sql9 = "CREATE TABLE if not exists Secondary_Reply (" +
                        "secondary_reply_id SERIAL PRIMARY KEY," +
                        "reply_id INTEGER NOT NULL," +
                        "secondary_reply_content TEXT NOT NULL," +
                        "secondary_reply_stars INTEGER NOT NULL," +
                        "secondary_reply_author_id TEXT NOT NULL," +
                        "FOREIGN KEY (reply_id) REFERENCES Reply(reply_id)," +
                        "FOREIGN KEY (secondary_reply_author_id) REFERENCES Author(author_id)," +
                        "CONSTRAINT rep_sec UNIQUE (reply_id,secondary_reply_content,secondary_reply_stars,secondary_reply_author_id)"
                        +
                        ")";
                stmt0.executeUpdate(sql9);
                con.commit();
                String sql10 = "CREATE TABLE if not exists Category\n"
                        + "(\n"
                        + "    category_id SERIAL PRIMARY KEY,\n"
                        + "    name        TEXT    NOT NULL UNIQUE\n"
                        + ");";
                stmt0.executeUpdate(sql10);
                con.commit();
                String sql11 = "CREATE TABLE if not exists Post_Category\n"
                        + "(\n"
                        + "    post_id     INTEGER NOT NULL,\n"
                        + "    category_id INTEGER NOT NULL,\n"
                        + "    FOREIGN KEY (post_id) REFERENCES Post (post_id),\n"
                        + "    FOREIGN KEY (category_id) REFERENCES Category (category_id),\n"
                        + "    PRIMARY KEY (post_id, category_id)\n"
                        + ");";
                stmt0.executeUpdate(sql11);
                con.commit();
                stmt0.close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private String getGusers(List<String> stringList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : stringList) {
            stringBuilder.append(s).append("~");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

//    private String GgetUsers() {
//        StringBuilder stringBuilder = new StringBuilder();
//        for (Map.Entry<String,Socket> entry : Users.userGroup_socket_map.entrySet()) {
//            stringBuilder.append(entry.getKey());
//            stringBuilder.append("~");
//        }
//        return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
//    }

    public String getUsers() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Socket> entry : Users.user_socket_map.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append("~");
        }
//        return new Message(System.currentTimeMillis(),"Server",m.getSentBy(),stringBuilder.substring(0,stringBuilder.length()-1),MsgType.REQ);
        return stringBuilder.append(Users.user_socket_map.size()).toString();
    }


}
//}
