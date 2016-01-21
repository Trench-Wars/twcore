package twcore.core.util;

/*
    SearchableStructure.java

    Created on January 19, 2002, 4:32 PM
*/

/**

    @author  harvey
*/
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

public class SearchableStructure {
    //Stores keywords mapped to SearchData objects
    HashMap<Object, ArrayList<SearchData>> data;
    /** Creates a new instance of SearchableStructure */
    public SearchableStructure() {
        data = new HashMap<Object, ArrayList<SearchData>>();
    }

    public void add( String result, String keywords ) {
        SearchData value =
            new SearchData( keywords, result );

        ArrayList<SearchData> list = new ArrayList<SearchData>();
        list.add(value);

        for( Iterator<String> i = value.getAllKeysIterator(); i.hasNext(); ) {
            String o = i.next();

            if( data.containsKey( o ))
                data.get(o).add(value);
            else
                data.put( o, list );

        }
    }

    public String[] search( String keywordsAsString ) {
        //Stages:
        //Stage 1: Grab the SearchData objects from the hashmap that correspond to the data
        //and add to a hashmap that maps them by match count
        HashMap<SearchData, Integer> mapDataToMatchCount = new HashMap<SearchData, Integer>();
        LinkedList<String> keywords = SearchableStructure.stringChopper( keywordsAsString, ' ' );

        for( Iterator<String> i = keywords.iterator(); i.hasNext(); ) {
            String keyword = i.next();

            if( data.containsKey( keyword )) {
                ArrayList<SearchData> list = data.get( keyword );

                for( Iterator<SearchData> i2 = list.iterator(); i2.hasNext(); ) {
                    SearchData listing = i2.next();

                    if( listing.isValid( keywords )) {
                        if( mapDataToMatchCount.containsKey( listing )) {
                            Integer oldValue = mapDataToMatchCount.get( listing );
                            Integer newValue = new Integer( oldValue.intValue() + 1 );
                            mapDataToMatchCount.put( listing, newValue );
                        } else {
                            int modifier = listing.getPointAdjusterValue( keywords );

                            if( modifier != 0 )
                                mapDataToMatchCount.put( listing, new Integer( modifier + 1 ));
                            else mapDataToMatchCount.put( listing, new Integer( 1 ));
                        }
                    }
                }
            }
        }

        //Keys: Integers representing number of matches
        //Values: Data objects
        TreeMap<Integer, ArrayList<SearchData>> orderedMap = new TreeMap<Integer, ArrayList<SearchData>>();

        //Populate the orderedMap...
        for( Iterator<SearchData> i = mapDataToMatchCount.keySet().iterator(); i.hasNext(); ) {
            SearchData dataObject = i.next();
            Integer hitCount = mapDataToMatchCount.get( dataObject );

            if( orderedMap.containsKey( hitCount )) {
                orderedMap.get( hitCount ).add( dataObject );
            } else {
                ArrayList<SearchData> list = new ArrayList<SearchData>();
                list.add( dataObject );
                orderedMap.put( hitCount, list );
            }
        }

        ArrayList<String> endList = new ArrayList<String>();

        for( int i = 0; i < orderedMap.size(); i++ ) {
            ArrayList<SearchData> list = orderedMap.remove( orderedMap.lastKey() );

            for( Iterator<SearchData> i2 = list.iterator(); i2.hasNext(); ) {
                endList.add((i2.next()).getResult());
            }

        }

        return endList.toArray(new String[endList.size()]);
    }

    //Wrapper class for each value
    static class SearchData {
        LinkedList<String> mayMatch;
        LinkedList<String> mustMatch;
        LinkedList<String> exclusions;
        LinkedList<String> pointAdjusters;
        String entry;
        public SearchData( String keywords, String entry ) {
            mayMatch = new LinkedList<String>();
            mustMatch = new LinkedList<String>();
            exclusions = new LinkedList<String>();
            pointAdjusters = new LinkedList<String>();
            this.entry = entry;
            LinkedList<String> keywordList = SearchableStructure.stringChopper( keywords, ' ' );

            for(String keyword : keywordList) {
                addKeyword(keyword);
            }
        }

        public void addKeyword( String keyword ) {
            if( keyword.charAt(0) == '&' )
                addMustMatch( keyword.substring( 1 ));
            else if( keyword.charAt( 0 ) == '-' )
                addExclusion( keyword.substring( 1 ));
            else if( keyword.charAt( 0 ) == '+' )
                addPointAdjuster( keyword.substring( 1 ));
            else
                mayMatch.add( keyword );
        }

        public Iterator<String> getAllKeysIterator() {
            LinkedList<String> returnList = new LinkedList<String>( mayMatch );
            returnList.addAll( mustMatch );
            return returnList.iterator();
        }

        public void addMustMatch( String keyword ) {
            mustMatch.add( keyword );
        }
        public void addExclusion( String keyword ) {
            exclusions.add( keyword );
        }
        public void addPointAdjuster( String keyword ) {
            pointAdjusters.add( keyword );
        }

        public Iterator<String> getMustMatchIterator() {
            return mustMatch.iterator();
        }
        public Iterator<String> getMayMatchIterator() {
            return mayMatch.iterator();
        }
        public Iterator<String> getExclusionsIterator() {
            return exclusions.iterator();
        }
        public Iterator<String> getPointAdjusterIterator() {
            return pointAdjusters.iterator();
        }

        public boolean matchesAllMustMatch( LinkedList<String> keywords ) {
            if( keywords.containsAll( mustMatch ))
                return true;
            else return false;
        }

        public int getPointAdjusterValue( LinkedList<String> keywords ) {
            int returnValue = 0;

            for( Iterator<String> i = getPointAdjusterIterator(); i.hasNext(); ) {
                if( keywords.contains( i.next() )) returnValue++;
            }

            return returnValue;
        }

        //Checks if the given keywords contain any of the
        public boolean containsNoExcluded( LinkedList<String> keywords ) {
            for( Iterator<String> i = getExclusionsIterator(); i.hasNext(); ) {
                if(keywords.contains( i.next() )) return false;
            }

            return true;
        }

        public boolean isValid( LinkedList<String> keywords ) {
            return matchesAllMustMatch( keywords ) && containsNoExcluded( keywords );
        }

        public int mustMatchSize() {
            return mustMatch.size();
        }

        public String getResult() {
            return toString();
        }

        public String toString() {
            return entry;
        }
    }

    public static LinkedList<String> stringChopper( String input, char deliniator ) {
        LinkedList<String> list = new LinkedList<String>();

        int nextSpace = 0;
        int previousSpace = 0;

        do {
            previousSpace = nextSpace;
            nextSpace = input.indexOf( deliniator, nextSpace + 1 );

            if ( nextSpace != -1 ) {
                String stuff = input.substring( previousSpace, nextSpace ).trim().toLowerCase();
                stuff = stuff.replace('?', ' ').replace('!', ' ');

                if( stuff != null && !stuff.equals("") )
                    list.add( stuff );
            }

        } while( nextSpace != -1 );

        String stuff = input.substring( previousSpace ).toLowerCase();
        stuff = stuff.replace('?', ' ').replace('!', ' ').trim();
        list.add( stuff );

        return list;
    }

}

