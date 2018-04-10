
package org.springframework.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.springframework.util.StringUtils;

public class MutablePropertyValues implements PropertyValues, Serializable {

	private final ArrayList propertyValueList;
	
	public MutablePropertyValues() {
		this.propertyValueList = new ArrayList();
	}
	
	public MutablePropertyValues(PropertyValues source) {
		// We can optimize this because it's all new: There is no replacement of existing property values.
		if (source != null) {
			PropertyValue[] pvs = source.getPropertyValues();
			propertyValueList = new ArrayList(pvs.length);
			for (int i = 0; i < pvs.length; i++) {
				PropertyValue newPv = new PropertyValue(pvs[i].getName(), pvs[i].getValue());
				propertyValueList.add(newPv);
			}
		} else {
			propertyValueList = new ArrayList(0);
		}
	}

	public MutablePropertyValues(Map source) {
		// We can optimize this because it's all new: There is no replacement of existing property values.
		if (source != null) {
			propertyValueList = new ArrayList(source.size());
			Iterator it = source.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry)it.next();
				PropertyValue newPv = new PropertyValue((String)entry.getKey(), entry.getValue());
				propertyValueList.add(newPv);
			}
		} else {
			propertyValueList = new ArrayList(0);
		}
	}
	
	public MutablePropertyValues addPropertyValues(PropertyValues source) {
		if (source != null) {
			PropertyValue[] pvs = source.getPropertyValues();
			for (int i = 0; i < pvs.length; i++) {
				PropertyValue newPv = new PropertyValue(pvs[i].getName(), pvs[i].getValue());
				addPropertyValue(newPv);
			}
		}
		return this;
	}
	
	public MutablePropertyValues addPropertyValues(Map source) {
		if (source != null) {
			Iterator it = source.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry)it.next();
				PropertyValue newPv = new PropertyValue((String) entry.getKey(), entry.getValue());
				addPropertyValue(newPv);
			}
		}
		return this;
	}
	
	public MutablePropertyValues addPropertyValue(PropertyValue pv) {
		for (int i = 0; i < propertyValueList.size(); i++) {
			PropertyValue currentPv = (PropertyValue)propertyValueList.get(i);
			if (currentPv.getName().equals(pv.getName())) {
				setPropertyValueAt(pv, i);
				return this;
			}
		}
		propertyValueList.add(pv);
		return this;
	}
	
	public void addPropertyValue(String propertyName, Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
	}

	/**
	 * Remove the given PropertyValue, if contained.
	 * @param pv the PropertyValue to remove
	 */
	public void removePropertyValue(PropertyValue pv) {
		this.propertyValueList.remove(pv);
	}
	
	public void removePropertyValue(String propertyName) {
		removePropertyValue(getPropertyValue(propertyName));
	}
	
	public void setPropertyValueAt(PropertyValue pv, int i) {
		propertyValueList.set(i, pv);
	}

	public PropertyValue[] getPropertyValues() {
		return (PropertyValue[])propertyValueList.toArray(new PropertyValue[propertyValueList.size()]);
	}

	public PropertyValue getPropertyValue(String propertyName) {
		for (int i = 0; i < this.propertyValueList.size(); i++) {
			PropertyValue pv = (PropertyValue) propertyValueList.get(i);
			if (pv.getName().equals(propertyName)) {
				return pv;
			}
		}
		return null;
	}

	public boolean contains(String propertyName) {
		return (getPropertyValue(propertyName) != null);
	}

	public PropertyValues changesSince(PropertyValues old) {
		MutablePropertyValues changes = new MutablePropertyValues();
		if (old == this) {
			return changes;
		}
		// for each property value in the new set
		for (Iterator it = this.propertyValueList.iterator(); it.hasNext(); ) {
			PropertyValue newPv = (PropertyValue) it.next();
			// if there wasn't an old one, add it
			PropertyValue pvOld = old.getPropertyValue(newPv.getName());
			if (pvOld == null) {
				changes.addPropertyValue(newPv);
			} else if (!pvOld.equals(newPv)) {
				// it's changed
				changes.addPropertyValue(newPv);
			}
		}
		return changes;
	}

	public String toString() {
		PropertyValue[] pvs = getPropertyValues();
		StringBuffer sb = new StringBuffer("PropertyValues: length=" + pvs.length + "; ");
		sb.append(StringUtils.arrayToDelimitedString(pvs, "; "));
		return sb.toString();
	}
}
