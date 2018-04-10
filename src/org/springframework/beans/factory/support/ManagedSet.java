
package org.springframework.beans.factory.support;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.springframework.core.CollectionFactory;

public class ManagedSet extends HashSet {

	private final Set targetSet;

	public ManagedSet() {
		this(16);
	}

	public ManagedSet(int initialCapacity) {
		this.targetSet = CollectionFactory.createLinkedSetIfPossible(initialCapacity);
	}

	public ManagedSet(Set targetSet) {
		this.targetSet = targetSet;
	}

	public int size() {
		return this.targetSet.size();
	}

	public boolean isEmpty() {
		return this.targetSet.isEmpty();
	}

	public boolean contains(Object obj) {
		return this.targetSet.contains(obj);
	}

	public Iterator iterator() {
		return this.targetSet.iterator();
	}

	public Object[] toArray() {
		return this.targetSet.toArray();
	}

	public Object[] toArray(Object[] arr) {
		return this.targetSet.toArray(arr);
	}

	public boolean add(Object obj) {
		return this.targetSet.add(obj);
	}

	public boolean remove(Object obj) {
		return this.targetSet.remove(obj);
	}

	public boolean containsAll(Collection coll) {
		return this.targetSet.containsAll(coll);
	}

	public boolean addAll(Collection coll) {
		return this.targetSet.addAll(coll);
	}

	public boolean retainAll(Collection coll) {
		return this.targetSet.retainAll(coll);
	}

	public boolean removeAll(Collection coll) {
		return this.targetSet.removeAll(coll);
	}

	public void clear() {
		this.targetSet.clear();
	}

	public int hashCode() {
		return this.targetSet.hashCode();
	}

	public boolean equals(Object obj) {
		return this.targetSet.equals(obj);
	}

	public String toString() {
		return this.targetSet.toString();
	}
}
