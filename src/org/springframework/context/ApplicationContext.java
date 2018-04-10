
package org.springframework.context;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.io.support.ResourcePatternResolver;

public interface ApplicationContext extends ListableBeanFactory, HierarchicalBeanFactory, MessageSource, ApplicationEventPublisher, ResourcePatternResolver {
	
	ApplicationContext getParent();
	
	String getDisplayName();
	
	long getStartupDate();
	
	void publishEvent(ApplicationEvent event);
}
