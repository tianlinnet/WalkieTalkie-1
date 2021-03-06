package jp.co.shiratsuki.walkietalkie.bean.websocket;

import java.util.ArrayList;

import jp.co.shiratsuki.walkietalkie.bean.User;

public class ContactsList {

    private String eventName;
    private Data data;

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Data getData() {
        return data;
    }

    public class Data {

        private String userId;
        private String roomId;
        private ArrayList<User> contacts;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getRoomId() {
            return roomId;
        }

        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }

        public ArrayList<User> getContacts() {
            return contacts;
        }

        public void setContacts(ArrayList<User> contacts) {
            this.contacts = contacts;
        }
    }

}
