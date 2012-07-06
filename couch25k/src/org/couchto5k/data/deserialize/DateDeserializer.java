package org.couchto5k.data.deserialize;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

public class DateDeserializer extends JsonDeserializer<Date> {

	private String[] formats = new String[] { "yyyy-MM-dd'T'HH:mm:ss'Z'",
			"yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd HH:mm:ss Z",
			"yyyy-MM-dd HH:mmZ", "yyyy-MM-dd HH:mm", "yyyy-MM-dd" };

	@Override
	public Date deserialize(JsonParser parser, DeserializationContext context)
			throws IOException, JsonProcessingException {
		String dateAsString = parser.getText();
		return parseDateString(dateAsString);
	}

	private Date parseDateString(String dateAsString) throws IOException {
		for (String format : formats) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(format);
			try {
				return dateFormat.parse(dateAsString);
			} catch (ParseException parseException) {
				// bad format
			}
		}
		try {
			// try default parsing
			return DateFormat.getInstance().parse(dateAsString);
		} catch (Exception exception) {
			// hmm, didn't work dude!
			throw new IOException("unknown date");
		}
	}
}
