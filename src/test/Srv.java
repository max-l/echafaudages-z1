package test;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.AbstractService;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;


public class Srv {

	HashMap<Long, String> _sessions = new HashMap<Long, String>();
	
	class SessionListenerImpl implements BayeuxServer.SessionListener {

		public void sessionAdded(ServerSession session) {
			
			Long internalId = new Long(0);
			session.setAttribute("INTERNAL_ID", internalId);
			_sessions.put(0L, session.getId());
		}

		public void sessionRemoved(ServerSession session, boolean timedout) {			
			
			Object k = (Long) session.getAttribute("INTERNAL_ID");
			_sessions.remove(k);
		}
	}
	
	BayeuxServer bayeuxServer;
	
	long getInternalIdByBayeuxSessionId(String bayeuxSessionId) {
		return 
		  ((Long)bayeuxServer.getSession(bayeuxSessionId).getAttribute("INTERNAL_ID")).longValue();
	}
	
	
	public static void main(String[] args) throws Exception {
		new Srv().start();
	}		 
	
	public void start() throws Exception {
        Server server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(8080);
        server.addConnector(connector);
                                                
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { 
        		_createFileHandler(),
        		_createBayeuxServerContext(),
        		new DefaultHandler() });
        server.setHandler(handlers);

        
        server.start();
        
        _publishAtLarge();
        
        server.join();        
    }	

	private void _publishAtLarge() {
	    new Thread() {        	
	    	public void run() {
	    		for(int i = 1; i < 100000000; i ++) {        	        
	
	    			if(bayeuxServer == null)
	    				sleep_(3);
	    			else {
	        	        for(ServerSession s : bayeuxServer.getSessions()) {
	
	        	        	sleep_(3);
	        	            Map<String, Object> output = new HashMap<String, Object>();
	        	            output.put("greeting", "--->, " + i);
	        	                        
	        	            s.deliver(s, "/hello", output, null);        
	        	        }
	    			}
	    		}
	    	}
	    }.start();
	}
	
	private void sleep_(int s) {
	    try {
			Thread.sleep(1000 * s);
		} 
	    catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
    
	private Handler _createBayeuxServerContext() {
		
	    ServletContextHandler bayCtx = new ServletContextHandler(ServletContextHandler.SESSIONS);
	    ServletHolder sh = bayCtx.addServlet(org.cometd.server.CometdServlet.class, "/cometd/*");

	    sh.setInitParameter("logLevel", "3");

	    bayCtx.addEventListener(new ServletContextAttributeListener() {
			public void attributeAdded(ServletContextAttributeEvent event) {
								
				if (BayeuxServer.ATTRIBUTE.equals(event.getName())) {
					bayeuxServer = (BayeuxServer) event.getValue();
			        new HelloService(bayeuxServer);
			        bayeuxServer.addListener(new SessionListenerImpl());			        
				}				
			}
			public void attributeRemoved(ServletContextAttributeEvent scab) {}
			public void attributeReplaced(ServletContextAttributeEvent scab) {}        	
	    });
	    
	    bayCtx.setContextPath("/"); 

	    return bayCtx;
	}

	
	private ResourceHandler _createFileHandler() {
	        		 
        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{ "index.html" });        
        resource_handler.setResourceBase("D:/dev/comet-emb/emb/src/web");	      
        return resource_handler;
   }
}

class HelloService extends AbstractService {

    public HelloService(BayeuxServer bayeux) {
        super(bayeux, "hello");
        addService("/service/hello", "processHello");
    }

    public void processHello(ServerSession remote, Message message) {
        Map<String, Object> input = message.getDataAsMap();
        String name = (String)input.get("name");

        Map<String, Object> output = new HashMap<String, Object>();
        output.put("greeting", "Hellozzz, " + name);
        ServerSession ss = getServerSession();
        //String clientId = remote.getId();        
        remote.deliver(ss, "/hello", output, null);
    }
}
