package org.zinclib.testjsj;

import java.io.IOException;

import org.mozilla.javascript.ScriptableObject;

public class RhinoSupport {

	public static Object openWebSocket(ScriptableObject myThis, ScriptableObject args) throws IOException {
		WSWrapper thr = new WSWrapper(myThis, (String) args.get(0));
		thr.setName("WSThr");
		thr.setDaemon(true);
		thr.start();
		return null;
	}
}
