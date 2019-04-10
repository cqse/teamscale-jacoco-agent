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

/**
 * Simple pair class.
 * 
 * @author hummelb
 */
public class Pair<S, T> extends ImmutablePair<S, T> {

	/** Version used for serialization. */
	private static final long serialVersionUID = 1;

	/** Constructor. */
	public Pair(S first, T second) {
		super(first, second);
	}

	/** Copy constructor. */
	public Pair(ImmutablePair<S, T> p) {
		super(p);
	}

	/** Set the first value. */
	public void setFirst(S first) {
		this.first = first;
	}

	/** Set the second value. */
	public void setSecond(T second) {
		this.second = second;
	}

	/** {@inheritDoc} */
	@Override
	protected Pair<S, T> clone() {
		return new Pair<S, T>(this);
	}


	/** Factory method for pairs, to simplify creation. */
	public static <S, T> Pair<S, T> createPair(S first, T second) {
		return new Pair<>(first, second);
	}
}