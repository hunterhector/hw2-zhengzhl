/**
 * 
 */
package edu.cmu.deiis.util;

import java.util.List;

/**
 * @author Hector
 * 
 */
public class ListUtils {
	public static <E extends Object> boolean isNullOrEmptyList(List<E> aList) {
		if (aList == null || aList.size() == 0) {
			return true;
		}
		return false;
	}
}
