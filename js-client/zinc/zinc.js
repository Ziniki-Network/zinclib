define('zinc', ['rsvp', 'atmosphere', 'exports'], function(RSVP, atmosphere, exports) {
  "use strict";

  function Connection(atmo) {
    this.atmo = atmo;
    this.nextId = 0;
    this.dispatch = {};
  }

  Connection.prototype.nextHandler = function(handler) {
    var ret = ++this.nextId;
    this.dispatch[ret] = handler;
    return ret;
  }
    
  Connection.prototype.sendJson = function(json) {
    this.atmo.push(JSON.stringify(json));
  }

  Connection.prototype.processIncoming = function(json) {
    var msg = JSON.parse(json);
    if (!msg.subscription)
      return;
    var h = this.dispatch[msg.subscription];
    h(msg.payload);
  }
    
  function Requestor(conn) {
    this.conn = conn;
  }
  
  Requestor.prototype.subscribe = function(resource, handler) {
    if (!handler)
      throw "subscribe requires a handler";
    var req = new MakeRequest(this.conn, "subscribe", handler);
    req.req.resource = resource;
    return req;
  }
  
  Requestor.prototype.create = function(resource, handler) {
    var req = new MakeRequest(this.conn, "create", handler);
    req.req.resource = resource;
    return req;
  }
  
  function MakeRequest(conn, method, handler) {
    this.conn = conn;
    this.handler = handler;
    this.req = {"method": method};
    this.msg = {"request": this.req};
    if (handler)
      this.msg.subscription = conn.nextHandler(handler);
    this.method = method;
  }
  
  MakeRequest.prototype.getHandler = function () {
    return this.handler;
  }
  
  MakeRequest.prototype.setOption = function(opt, val) {
    if (!this.req.options)
      this.req.options = {};
    if (this.req.options[opt])
      throw "Option " + opt + " is already set";
    this.req.options[opt] = val;
    return this;
  }

  MakeRequest.prototype.setPayload = function(json) {
    if (this.msg.payload)
      throw "Cannot set the payload more than once";
    this.msg.payload = json;
    return this;
  }
  
  MakeRequest.prototype.send = function() {
    this.conn.sendJson(this.msg);
  }
  
  MakeRequest.prototype.unsubscribe = function() {
    if (!this.msg.subscription)
      throw "There is no subscription to unsubscribe"
    this.conn.sendJson({subscription: this.msg.subscription, request: {method: "unsubscribe"}});
  }
  
  exports.newRequestor = function(uri) {
    return new RSVP.Promise(function(resolve, reject) {
      var atmo, conn;
      var req = new atmosphere.AtmosphereRequest({
        url: uri
      });
      req.url = uri;
      req.transport = 'websocket';
      req.fallbackTransport = 'long-polling';
      req.onOpen = function() {
        var msg = {"request":{"method":"establish"}};
        conn.sendJson(msg);
        resolve(new Requestor(conn));
      };
      req.onMessage = function(msg) {
        if (!msg || !msg.status || msg.status != 200 || !msg.responseBody)
          console.log("invalid message received", msg);
        conn.processIncoming(msg.responseBody);
      };
      atmo = atmosphere.subscribe(req);
      conn = new Connection(atmo);
    });
  };
});