package org.zinclib.testjj;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import static org.junit.Assert.*;

import org.zincapi.MakeRequest;
import org.zincapi.ResponseHandler;

import com.gmmapowell.sync.SyncUtils;

public class TestingResponseHandler implements ResponseHandler {
	private final List<Exception> errors = new ArrayList<Exception>();
	private final List<String> responses = new ArrayList<String>();

	@Override
	public void response(MakeRequest req, JSONObject payload) {
		synchronized (responses) {
			try {
				responses.add(payload.getJSONObject("message").getString("text"));
				responses.notify();
			} catch (JSONException e) {
				errors.add(e);
			}
		}
	}

	protected void throwErrors() throws Exception {
		if (!errors.isEmpty())
			throw errors.get(0);
	}

	protected void assertMessages(String... strings) {
		synchronized (responses) {
			Date d = new Date(new Date().getTime()+100);
			while (d.after(new Date()) && responses.size() != strings.length)
				SyncUtils.waitUntil(responses, d);
			assertEquals("Incorrect number of responses seen", strings.length, responses.size());
			for (int i=0;i<strings.length;i++)
				assertEquals("Incorrect response " + i, strings[i], responses.get(i));
		}
	}
}
