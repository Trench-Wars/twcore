package twcore.bots.zbot;

import java.util.Vector;

/**
 * <p>Title: </p>StringTools
 * <p>Description: </p>This class provides tools for manipulation Strings.
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>SSCU Trench Wars
 * @author Cpt.Guano!
 * @version 1.0
 */

public class StringTools
{
  public static final int SPACE_CHAR = ' ';

  private StringTools()
  {
  }

  /**
   * This method wraps a string so that it will fit under a certain length.  The
   * String will be returned as a String array, with each array member being
   * one line of the wrapped text.  The String will only be cut at word
   * boundaries.  The lineLength must be greater than 0 or else an exception
   * will be thrown.
   *
   * @param string is the String to wrap.
   * @param lineLength is the maximum length of each line after wrapping.
   * @return an array of Strings representing the wrapped String will be
   * returned.
   */

  public static String[] wrapString(String string, int lineLength)
  {
    Vector<String> result = new Vector<String>();
    int beginIndex = 0;
    int endIndex = 0;

    if(lineLength <= 0)
      throw new IllegalArgumentException("ERROR: Line Length must be greater than 0.");

    do
    {
      beginIndex = indexNotOf(string, SPACE_CHAR, endIndex);
      endIndex = getWrapIndex(string, lineLength, beginIndex);
      result.add(string.substring(beginIndex, endIndex));
    } while(endIndex != string.length());
    return result.toArray(new String[result.size()]);
  }

  /**
   * This static method creates a string from one character repeated for a
   * certain length.
   *
   * @param length is the length of the resultant string.
   * @param c is the character to repeat.
   * @return the string of length length and with charcter c is returned.
   */

  public static String repeatChar(int length, char c)
  {
    StringBuffer string = new StringBuffer();

    for(int counter = 0; counter < length; counter++)
      string.append(c);
    return string.toString();
  }

  /**
   * This method takes an integer i and a String string and concats the two with
   * a space between them.  If i is plural then an s is appended onto the end of
   * string.  The resulting string is returned.
   *
   * @param i is the integer.
   * @param string is the string.
   * @return a string of the form i string(s) is returned.  The (s) will be
   * present if i is plural.
   */
  public static String pluralize(int i, String string)
  {
    if(i != 1)
      return i + " " + string + "s";
    return i + " " + string;
  }

  /**
   * This method takes a number and turns it in to a count by appending st, nd,
   * rd, or th on the end of it.
   *
   * @param i is the integer to turn into a count string.
   * @return the count string is returned.
   */
  public static String getCountString(int i)
  {
    int lastTwoDigits = i % 100;
    int lastDigit = i % 10;

    if(lastTwoDigits > 10 && lastTwoDigits < 20)
      return i + "th";
    switch(lastDigit)
    {
      case 1: return i + "st";
      case 2: return i + "nd";
      case 3: return i + "rd";
      default: return i + "th";
    }
  }

  /**
   * This method finds and returns the first index of target in s.  In this
   * check, case is ignored.  If target is not found then -1 is returned.
   *
   * @param s is the string to search through.
   * @param target is the string to look for.
   * @return the index of target in s is returned.  If target is not in s then
   * -1 is returned.
   */
  public static int indexOfIgnoreCase(String s, String target)
  {
    boolean isFound;

    for(int index = 0; index < s.length() - target.length(); index++)
    {
      isFound = true;
      for(int subIndex = 0; subIndex < target.length(); subIndex++)
      {
        if(Character.toLowerCase(s.charAt(index + subIndex)) !=
           Character.toLowerCase(s.charAt(subIndex)))
        {
          isFound = false;
          break;
        }
      }
      if(isFound == true)
        return index;
    }
    return -1;
  }

  /**
   * This method checks to see if String s starts with String target.  When
   * performing the check, case is ignored.
   *
   * @param s is the string to check.
   * @param target is the target string to look for.
   * @return true is returned if s starts with target, not taking the case of
   * the characters into account.
   */
  public static boolean startsWithIgnoreCase(String s, String target)
  {
    int targetLength = target.length();
    char sChar;
    char targetChar;

    if(targetLength > s.length())
      return false;
    for(int index = 0; index < targetLength; index++)
    {
      sChar = Character.toLowerCase(s.charAt(index));
      targetChar = Character.toLowerCase(target.charAt(index));
      if(sChar != targetChar)
        return false;
    }
    return true;
  }

  /**
   * This method gets the index to chop a string so that it fits under a
   * specfied length.  This method will only cut the string at word boundaries
   * unless one word is longer than the lineLength.  The line is said to begin
   * at a certain part of the string and ends at a distance lineLength later.
   *
   * @param string is the String to wrap
   * @param lineLength is the length to chop the line at.
   * @param beginIndex is the index to begin the line at.
   * @return the index of the where to chop the string at is returned.
   */

  private static int getWrapIndex(String string, int lineLength, int beginIndex)
  {
    int lineEnd = beginIndex + lineLength;
    int returnIndex;

    if(string.length() <= lineEnd)
      return string.length();
    returnIndex = string.lastIndexOf(SPACE_CHAR, lineEnd);
    if(returnIndex == -1)
      return lineEnd;
    return returnIndex;
  }

  public static int indexNotOf(String string, int target, int beginIndex)
  {
    char character;

    for(int index = beginIndex; index < string.length(); index++)
    {
      character = string.charAt(index);
      if((char) target != character)
        return index;
    }
    return -1;
  }
}