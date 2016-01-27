package twcore.core.util;

/*
    StringBag.java

    Created on January 11, 2002, 11:37 AM
*/
import java.util.ArrayList;
public class StringBag {
    ArrayList<String> list;
    /** Creates a new instance of StringBag */
    public StringBag() {
        list = new ArrayList<String>();
    }

    public StringBag( String string ) {
        this();
        add( string );
    }

    public void clear() {
        list.clear();
    }

    public void add( String string ) {
        list.add( string );
    }

    public ArrayList<String> getList() {
        return list;
    }

    public String grab() {
        if( isEmpty() ) {
            return null;
        } else {
            return list.get( random( list.size() ));
        }
    }

    public String grabAndRemove() {
        if( isEmpty() ) {
            return null;
        } else {
            int i = random( list.size() );
            String grabbed;

            grabbed = list.get( i ) ;
            list.remove( i );
            return grabbed;
        }
    }

    public int size() {
        return list.size();
    }

    private int random( int maximum ) {
        return (int)(Math.random() * maximum);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public String toString() {
        return grab();
    }

}
