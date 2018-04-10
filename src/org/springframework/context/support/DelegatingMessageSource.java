
package org.springframework.context.support;

import java.util.Locale;

import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

public class DelegatingMessageSource implements HierarchicalMessageSource {

	private MessageSource parentMessageSource = null;

	public void setParentMessageSource(MessageSource parent) {
		this.parentMessageSource = parent;
	}

	public MessageSource getParentMessageSource() {
		return parentMessageSource;
	}

	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		if (parentMessageSource != null) {
			return parentMessageSource.getMessage(code, args, defaultMessage, locale);
		} else {
			return defaultMessage;
		}
	}

	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		if (parentMessageSource != null) {
			return parentMessageSource.getMessage(code, args, locale);
		} else {
			throw new NoSuchMessageException(code, locale);
		}
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		if (parentMessageSource != null) {
			return parentMessageSource.getMessage(resolvable, locale);
		} else {
			String[] codes = resolvable.getCodes();
			String code = (codes != null && codes.length > 0 ? codes[0] : null);
			throw new NoSuchMessageException(code, locale);
		}
	}

}
