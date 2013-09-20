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
	public static boolean isNullOrEmptyList(List aList){
		if (aList == null || aList.size() == 0){
			return true;
		}
		return false;
	}
}
