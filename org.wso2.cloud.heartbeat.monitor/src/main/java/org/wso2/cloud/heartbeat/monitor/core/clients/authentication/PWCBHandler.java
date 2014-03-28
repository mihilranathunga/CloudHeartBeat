package org.wso2.cloud.heartbeat.monitor.core.clients.authentication;

import org.apache.ws.security.WSPasswordCallback;
 
    import javax.security.auth.callback.Callback;
    import javax.security.auth.callback.CallbackHandler;
    import javax.security.auth.callback.UnsupportedCallbackException;
 
    public class PWCBHandler implements CallbackHandler {
 
     public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
      WSPasswordCallback cb = (WSPasswordCallback) callbacks[0];
      if ("prabath".equals(cb.getIdentifier())) {
       cb.setPassword("prabath");
      } else {
       cb.setPassword("wso2carbon");
      }
     }
    }