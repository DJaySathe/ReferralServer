package drools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActorMessage {
    public String Timestamp;
    public String ActorName;
    public String SendRecv;
    public String MessageType;
    public String Info;

    @Override
    public String toString() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date(Long.parseLong(Timestamp));
        return "[" + dateFormat.format(date) +
                "] [ " + ActorName +
                " ] [ " + SendRecv +
                " ] [ " + MessageType +
                " ] [ " + Info +
                " ]";
    }

//    @Override
//    public String toString() {
//        return "[" +
//                "Timestamp='" + Timestamp + '\'' +
//                ", ActorName='" + ActorName + '\'' +
//                ", SendRecv='" + SendRecv + '\'' +
//                ", MessageType='" + MessageType + '\'' +
//                ", Info='" + Info + '\'' +
//                ']';
//    }

    public String getInfo() {
        return Info;
    }

    public void setInfo(String info) {
        Info = info;
    }

    public ActorMessage(String timestamp, String actorName, String sendRecv, String messageType, String info) {
        Timestamp = timestamp;
        ActorName = actorName;
        SendRecv = sendRecv;
        MessageType = messageType;
        Info = info;
    }

    public String getTimestamp() {
        return Timestamp;
    }

    public void setTimestamp(String timestamp) {
        Timestamp = timestamp;
    }

    public String getActorName() {
        return ActorName;
    }

    public void setActorName(String actorName) {
        ActorName = actorName;
    }

    public String getSendRecv() {
        return SendRecv;
    }

    public void setSendRecv(String sendRecv) {
        SendRecv = sendRecv;
    }

    public String getMessageType() {
        return MessageType;
    }

    public void setMessageType(String messageType) {
        MessageType = messageType;
    }
}