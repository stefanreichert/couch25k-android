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

	private String[] formats = new String[] { "yyyy-MM-DD HH:mm:ss",
			"yyyy-MM-DD HH:mm:ss Z", "yyyy-MM-dd'T'HH:mm:ss'Z'" };

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
			return DateFormat.getInstance().parse(dateAsString);
		} catch (Exception exception) {
			throw new IOException("unknown date");
		}
	}
}
