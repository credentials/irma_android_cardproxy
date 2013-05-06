package org.irmacard.androidcardproxy.messages;

import java.util.HashMap;
import java.util.Map;

public class EventArguments extends ReaderMessageArguments {
	public Map<String,String> data;
	public EventArguments withEntry(String key, String value) {
		if (data == null) {
			data = new HashMap<String, String>();
		}
		data.put(key, value);
		return this;
	}
}
