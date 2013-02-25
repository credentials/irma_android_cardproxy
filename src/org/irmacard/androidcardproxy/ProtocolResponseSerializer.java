package org.irmacard.androidcardproxy;

import java.lang.reflect.Type;

import net.sourceforge.scuba.smartcards.ProtocolResponse;
import net.sourceforge.scuba.util.Hex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Helper class to serialize ProtocolResponse to JSON.
 *
 */
public class ProtocolResponseSerializer implements JsonSerializer<ProtocolResponse> {
	@Override
	public JsonElement serialize(ProtocolResponse src, Type typeOfSrc,
			JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("key", src.getKey());
		obj.addProperty("apdu", Hex.bytesToHexString(src.getAPDU().getBytes()));
		return obj;
	}
}
