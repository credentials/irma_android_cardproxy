package org.irmacard.androidcardproxy.messages;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class ReaderMessageDeserializer implements JsonDeserializer<ReaderMessage> {

	@Override
	public ReaderMessage deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		// TODO Auto-generated method stub
		ReaderMessage rm = new ReaderMessage(
				json.getAsJsonObject().get("type").getAsString(), 
				json.getAsJsonObject().get("name").getAsString(), 
				json.getAsJsonObject().get("id").getAsString());
		if (rm.type.equals("event")) {
			rm.arguments = context.deserialize(json.getAsJsonObject().get("arguments"), EventArguments.class);
		} else if (rm.type.equals("command")) {
			if (rm.name.equals("transmitCommandSet")) {
				rm.arguments = context.deserialize(json.getAsJsonObject().get("arguments"), TransmitCommandSetArguments.class);
			} else if (rm.name.equals("selectApplet")) {
				rm.arguments = context.deserialize(json.getAsJsonObject().get("arguments"), SelectAppletArguments.class);
			}
		}
		
		return rm;
	}

}
