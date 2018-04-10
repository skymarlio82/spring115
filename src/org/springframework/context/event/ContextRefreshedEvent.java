
package org.springframework.context.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

public class ContextRefreshedEvent extends ApplicationEvent {
	
	public ContextRefreshedEvent(ApplicationContext source) {
		super(source);
	}

	public ApplicationContext getApplicationContext() {
		return (ApplicationContext) getSource();
	}
}
