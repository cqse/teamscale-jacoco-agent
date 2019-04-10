/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright 2005-2011 The ConQAT Project                                   |
|                                                                          |
| Licensed under the Apache License, Version 2.0 (the "License");          |
| you may not use this file except in compliance with the License.         |
| You may obtain a copy of the License at                                  |
|                                                                          |
|    http://www.apache.org/licenses/LICENSE-2.0                            |
|                                                                          |
| Unless required by applicable law or agreed to in writing, software      |
| distributed under the License is distributed on an "AS IS" BASIS,        |
| WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. |
| See the License for the specific language governing permissions and      |
| limitations under the License.                                           |
+-------------------------------------------------------------------------*/
package com.teamscale.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A list for storing pairs in a specific order.
 */
public class PairList<S, T> implements Serializable, Iterable<Pair<S, T>> {

	/** Version used for serialization. */
	private static final long serialVersionUID = 1;

	/** The current size. */
	private int size = 0;

	/** The array used for storing the S. */
	private Object[] firstElements;

	/** The array used for storing the T. */
	private Object[] secondElements;

	/** Constructor. */
	public PairList() {
		this(16);
	}

	/** Constructor. */
	public PairList(int initialCapacity) {
		if (initialCapacity < 1) {
			initialCapacity = 1;
		}
		firstElements = new Object[initialCapacity];
		secondElements = new Object[initialCapacity];
	}

	/**
	 * Creates a new pair list initialized with a single key/value pair. This is especially helpful for construction of
	 * small pair lists, as type inference reduces writing overhead.
	 */
	public static <S, T> PairList<S, T> from(S key, T value) {
		PairList<S, T> result = new PairList<>();
		result.add(key, value);
		return result;
	}

	/** Returns whether the list is empty. */
	public boolean isEmpty() {
		return size == 0;
	}

	/** Returns the size of the list. */
	public int size() {
		return size;
	}

	/** Add the given pair to the list. */
	public void add(S first, T second) {
		ensureSpace(size + 1);
		firstElements[size] = first;
		secondElements[size] = second;
		++size;
	}

	/** Adds all pairs from another list. */
	public void addAll(PairList<S, T> other) {
		// we have to store this in a local var, as other.size may change if
		// other == this
		int otherSize = other.size;

		ensureSpace(size + otherSize);
		for (int i = 0; i < otherSize; ++i) {
			firstElements[size] = other.firstElements[i];
			secondElements[size] = other.secondElements[i];
			++size;
		}
	}

	/** Make sure there is space for at least the given amount of elements. */
	protected void ensureSpace(int space) {
		if (space <= firstElements.length) {
			return;
		}

		Object[] oldFirst = firstElements;
		Object[] oldSecond = secondElements;
		int newSize = firstElements.length * 2;
		while (newSize < space) {
			newSize *= 2;
		}

		firstElements = new Object[newSize];
		secondElements = new Object[newSize];
		System.arraycopy(oldFirst, 0, firstElements, 0, size);
		System.arraycopy(oldSecond, 0, secondElements, 0, size);
	}

	/** Returns the first element at given index. */
	@SuppressWarnings("unchecked")
	public S getFirst(int i) {
		checkWithinBounds(i);
		return (S) firstElements[i];
	}

	/**
	 * Checks whether the given <code>i</code> is within the bounds. Throws an exception otherwise.
	 */
	private void checkWithinBounds(int i) {
		if (i < 0 || i >= size) {
			throw new IndexOutOfBoundsException("Out of bounds: " + i);
		}
	}

	/** Sets the first element at given index. */
	public void setFirst(int i, S value) {
		checkWithinBounds(i);
		firstElements[i] = value;
	}

	/** Returns the second element at given index. */
	@SuppressWarnings("unchecked")
	public T getSecond(int i) {
		checkWithinBounds(i);
		return (T) secondElements[i];
	}

	/** Sets the first element at given index. */
	public void setSecond(int i, T value) {
		checkWithinBounds(i);
		secondElements[i] = value;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append('[');
		for (int i = 0; i < size; i++) {
			if (i != 0) {
				result.append(',');
			}
			result.append('(');
			result.append(firstElements[i]);
			result.append(',');
			result.append(secondElements[i]);
			result.append(')');
		}
		result.append(']');
		return result.toString();
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		int prime = 31;
		int hash = size;
		hash = prime * hash + HashCodeUtils.hashArrayPart(firstElements, 0, size);
		return prime * hash + HashCodeUtils.hashArrayPart(secondElements, 0, size);
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PairList)) {
			return false;
		}

		PairList<?, ?> other = (PairList<?, ?>) obj;
		if (size != other.size) {
			return false;
		}
		for (int i = 0; i < size; i++) {
			if (!Objects.equals(firstElements[i], other.firstElements[i])
					|| !Objects.equals(secondElements[i], secondElements[i])) {
				return false;
			}
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<Pair<S, T>> iterator() {
		return new Iterator<Pair<S, T>>() {
			int index = 0;

			/** {@inheritDoc} */
			@Override
			public boolean hasNext() {
				return index < size;
			}

			/** {@inheritDoc} */
			@Override
			public Pair<S, T> next() {
				checkWithinBounds(index);
				int oldIndex = index;
				index++;
				return createPairForIndex(oldIndex);
			}
		};
	}

	/**
	 * Creates a pair from the values at the given index in this list.
	 * <p>
	 * We suppress unchecked cast warnings since the PairList stores all elements as plain Objects.
	 */
	@SuppressWarnings("unchecked")
	private Pair<S, T> createPairForIndex(int index) {
		return new Pair<>((S) firstElements[index], (T) secondElements[index]);
	}

	/**
	 * Creates a {@link PairList} that consists of pairs of values taken from the two {@link List}s. I.e. entry i in the
	 * resulting {@link PairList} will consists of the pair (a, b) where a is the entry at index i in the first
	 * collection and b is the entry at index i of the second collection.
	 * <p>
	 * Both collections must be of the same size. The order of insertion into the new {@link PairList} is determined by
	 * the order imposed by the collections' iterators.
	 */
	public static <S, T> PairList<S, T> zip(List<S> firstValues, List<T> secondValues) {
		PairList<S, T> result = new PairList<>(firstValues.size());
		Iterator<S> firstIterator = firstValues.iterator();
		Iterator<T> secondIterator = secondValues.iterator();
		while (firstIterator.hasNext()) {
			result.add(firstIterator.next(), secondIterator.next());
		}
		return result;
	}

	/**
	 * For each element pair in this list, calls the given consumer with the first and second value from the pair as the
	 * only arguments.
	 */
	public void forEach(BiConsumer<S, T> consumer) {
		for (int i = 0; i < size; i++) {
			consumer.accept(getFirst(i), getSecond(i));
		}
	}

	/**
	 * Returns a new {@link PairList}, where both mappers are applied to the elements of the current {@link PairList}
	 * (input for each mapper is the pair of elements at each position).
	 */
	public <S2, T2> PairList<S2, T2> map(BiFunction<S, T, S2> firstMapper, BiFunction<S, T, T2> secondMapper) {
		PairList<S2, T2> result = new PairList<>(size);
		forEach((key, value) -> result.add(firstMapper.apply(key, value), secondMapper.apply(key, value)));
		return result;
	}

	/**
	 * Filters the pairlist by testing all items against the predicate and returning a pairlist of those for which it
	 * returns true. This method does not modify the original pairlist.
	 */
	public PairList<S, T> filter(BiFunction<S, T, Boolean> filterPredicate) {
		PairList<S, T> filteredList = new PairList<>(size);
		for (int i = 0; i < size; i++) {
			if (filterPredicate.apply(getFirst(i), getSecond(i))) {
				filteredList.add(getFirst(i), getSecond(i));
			}
		}
		return filteredList;
	}

	/**
	 * Returns a newly allocated list containing all of the elements in this PairList as a list of Pair<S,T>s.
	 */
	public List<Pair<S, T>> toList() {
		List<Pair<S, T>> result = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			result.add(new Pair<>((S) firstElements[i], (T) secondElements[i]));
		}
		return result;
	}

	/**
	 * Maps a pair list to another one using separate mappers for keys and values.
	 */
	public <S2, T2> PairList<S2, T2> map(Function<S, S2> keyMapper, Function<T, T2> valueMapper) {
		PairList<S2, T2> result = new PairList<>();
		forEach((key, value) -> result.add(keyMapper.apply(key), valueMapper.apply(value)));
		return result;
	}
}
