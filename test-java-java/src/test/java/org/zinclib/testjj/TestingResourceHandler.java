package org.zinclib.testjj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONException;
import org.zincapi.HandleRequest;
import org.zincapi.ResourceHandler;
import org.zincapi.Response;
import org.zincapi.jsonapi.Payload;
import org.zincapi.jsonapi.PayloadItem;

public class TestingResourceHandler implements ResourceHandler {
	public class Action {
		private final int delay;
		private final String text;

		public Action(int delay, String text) {
			this.delay = delay;
			this.text = text;
		}
	}

	public class ActionRaft {
		private final List<Action> actions = new ArrayList<Action>();
		
		public void addAction(int delay, String text) {
			actions.add(new Action(delay, text));
		}
	}

	private final Map<Integer, ActionRaft> actions = new TreeMap<Integer, ActionRaft>();
	private int which = 0;
	
	@Override
	public void handle(final HandleRequest hr, final Response response) throws Exception {
		int me = which++;
		if (!actions.containsKey(me)) {
			// I think this should be an error
			return;
		}
		for (final Action a : actions.get(me).actions) {
			if (a.delay == 0) {
				Payload p = new Payload("messages");
				PayloadItem item = p.newItem();
				item.set("text", a.text);
				response.send(p);
			} else
				new Timer().schedule(new TimerTask() {
					public void run() {
						try {
							Payload p = new Payload("messages");
							PayloadItem item = p.newItem();
							item.set("text", a.text);
							response.send(p);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}, a.delay);
		}
	}

	public void immediate(int conn, String text) {
		if (!actions.containsKey(conn))
			actions.put(conn, new ActionRaft());
		ActionRaft raft = actions.get(conn);
		raft.addAction(0, text);
	}

	public void delayed(int conn, int delay, String text) {
		if (!actions.containsKey(conn))
			actions.put(conn, new ActionRaft());
		ActionRaft raft = actions.get(conn);
		raft.addAction(delay, text);
	}
}
