// navigator = { userAgent: "rhino" };
// window = { };

function WebSocket() {
  org.zinclib.testjsj.RhinoSupport.openWebSocket(this, arguments);
  java.lang.System.out.println("created websocket");
  return this;
}

WebSocket.prototype.send = function(s) {
  java.lang.System.out.println("send");
}

function MessageEvent(data) {
  this.data = data;
  return this;
}
