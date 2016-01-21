package twcore.core.net.iharder;

/**

    @author Robert.Harder
*/
public class PushbulletException extends Exception {

    public PushbulletException() {
        super();
    }

    public PushbulletException(Throwable ex) {
        super( ex );
    }

    public PushbulletException(String msg, Throwable ex) {
        super( msg, ex );
    }

    public PushbulletException(String msg) {
        super( msg );
    }


}
