package org.irmacard.androidcardproxy.messages;

public class ReaderMessage {
	public String type = null;
	public String name = null;
	public String id = null;
	public ReaderMessageArguments arguments = null;
	
	public static String TYPE_EVENT = "event";
	public static String TYPE_COMMAND = "command";
	public static String TYPE_RESPONSE = "response";
	
	public static String NAME_COMMAND_AUTHPIN = "authorizeWithPin";
	public static String NAME_COMMAND_TRANSMIT = "transmitCommandSet";
	public static String NAME_COMMAND_SELECTAPPLET = "selectApplet";
	public static String NAME_COMMAND_IDLE = "idle";

	public static String NAME_EVENT_CARDFOUND = "cardInserted";
	public static String NAME_EVENT_CARDLOST = "cardRemoved";
	public static String NAME_EVENT_CARDREADERFOUND = "cardReaderFound";
	public static String NAME_EVENT_STATUSUPDATE = "statusUpdate";
	public static String NAME_EVENT_TIMEOUT = "timeout";
	public static String NAME_EVENT_DONE = "done";

	public ReaderMessage(String type, String name) {
		this.type = type;
		this.name = name;
	}
	
	public ReaderMessage(String type, String name, String id) {
		this.type = type;
		this.name = name;
		this.id = id;
	}
	
	public ReaderMessage(String type, String name, String id, ReaderMessageArguments arguments) {
		this.type = type;
		this.name = name;
		this.id = id;
		this.arguments = arguments;		
	}

	public String toString() {
		return "<Type: " + type + ", name: " + name + ", id: " + id + ", arguments: " + arguments.toString() + ">";
	}
}
