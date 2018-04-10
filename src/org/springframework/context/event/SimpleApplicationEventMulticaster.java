
package org.springframework.context.event;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class SimpleApplicationEventMulticaster implements ApplicationEventMulticaster {

	private final Set applicationListeners = new HashSet();

	public void addApplicationListener(ApplicationListener listener) {
		applicationListeners.add(listener);
	}

	public void removeApplicationListener(ApplicationListener listener) {
		applicationListeners.remove(listener);
	}

	public void removeAllListeners() {
		applicationListeners.clear();
	}

	public void multicastEvent(ApplicationEvent event) {
		Iterator it = applicationListeners.iterator();
		while (it.hasNext()) {
			ApplicationListener listener = (ApplicationListener) it.next();
			listener.onApplicationEvent(event);
		}
	}
}
