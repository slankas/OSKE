package edu.ncsu.las.model;

import org.json.JSONObject;

public class EmailConfiguration {

	private final String _server;
	private final int    _port;
	private final String _user;
	private final String _password;
	
	public EmailConfiguration(String server, int port, String user, String password) {
		_server = server;
		_port   = port;
		_user   = user;
		_password = password;
	}
	
	public EmailConfiguration(JSONObject config) {
		_server   = config.getString("server");
		_port     = config.getInt("port");
		_user     = config.getString("user");
		_password = config.getString("password");
	}
		
	public String getServer() {
		return _server;
	}
	public int getPort() {
		return _port;
	}
	public String getUser() {
		return _user;
	}
	public String getPassword() {
		return _password;
	}
	
	
}
