
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
* Convenience methods for altering special characters related to URLs,
* regular expressions, and HTML tags.
*/
public final class EscapeChars {

  /**
  * Synonym for <tt>URLEncoder.encode(String, "UTF-8")</tt>.
  *
  * <P>Used to ensure that HTTP query strings are in proper form, by escaping
  * special characters such as spaces.
  *
  * <P>An example use case for this method is a login scheme in which, after successful
  * login, the user is redirected to the "original" target destination. Such a target
  * might be passed around as a request parameter. Such a request parameter
  * will have a URL as its value, as in "LoginTarget=Blah.jsp?this=that&blah=boo", and
  * would need to be URL-encoded in order to escape its special characters.
  *
  * <P>It is important to note that if a query string appears in an <tt>HREF</tt>
  * attribute, then there are two issues - ensuring the query string is valid HTTP
  * (it is URL-encoded), and ensuring it is valid HTML (ensuring the ampersand is escaped).
  */
  public static String forURL(String aURLFragment){
    String result = null;
    try {
      result = URLEncoder.encode(aURLFragment, "UTF-8");
    }
    catch (UnsupportedEncodingException ex){
      throw new RuntimeException("UTF-8 not supported", ex);
    }
    return result;
  }


  /**
  * Replace characters having special meaning <em>inside</em> HTML tags
  * with their escaped equivalents, using character entities such as <tt>'&amp;'</tt>.
  *
  * <P>The escaped characters are :
  * <ul>
  * <li> <
  * <li> >
  * <li> "
  * <li> '
  * <li> \
  * <li> &
  * </ul>
  *
  * <P>This method ensures that arbitrary text appearing inside a tag does not "confuse"
  * the tag. For example, <tt>HREF='Blah.do?Page=1&Sort=ASC'</tt>
  * does not comply with strict HTML because of the ampersand, and should be changed to
  * <tt>HREF='Blah.do?Page=1&amp;Sort=ASC'</tt>. This is commonly seen in building
  * query strings. (In JSTL, the c:url tag performs this task automatically.)
  */
  public static String forHTMLTag(String aTagFragment){
    final StringBuffer result = new StringBuffer();

    final StringCharacterIterator iterator = new StringCharacterIterator(aTagFragment);
    char character =  iterator.current();
    while (character != CharacterIterator.DONE ){
      if (character == '<') {
        result.append("&lt;");
      }
      else if (character == '>') {
        result.append("&gt;");
      }
      else if (character == '\"') {
        result.append("&quot;");
      }
      else if (character == '\'') {
        result.append("&#039;");
      }
      else if (character == '\\') {
         result.append("&#092;");
      }
      else if (character == '&') {
         result.append("&amp;");
      }
      else {
        //the char is not a special one
        //add it to the result as is
        result.append(character);
      }
      character = iterator.next();
    }
    return result.toString();
  }

  /**
  * Return <tt>aText</tt> with all start-of-tag and end-of-tag characters
  * replaced by their escaped equivalents.
  *
  * <P>If user input may contain tags which must be disabled, then call
  * this method, not {@link #forHTMLTag}. This method is used for text appearing
  * <em>outside</em> of a tag, while {@link #forHTMLTag} is used for text appearing
  * <em>inside</em> an HTML tag.
  *
  * <P>It is not uncommon to see text on a web page presented erroneously, because
  * <em>all</em> special characters are escaped (as in {@link #forHTMLTag}), instead of 
  * just the start-of-tag and end-of-tag characters. In
  * particular, the ampersand character is often escaped not once but <em>twice</em> :
  * once when the original input occurs, and then a second time when the same item is
  * retrieved from the database. This occurs because the ampersand is the only escaped
  * character which appears in a character entity.
  */
  public static String toDisableTags(String aText){
    final StringBuffer result = new StringBuffer();
    final StringCharacterIterator iterator = new StringCharacterIterator(aText);
    char character =  iterator.current();
    while (character != CharacterIterator.DONE ){
      if (character == '<') {
        result.append("&lt;");
      }
      else if (character == '>') {
        result.append("&gt;");
      }
      else {
        //the char is not a special one
        //add it to the result as is
        result.append(character);
      }
      character = iterator.next();
    }
    return result.toString();
  }

  /**
  * Replace characters having special meaning in regular expressions
  * with their escaped equivalents.
  *
  * <P>The escaped characters include :
  *<ul>
  *<li>.
  *<li>\
  *<li>?, * , and +
  *<li>&
  *<li>:
  *<li>{ and }
  *<li>[ and ]
  *<li>( and )
  *<li>^ and $
  *</ul>
  *
  */
  public static String forRegex(String aRegexFragment){
    final StringBuffer result = new StringBuffer();

    final StringCharacterIterator iterator = new StringCharacterIterator(aRegexFragment);
    char character =  iterator.current();
    while (character != CharacterIterator.DONE ){
      /*
      * All literals need to have backslashes doubled.
      */
      if (character == '.') {
        result.append("\\.");
      }
      else if (character == '\\') {
        result.append("\\\\");
      }
      else if (character == '?') {
        result.append("\\?");
      }
      else if (character == '*') {
        result.append("\\*");
      }
      else if (character == '+') {
        result.append("\\+");
      }
      else if (character == '&') {
        result.append("\\&");
      }
      else if (character == ':') {
        result.append("\\:");
      }
      else if (character == '{') {
        result.append("\\{");
      }
      else if (character == '}') {
        result.append("\\}");
      }
      else if (character == '[') {
        result.append("\\[");
      }
      else if (character == ']') {
        result.append("\\]");
      }
      else if (character == '(') {
        result.append("\\(");
      }
      else if (character == ')') {
        result.append("\\)");
      }
      else if (character == '^') {
        result.append("\\^");
      }
      else if (character == '$') {
        result.append("\\$");
      }
      else {
        //the char is not a special one
        //add it to the result as is
        result.append(character);
      }
      character = iterator.next();
    }
    return result.toString();
  }
  
  // PRIVATE //
  
  private EscapeChars(){
    //empty - prevent construction
  }
}
