
package com.dc.piglet.rtmp.core.protocol;

import com.dc.piglet.rtmp.client.ClientOptions;
import com.dc.piglet.rtmp.core.protocol.amf.Amf0Object;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 命令消息
 * 对端执行某些操作的命令消息
 */

public abstract class Command extends AbstractMessage {
    
    protected String name;
    protected int transactionId;
    protected Amf0Object object;
    protected Object[] args;

    public Command(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }
    
    public Command(int transactionId, String name, Amf0Object object, Object ... args) {
        this.transactionId = transactionId;
        this.name = name;
        this.object = object;
        this.args = args;
    }

    public Command(String name, Amf0Object object, Object ... args) {
        this(0, name, object, args);
    }

    public Amf0Object getObject() {
        return object;
    }

    public Object getArg(int index) {
        return args[index];
    }

    public int getArgCount() {
        if(args == null) {
            return 0;
        }
        return args.length;
    }

    public static enum OnStatus {
        
        ERROR, STATUS, WARNING;        
        
        public static OnStatus parse(final String raw) {
            return OnStatus.valueOf(raw.substring(1).toUpperCase());
        }

        public String asString() {
            return "_" + this.name().toLowerCase();
        }
        
    }

    private static Amf0Object onStatus(final OnStatus level, final String code,
            final String description, final String details, final Pair ... pairs) {
        final Amf0Object object = object(
            pair("level", level.asString()),
            pair("code", code));
        if(description != null) {
            object.put("description", description);
        }
        if(details != null) {
            object.put("details", details);
        }
        return object(object, pairs);
    }

    private static Amf0Object onStatus(final OnStatus level, final String code,
            final String description, final Pair ... pairs) {
        return onStatus(level, code, description, null, pairs);
    }

    public static Amf0Object onStatus(final OnStatus level, final String code, final Pair ... pairs) {
        return onStatus(level, code, null, null, pairs);
    }


    public static Command connect(ClientOptions options) {
        Amf0Object object = object(
            AbstractMessage.pair("app", options.getAppName()),
            AbstractMessage.pair("flashVer", "LNX 11,1,102,55"),
            AbstractMessage.pair("tcUrl", options.getTcUrl()),
            AbstractMessage.pair("fpad", false),
            AbstractMessage.pair("capabilities", 239.0),
            AbstractMessage.pair("audioCodecs", 3575.0),
            AbstractMessage.pair("videoCodecs", 252.0),
            AbstractMessage.pair("videoFunction", 1.0),
            AbstractMessage.pair("objectEncoding", 0.0));
        if(options.getParams() != null) {
            object.putAll(options.getParams());
        }
        return new CommandAmf0("connect", object, options.getArgs());
    }

    public static Command connectSuccess(int transactionId) {
        Map<String, Object> object = onStatus(OnStatus.STATUS,
            "NetConnection.Connect.Success", "Connection succeeded.",            
            pair("fmsVer", "FMS/3,5,1,516"),
            pair("capabilities", 31.0),
            pair("mode", 1.0),
            pair("objectEncoding", 0.0));
        return new CommandAmf0(transactionId, "_result", null, object);
    }

    public static Command createStream() {
        return new CommandAmf0("createStream", null);
    }

    public static Command onBWDone() {
        return new CommandAmf0("onBWDone", null);
    }

    public static Command createStreamSuccess(int transactionId, int streamId) {
        return new CommandAmf0(transactionId, "_result", null, streamId);
    }

    public static Command play(int streamId, ClientOptions options) {
        final List<Object> playArgs = new ArrayList<Object>();
        playArgs.add(options.getStreamName());
        if(options.getStart() != -2 || options.getArgs() != null) {
            playArgs.add(options.getStart());
        }
        if(options.getLength() != -1 || options.getArgs() != null) {
            playArgs.add(options.getLength());
        }
        if(options.getArgs() != null) {
            playArgs.addAll(Arrays.asList(options.getArgs()));
        }
        Command command = new CommandAmf0("play", null, playArgs.toArray());
        command.header.setCsId(8);
        command.header.setStreamId(streamId);        
        return command;
    }

    private static Command playStatus(String code, String description, String playName, String clientId, Pair ... pairs) {
        Amf0Object status = onStatus(OnStatus.STATUS,
                "NetStream.Play." + code, description + " " + playName + ".",
                pair("details", playName),
                pair("clientid", clientId));
        object(status, pairs);
        Command command = new CommandAmf0("onStatus", null, status);
        command.header.setCsId(5);
        return command;
    }

    public static Command playReset(String playName, String clientId) {
        Command command = playStatus("Reset", "Playing and resetting", playName, clientId);
        command.header.setCsId(4); // ?
        return command;
    }

    public static Command playStart(String playName, String clientId) {
        Command play = playStatus("Start", "Started playing", playName, clientId);
        return play;
    }

    public static Command playStop(String playName, String clientId) {
        return playStatus("Stop", "Stopped playing", playName, clientId);
    }

    public static Command playFailed(String playName, String clientId) {
        Amf0Object status = onStatus(OnStatus.ERROR,
                "NetStream.Play.Failed", "Stream not found");
        Command command = new CommandAmf0("onStatus", null, status);
        command.header.setCsId(8);
        return command;
    }

    public static Command seekNotify(int streamId, int seekTime, String playName, String clientId) {
        Amf0Object status = onStatus(OnStatus.STATUS,
                "NetStream.Seek.Notify", "Seeking " + seekTime + " (stream ID: " + streamId + ").",
                pair("details", playName),
                pair("clientid", clientId));        
        Command command = new CommandAmf0("onStatus", null, status);
        command.header.setCsId(5);
        command.header.setStreamId(streamId);
        command.header.setTimestamp(seekTime);
        return command;
    }

    public static Command pauseNotify(String playName, String clientId) {
        Amf0Object status = onStatus(OnStatus.STATUS,
                "NetStream.Pause.Notify", "Pausing " + playName,
                pair("details", playName),
                pair("clientid", clientId));
        Command command = new CommandAmf0("onStatus", null, status);
        command.header.setCsId(5);
        return command;
    }

    public static Command unpauseNotify(String playName, String clientId) {
        Amf0Object status = onStatus(OnStatus.STATUS,
                "NetStream.Unpause.Notify", "Unpausing " + playName,
                pair("details", playName),
                pair("clientid", clientId));
        Command command = new CommandAmf0("onStatus", null, status);
        command.header.setCsId(5);
        return command;
    }
    
    public static Command publish(int streamId, int channelId, ClientOptions options) { // TODO
        Command command = new CommandAmf0("publish", null, options.getStreamName(),
                options.getPublishType().asString());
        command.header.setCsId(channelId);
        command.header.setStreamId(streamId);
        return command;
    }
    
	private static Command publishStatus(String code, String streamName, String clientId, Pair ... pairs) {
        Amf0Object status = onStatus(OnStatus.STATUS,
                code, null, streamName,
                pair("details", streamName),
                pair("clientid", clientId));
        object(status, pairs);
        Command command = new CommandAmf0("onStatus", null, status);
        command.header.setCsId(8);
        return command;
    }

    public static Command publishStart(String streamName, String clientId, int streamId) {
        return publishStatus("NetStream.Publish.Start", streamName, clientId);
    }

    public static Command unpublishSuccess(String streamName, String clientId, int streamId) {
        return publishStatus("NetStream.Unpublish.Success", streamName, clientId);
    }

    public static Command unpublish(int streamId) {
        Command command = new CommandAmf0("publish", null, false);
        command.header.setCsId(8);
        command.header.setStreamId(streamId);
        return command;
    }

    public static Command publishBadName(int streamId) {
        Command command = new CommandAmf0("onStatus", null, 
                onStatus(OnStatus.ERROR, "NetStream.Publish.BadName", "Stream already exists."));
        command.header.setCsId(8);
        command.header.setStreamId(streamId);
        return command;
    }

    public static Command publishNotify(int streamId) {
        Command command = new CommandAmf0("onStatus", null,
                onStatus(OnStatus.STATUS, "NetStream.Play.PublishNotify"));
        command.header.setCsId(8);
        command.header.setStreamId(streamId);
        return command;
    }

    public static Command unpublishNotify(int streamId) {
        Command command = new CommandAmf0("onStatus", null,
                onStatus(OnStatus.STATUS, "NetStream.Play.UnpublishNotify"));
        command.header.setCsId(8);
        command.header.setStreamId(streamId);
        return command;
    }

    public static Command closeStream(int streamId) {
        Command command = new CommandAmf0("closeStream", null);
        command.header.setCsId(8);
        command.header.setStreamId(streamId);
        return command;
    }


    public String getName() {
        return name;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(super.toString());        
        sb.append("name: ").append(name);
        sb.append(", transactionId: ").append(transactionId);
        sb.append(", object: ").append(object);
        sb.append(", args: ").append(Arrays.toString(args));
        return sb.toString();
    }

}
