package org.irmacard.androidcardproxy;

import java.lang.reflect.Type;

import net.sf.scuba.smartcards.CommandAPDU;
import net.sf.scuba.smartcards.ProtocolCommand;
import net.sf.scuba.util.Hex;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Helper class to deserialize a ProtocolResponse from json
 *
 */
public class ProtocolCommandDeserializer implements JsonDeserializer<ProtocolCommand> {
	@Override
	public ProtocolCommand deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		return new ProtocolCommand(
				json.getAsJsonObject().get("key").getAsString(), "",
				new CommandAPDU(Hex.hexStringToBytes(json.getAsJsonObject().get("command").getAsString())));
	}
}
