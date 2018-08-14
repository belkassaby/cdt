package org.eclipse.cdt.internal.ui.refactoring.rename.resources;

public class StringUtils {

	private static final int STATE_LOWER = 0;
	private static final int STATE_UPPER = 1;
	private static final int STATE_NUMBER = 2;

	public static String asStyleLowercaseUnderscores(String string) {
		int len = string.length();
		FastStringBuffer buf = new FastStringBuffer(len * 2);

		int lastState = 0;
		for (int i = 0; i < len; i++) {
			char c = string.charAt(i);
			if (Character.isUpperCase(c)) {
				if (lastState != STATE_UPPER) {
					if (buf.length() > 0 && buf.lastChar() != '_') {
						buf.append('_');
					}
				}
				buf.append(Character.toLowerCase(c));
				lastState = STATE_UPPER;

			} else if (Character.isDigit(c)) {
				if (lastState != STATE_NUMBER) {
					if (buf.length() > 0 && buf.lastChar() != '_') {
						buf.append('_');
					}
				}

				buf.append(c);
				lastState = STATE_NUMBER;
			} else {
				buf.append(c);
				lastState = STATE_LOWER;
			}
		}
		return buf.toString();
	}

	public static boolean isAllUpper(String string) {
		int len = string.length();
		for (int i = 0; i < len; i++) {
			char c = string.charAt(i);
			if (Character.isLetter(c) && !Character.isUpperCase(c)) {
				return false;
			}
		}
		return true;
	}

	public static String asStyleCamelCaseFirstLower(String string) {
		if (isAllUpper(string)) {
			string = string.toLowerCase();
		}

		int len = string.length();
		FastStringBuffer buf = new FastStringBuffer(len);
		boolean first = true;
		int nextUpper = 0;

		for (int i = 0; i < len; i++) {
			char c = string.charAt(i);
			if (first) {
				if (c == '_') {
					// underscores at the start
					buf.append(c);
					continue;
				}
				buf.append(Character.toLowerCase(c));
				first = false;
			} else {

				if (c == '_') {
					nextUpper += 1;
					continue;
				}
				if (nextUpper > 0) {
					c = Character.toUpperCase(c);
					nextUpper = 0;
				}

				buf.append(c);
			}
		}

		if (nextUpper > 0) {
			// underscores at the end
			buf.appendN('_', nextUpper);
		}
		return buf.toString();
	}

	public static String asStyleCamelCaseFirstUpper(String string) {
		string = asStyleCamelCaseFirstLower(string);
		if (string.length() > 0) {
			return Character.toUpperCase(string.charAt(0)) + string.substring(1);
		}
		return string;
	}

	/**
	 * Tests whether each character in the given string is a valid identifier.
	 *
	 * @param str
	 * @return <code>true</code> if the given string is a word
	 */
	public static boolean isValidIdentifier(final String str, boolean acceptPoint) {
		if (str == null)
			return false;
		int len = str.length();
		if (len == 0)
			return false;

		char c = '\0';
		boolean lastWasPoint = false;
		for (int i = 0; i < len; i++) {
			c = str.charAt(i);
			if (i == 0) {
				if (!Character.isJavaIdentifierStart(c)) {
					return false;
				}
			} else {
				if (!Character.isJavaIdentifierPart(c)) {
					if (acceptPoint && c == '.') {
						if (lastWasPoint) {
							return false; // can't have 2 consecutive dots.
						}
						lastWasPoint = true;
						continue;
					}
					return false;
				}
			}
			lastWasPoint = false;

		}
		if (c == '.') {
			// if the last char is a point, don't accept it (i.e.: only accept
			// at middle).
			return false;
		}
		return true;
	}
}
