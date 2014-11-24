package org.zincapi.inline.server;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResponse;
import org.zincapi.Zinc;
import org.zinutils.http.ws.AsyncProcessor;
import org.zinutils.reflection.Reflection;

@SuppressWarnings("serial")
public class ZincServlet extends HttpServlet {
    private final AtmosphereFramework framework;
	private final Zinc zinc;
	private Object global;

	public ZincServlet() {
		zinc = new Zinc();
		framework = new AtmosphereFramework(false, false);
    }

	public Zinc getZinc() {
		return zinc;
	}
	
	public Object getState() {
		return global;
	}
	
    @Override
    public void init(ServletConfig config) throws ServletException {
    	super.init(config);
    	try {
    		String ip = config.getInitParameter("org.zincapi.server.init");
    		if (ip != null)
    			global = Reflection.callStatic(ip, "initZinc", zinc);
    	} catch (Exception ex) {
    		throw new ServletException(ex);
    	}
		try {
			AsyncProcessor support = new AsyncProcessor(framework.getAtmosphereConfig());
			support.morphToTextMessages();
			framework.setAsyncSupport(support);
			framework.allowAllClassesScan(false);
			framework.addAtmosphereHandler("/*", new AtmosphereResponder(zinc));
	    	framework.init(config);
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
    }
    
    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}
    
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}

	private void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		AtmosphereRequest areq = new AtmosphereRequest.Builder().request(req).build();
		AtmosphereResponse.Builder builder = new AtmosphereResponse.Builder();
		AtmosphereResponse aresp = builder.response(resp).request(areq).build();
		framework.doCometSupport(areq, aresp);
	}
}
