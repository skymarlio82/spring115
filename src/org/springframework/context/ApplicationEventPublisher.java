
package org.springframework.context;

public interface ApplicationEventPublisher {
    
	void publishEvent(ApplicationEvent event);

}
