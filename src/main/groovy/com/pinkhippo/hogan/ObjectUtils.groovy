package com.pinkhippo.hogan

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