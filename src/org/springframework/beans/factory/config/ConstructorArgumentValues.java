
package org.springframework.beans.factory.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstructorArgumentValues {

	private final Map indexedArgumentValues = new HashMap();
	private final Set genericArgumentValues = new HashSet();
	
	public ConstructorArgumentValues() {
		
	}
	
	public ConstructorArgumentValues(ConstructorArgumentValues other) {
		addArgumentValues(other);
	}
	
	public void addArgumentValues(ConstructorArgumentValues other) {
		if (other != null) {
			genericArgumentValues.addAll(other.genericArgumentValues);
			indexedArgumentValues.putAll(other.indexedArgumentValues);
		}
	}
	
	public void addIndexedArgumentValue(int index, Object value) {
		indexedArgumentValues.put(new Integer(index), new ValueHolder(value));
	}
	
	public void addIndexedArgumentValue(int index, Object value, String type) {
		indexedArgumentValues.put(new Integer(index), new ValueHolder(value, type));
	}
	
	public ValueHolder getIndexedArgumentValue(int index, Class requiredType) {
		ValueHolder valueHolder = (ValueHolder)indexedArgumentValues.get(new Integer(index));
		if (valueHolder != null) {
			if (valueHolder.getType() == null || requiredType.getName().equals(valueHolder.getType())) {
				return valueHolder;
			}
		}
		return null;
	}
	
	public Map getIndexedArgumentValues() {
		return indexedArgumentValues;
	}
	
	public void addGenericArgumentValue(Object value) {
		genericArgumentValues.add(new ValueHolder(value));
	}
	
	public void addGenericArgumentValue(Object value, String type) {
		genericArgumentValues.add(new ValueHolder(value, type));
	}
	
	public ValueHolder getGenericArgumentValue(Class requiredType) {
		for (Iterator it = this.genericArgumentValues.iterator(); it.hasNext();) {
			ValueHolder valueHolder = (ValueHolder) it.next();
			Object value = valueHolder.getValue();
			if (valueHolder.getType() != null) {
				if (valueHolder.getType().equals(requiredType.getName())) {
					return valueHolder;
				}
			}
			else if (requiredType.isInstance(value) || (requiredType.isArray() && List.class.isInstance(value))) {
				return valueHolder;
			}
		}
		return null;
	}
	
	public Set getGenericArgumentValues() {
		return genericArgumentValues;
	}
	
	public ValueHolder getArgumentValue(int index, Class requiredType) {
		ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType);
		if (valueHolder == null) {
			valueHolder = getGenericArgumentValue(requiredType);
		}
		return valueHolder;
	}
	
	public int getArgumentCount() {
		return getNrOfArguments();
	}
	
	public int getNrOfArguments() {
		return indexedArgumentValues.size() + genericArgumentValues.size();
	}
	
	public boolean isEmpty() {
		return indexedArgumentValues.isEmpty() && genericArgumentValues.isEmpty();
	}
	
	public static class ValueHolder {
		private Object value = null;
		private String type = null;

		private ValueHolder(Object value) {
			this.value = value;
		}

		private ValueHolder(Object value, String type) {
			this.value = value;
			this.type = type;
		}
		
		public void setValue(Object value) {
			this.value = value;
		}
		
		public Object getValue() {
			return value;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public String getType() {
			return type;
		}
	}
}
