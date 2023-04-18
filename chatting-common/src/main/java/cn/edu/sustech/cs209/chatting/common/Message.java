package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;

public class Message implements Serializable {

    private Long timestamp;

    private String sentBy;

    private String sendTo;

    private String data;

    public Message(Long timestamp, String sentBy, String sendTo, String data) {
        super();
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

//    public String MsgToString(){
//        String a = timestamp.toString();
//        StringBuilder answer = new StringBuilder();
//        answer.append(a+"~");answer.append(sentBy+"~");
//        answer.append(sendTo+" ~");answer.append(data);
//        return answer.toString();
//    }
}
