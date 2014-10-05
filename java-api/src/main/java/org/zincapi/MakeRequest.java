package org.zincapi;

import org.zincapi.jsonapi.Payload;
import org.zinutils.sync.Promise;

public interface MakeRequest {

	/** Set an option on the request.
	 * Options stand to one side of the actual payload or resource of the request, in much the same way that query parameters do in URIs
	 * @param opt the name of the option
	 * @param val the value of the option
	 * @return this object
	 */
	MakeRequest setOption(String opt, Object val);

	/** Set the request payload.
	 * The payload should be formatted as a JSONAPI structure, and the Payload class helps you to achieve this.
	 * @param payload the payload to send
	 * @return this object
	 * @throws JSONException 
	 */
	MakeRequest setPayload(Payload payload);

	/** It is also possible to set a payload to be a string.
	 * There are two use cases for this: if you happen to have a JSONAPI payload formatted as a string, it is possible to use it directly;
	 * if you have a "non-conforming" client and server, which have agreed not to use JSONAPI format, then this can be used to send non-JSONAPI strings.
	 * @param payload the payload as a string
	 * @return this object
	 * @throws JSONException 
	 */
	MakeRequest setPayload(String payload);

	/** Send the request.
	 * Until send is called, the request will just be hanging in space.  It can be configured but it will not act.
	 * @throws JSONException
	 */
	Promise<String> send();

	/** Recover the handler associated with this request.
	 * This is a convenience method for users who wish to maintain just a pointer to the request. 
	 * @return the handler associated with this request
	 */
	ResponseHandler getHandler();

	/** Unsubscribe from further updates.
	 * If the request specified a handler for updates, these will continue to come until cancelled either by the client or the server.
	 * This is the client method to cancel further updates.
	 * @throws JSONException
	 */
	void unsubscribe();
}
