import {Promise} from "rsvp";
import atmosphere from "atmosphere";

"use strict";

var zincConfig = {
  hbTimeout: 90000,
  reconnInterval: 2500
};
export {zincConfig as config};

function ZincError(message) {
    this.name = "ZincError";
    this.message = message;
    this.stack = (new Error()).stack;
}
ZincError.prototype = new Error();

function Connection(req) {
  var self = this;
  this.isBroken = false;
  this.sentEstablish = false;
  this.req = req;
  this.promises = {};
  this.openSubscriptions = {};
  this.req.onError = function(e) {
    console.log("saw error " + new Date());
    self.isBroken = true;
    self.sentEstablish = false;
    self.reconnecting = setTimeout(function() {
      if (self.isBroken) {
        console.log("attempting to restore connection");
        self.atmo = atmosphere.subscribe(req);
        self.connect();
        self.isBroken = false;
      }
    }, 2500);
  };
  req.logLevel = 'debug';
  this.atmo = atmosphere.subscribe(this.req);
  this.nextId = 0;
  this.dispatch = {};
  this.heartbeatInterval = setInterval(function() {
    if (self.sentEstablish) {
      console.log("sending heartbeat");
      self.atmo.push(JSON.stringify({"request":{"method":"heartbeat"}}));
    } else
      console.log("timer fired with nothing to do");
  }, zincConfig.hbTimeout);
}

Connection.prototype.connect = function() {
  var self = this;
  return new Promise(function(resolve, reject) {
    console.log("sending establish");
    var msg = {"request":{"method":"establish"}};
    self.sendJson(msg);
    self.sentEstablish = true;
    console.log("resubscribing");
//      console.log("subs = " + JSON.stringify(self.openSubscriptions));
    for (var v in self.openSubscriptions)
      if (self.openSubscriptions.hasOwnProperty(v)) {
        console.log("Need to re-subscribe to " + JSON.stringify(self.openSubscriptions[v].msg));
        self.sendJson(self.openSubscriptions[v].msg);
      }
    resolve(null);
  });
}

Connection.prototype.disconnect = function() {
  clearInterval(this.heartbeatInterval);
  atmosphere.unsubscribe();
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
  if (msg.requestid)
    this.handlePromise(msg.requestid, msg);
  if (!msg.subscription)
    return;
  var h = this.dispatch[msg.subscription];
  this.handlePromise(msg.subscription, msg);
  if (msg.action)
    h(msg.payload, msg.action);
  else
    h(msg.payload, 'replace');
}

Connection.prototype.handlePromise = function(id, json) {
  var p = this.promises[id];
  if (!p)
    return;
  if (json.error)
    p.reject(new ZincError(json.error));
  else if (json.status)
    p.resolve(json.status);
  else
    p.resolve(null);
  delete this.promises[id];
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

Requestor.prototype.invoke = function(resource, handler) {
  var req = new MakeRequest(this.conn, "invoke", handler);
  req.req.resource = resource;
  return req;
}

Requestor.prototype.disconnect = function() {
  this.conn.disconnect();
}

Requestor.prototype.toString = function() {
  return "Requestor[" + this.conn.req.url + "]";
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
  var req = this;
  var msg = this.msg;
  var conn = this.conn;
  return new Promise(function(resolve, reject) {
    var id;
    if (msg.subscription) {
      id = msg.subscription;
      conn.openSubscriptions[id] = req;
    } else
      id = msg.requestid = ++conn.nextId;
    conn.promises[id] = { resolve: resolve, reject: reject };
    console.log("sending message", msg);
    conn.sendJson(msg);
  });
}

MakeRequest.prototype.unsubscribe = function() {
  if (!this.msg.subscription)
    throw "There is no subscription to unsubscribe"
  this.conn.sendJson({subscription: this.msg.subscription, request: {method: "unsubscribe"}});
  delete this.conn.openSubscriptions[this.msg.subscription];
}

export default {
  newRequestor: function(uri) {
    return new Promise(function(resolve, reject) {
      var atmo, conn;
      var req = new atmosphere.AtmosphereRequest({
        url: uri
      });
      req.url = uri;
      req.transport = 'websocket';
      req.fallbackTransport = 'long-polling';
      req.onOpen = function() {
        conn.connect().then(function(x) { resolve(new Requestor(conn)) });
      };
      req.onMessage = function(msg) {
        if (!msg || !msg.status || msg.status != 200 || !msg.responseBody)
          console.log("invalid message received", msg);
        conn.processIncoming(msg.responseBody);
      };
      conn = new Connection(req);
    });
  }
};
