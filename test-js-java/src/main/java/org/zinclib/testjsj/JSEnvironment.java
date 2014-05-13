package org.zinclib.testjsj;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class JSEnvironment {
	private final Context context;
	private final ScriptableObject scope;

	public JSEnvironment(Context context, ScriptableObject scope) {
		this.context = context;
		this.scope = scope;
	}

	public Object eval(String jsexpr) {
		int lineno = 1;
		Object securityDomain = null;
		return context.evaluateString(scope, jsexpr, "sourceFile", lineno, securityDomain);
	}

}
