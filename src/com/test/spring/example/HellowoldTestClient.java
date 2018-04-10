
package com.test.spring.example;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class HellowoldTestClient {

	public HellowoldTestClient() {
		
	}
	
	public static void main(String[] args) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-bean-config.xml");
		SimpleTestService simpleTestService = (SimpleTestService)applicationContext.getBean("jaxRpcSimpleTestService");
		System.out.println(simpleTestService.getTestMesssage(""));
//		System.out.println("instance from BeanFactory : " + simpleTestService);
//		Car car = (Car)applicationContext.getBean("car1");
//		car.getTransport();
//		Bus bus = (Bus)applicationContext.getBean("bus1");
//		bus.getTransport();
	}
}
