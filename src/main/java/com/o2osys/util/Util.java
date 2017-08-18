package com.o2osys.util;

import java.lang.reflect.Field;
import java.util.HashMap;

public class Util {
	
	/**
	 * 오브젝트 => 맵 변환작업
	 * @param obj
	 * @return
	 */
	public static HashMap<String, Object> ConverObjectToMap(Object obj) {
		try {
			//Field[] fields = obj.getClass().getFields();	//private field는 나오지 않음.
			Field[] fields = obj.getClass().getDeclaredFields();
			HashMap<String, Object> resultMap = new HashMap<String, Object>();
			for (int i = 0; i <= fields.length - 1; i++) {
				fields[i].setAccessible(true);
				resultMap.put(fields[i].getName(), fields[i].get(obj));
			}
			return resultMap;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean toBoolean(String msg) {
		if (isNull(msg)) return false;
		
		msg = msg.toLowerCase();
		
		if (msg.equals("true")) return true;
		if (msg.equals("on")) return true;
		if (msg.equals("y")) return true;
		if (msg.equals("yes")) return true;
		
		return false;
	}
	
	public static int toInteger(String num) {
		if (isNull(num)) return 0;
		
		try {
			return Integer.parseInt(num);
		} catch(Exception e) {
			int pos = num.indexOf(".");
			if (pos > 0) {
				return toInteger(num.substring(0, pos));
			}
			return 0;
		}
	}
	
	public static double toDouble(String num) {
		if (isNull(num)) return 0.0;
		
		try {
			return Double.parseDouble(num);
		} catch(Exception e) {
			return 0.0;
		}
	}
	
	public static long toLong(String num) {
		if (isNull(num)) return 0;
		
		try {
			return Long.parseLong(num);
		} catch(Exception e) {
			int pos = num.indexOf(".");
			if (pos > 0) {
				return toLong(num.substring(0, pos));
			}
			return 0;
		}
	}
	
	public static boolean isNull(String msg) {
		if (msg == null || msg.equals("") || msg.equals("null"))
			return true;
		return false;
	}
	
	public static String getNullReplace(String msg, String replacement) {
		if (msg == null || msg.equals("") || msg.equals("null"))
			return replacement;
		return msg;
	}
	
	public static String replaceAll(String msg, String regex, String replacement) {
		if (isNull(msg)) return "";
		
		if (isNull(regex) || (replacement == null)) return msg;
		
		int pos = msg.indexOf(regex);
		while(pos != -1) {
			msg = msg.substring(0, pos) + replacement + msg.substring(pos + regex.length(), msg.length());
			pos = msg.indexOf(regex);
		}
		
		return msg;
	}
	
	public static String LPad(String Msg, int nLen, String reMsg) {
		
		if (isNull(Msg))
			return "";
		
		if (isNull(reMsg))
			reMsg = " ";
		
		String sResult = Msg;
		int len = sResult.length();
		
		for (int i = len; i < nLen; i++) {
			sResult = reMsg + sResult;
        }
		return sResult;
	}
	
	public static String RPad(String Msg, int nLen, String reMsg) {

		if (isNull(Msg))
			return "";
		
		if (isNull(reMsg))
			reMsg = " ";
		
		String sResult = Msg;
		int len = sResult.length();
		
		for (int i = len; i < nLen; i++) {
			sResult = sResult + reMsg;
        }
		return sResult;
	}
}
