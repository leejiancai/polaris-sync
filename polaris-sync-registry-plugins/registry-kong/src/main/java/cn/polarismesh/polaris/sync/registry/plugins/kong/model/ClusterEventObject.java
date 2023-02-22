package cn.polarismesh.polaris.sync.registry.plugins.kong.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ClusterEventObject {
    private String id;
    private String nodeId;

    private String at;
    private String expireAt;
    private String channel;
    private String data;

    public String getId() {
        return id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getAt() {
        return at;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public String getChannel() {
        return channel;
    }

    public String getData() {
        return data;
    }


    public ClusterEventObject() {}

    public ClusterEventObject(Builder builder) {
        this.id = builder.id;
        this.nodeId = builder.nodeId;
        this.at = builder.at;
        this.expireAt = builder.at;
        this.channel = builder.channel;
        this.data = builder.data;
    }
    public static class Builder {
        private String id;
        private static final String nodeId = "00000000-0000-0000-0000-000000000000";
        private String at;
        private String expireAt;
        private String channel;

        private String data;

        private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        public Builder() {
            this.id = UUID.randomUUID().toString();
            LocalDateTime at = LocalDateTime.now();
            this.at = dtf.format(at);
            this.expireAt = dtf.format(at.plusHours(1));
        }

        public Builder setChannel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder setData(String data) {
            this.data = data;
            return this;
        }

        public ClusterEventObject build() {
            return new ClusterEventObject(this);
        }

    }

}
