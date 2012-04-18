/*
 *  Copyright 2012 Phuong LeCong
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.plecong.hogan.utils

abstract class ObjectUtils {

	private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

	static boolean hasProperty(obj, prop) {
		if (obj instanceof Map) {
			obj.containsKey(prop)
		} else {
			obj.getProperties().containsKey(prop)
		}
	}

	static boolean isArray(obj) {
		obj instanceof Iterable || obj.getClass().isArray()
	}

	static boolean isPrimitive(obj) {
		WRAPPER_TYPES.contains(obj.class)
	}

	private static Set<Class<?>> getWrapperTypes()
	{
		Set<Class<?>> ret = new HashSet<Class<?>>();
		ret.add(Boolean.class);
		ret.add(Character.class);
		ret.add(Byte.class);
		ret.add(Short.class);
		ret.add(Integer.class);
		ret.add(Long.class);
		ret.add(Float.class);
		ret.add(Double.class);
		ret.add(Void.class);
		return ret;
	}

}