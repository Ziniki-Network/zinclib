package org.zincapi;

public interface Requestor {

	MakeRequest subscribe(String resource, ResponseHandler handler);

	MakeRequest create(String resource, ResponseHandler handler);

	MakeRequest invoke(String resource, ResponseHandler handler);

	Connection getConnection();

}
