
package com.test.spring.example;

public class JaxRpcSimpleTestService implements SimpleTestService {
	
	private HelloWorld helloWorld = null;
	
//	private static JaxRpcSimpleTestService instance = null;
//	
//	public static SimpleTestService newJaxRpcSimpleTestServiceInstance() {
//		if (instance == null) {
//			instance = new JaxRpcSimpleTestService();
//		}
//		System.out.println("static instance : " + instance);
//		return instance;
//	}
	
	public String getTestMesssage(String name) {
		return getHelloWorld().getMessage(name);
	}
	
	public HelloWorld getHelloWorld() {
		return helloWorld;
	}

	public void setHelloWorld(HelloWorld helloWorld) {
		this.helloWorld = helloWorld;
	}
	
//	public Car newCarInstance() {
//		return new Car();
//	}
//	
//	public Bus newBusInstance() {
//		return new Bus();
//	}
}
