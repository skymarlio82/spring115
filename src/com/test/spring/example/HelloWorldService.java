
package com.test.spring.example;

public class HelloWorldService implements HelloWorld {

	private String defaultName = null;
		
//	public HelloWorldService(String defaultName) {
//		this.defaultName = defaultName;
//	}
	
	public String getMessage(String name) {
		return "Hello, world! <" + ((name != null && !"".equals(name)) ? name : defaultName) + ">";
	}
}
