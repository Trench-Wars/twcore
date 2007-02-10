package twcore.core.util;

/*
 * SearchableStructure.java
 *
 * Created on January 19, 2002, 4:32 PM
 */

/**
 *
 * @author  harvey
 */
import java.util.*;
public class SearchableStructure {
    //Stores keywords mapped to SearchData objects
    HashMap data;
    /** Creates a new instance of SearchableStructure */
    public SearchableStructure() {
        data = new HashMap();
    }
    
    public void add( String result, String keywords ){
        SearchableStructure.SearchData value =
        new SearchableStructure.SearchData( keywords, result );
        
        ArrayList list = new ArrayList();
        list.add(value);
        
        for( Iterator i = value.getAllKeysIterator(); i.hasNext(); ){
            Object o = i.next();
            if( data.containsKey( o ))
                ((ArrayList)data.get(o)).add(value);
            else
                data.put( o, list );
            
        }
    }
    
    public String[] search( String keywordsAsString ){
        //Stages:
        //Stage 1: Grab the SearchData objects from the hashmap that correspond to the data
        //and add to a hashmap that maps them by match count
        HashMap mapDataToMatchCount = new HashMap();        
        LinkedList keywords = SearchableStructure.stringChopper( keywordsAsString, ' ' );
        for( Iterator i = keywords.iterator(); i.hasNext(); ){
            String keyword = (String)i.next();
            if( data.containsKey( keyword )){
                ArrayList list = (ArrayList)data.get( keyword );
                for( Iterator i2 = list.iterator(); i2.hasNext(); ){
                    SearchableStructure.SearchData listing =
                    (SearchableStructure.SearchData)i2.next();
                    if( listing.isValid( keywords )){
                        if( mapDataToMatchCount.containsKey( listing )){
                            Integer oldValue = (Integer)mapDataToMatchCount.get( listing );
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
        TreeMap orderedMap = new TreeMap();
        //Populate the orderedMap...
        for( Iterator i = mapDataToMatchCount.keySet().iterator(); i.hasNext(); ){
            SearchableStructure.SearchData dataObject = (SearchableStructure.SearchData)i.next();
            Integer hitCount = (Integer)mapDataToMatchCount.get( dataObject );
            if( orderedMap.containsKey( hitCount )){
                ((ArrayList)orderedMap.get( hitCount )).add( dataObject );
            }else{
                ArrayList list = new ArrayList();
                list.add( dataObject );
                orderedMap.put( hitCount, list );
            }
        }

        ArrayList endList = new ArrayList();
        for( int i = 0; i< orderedMap.size(); i++ ){
            ArrayList list = (ArrayList)orderedMap.remove( orderedMap.lastKey() );
            for( Iterator i2 = list.iterator(); i2.hasNext(); ){
                endList.add(((SearchableStructure.SearchData)i2.next()).getResult());
            }

        }
        return (String[])endList.toArray(new String[endList.size()]);
    }

    //Wrapper class for each value
    static class SearchData{
        LinkedList mayMatch;
        LinkedList mustMatch;
        LinkedList exclusions;
        LinkedList pointAdjusters;
        String entry;
        public SearchData( String keywords, String entry ){
            mayMatch = new LinkedList();
            mustMatch = new LinkedList();
            exclusions = new LinkedList();
            pointAdjusters = new LinkedList();
            this.entry = entry;
            LinkedList keywordList = SearchableStructure.stringChopper( keywords, ' ' );
            for( Iterator i = keywordList.iterator(); i.hasNext(); ){
                addKeyword( ((String)i.next()).trim() );
            }
        }
        
        public void addKeyword( String keyword ){
            if( keyword.charAt(0) == '&' )
                addMustMatch( keyword.substring( 1 ));
            else if( keyword.charAt( 0 ) == '-' )
                addExclusion( keyword.substring( 1 ));
            else if( keyword.charAt( 0 ) == '+' )
                addPointAdjuster( keyword.substring( 1 ));
            else
                mayMatch.add( keyword );
        }
        
        public Iterator getAllKeysIterator(){
            LinkedList returnList = new LinkedList( mayMatch );
            returnList.addAll( mustMatch );
            return returnList.iterator();
        }
                
        public void addMustMatch( String keyword ){
            mustMatch.add( keyword );
        }
        public void addExclusion( String keyword ){
            exclusions.add( keyword );
        }
        public void addPointAdjuster( String keyword ){
            pointAdjusters.add( keyword );
        }
        
        public Iterator getMustMatchIterator(){
            return mustMatch.iterator();
        }
        public Iterator getMayMatchIterator(){
            return mayMatch.iterator();
        }
        public Iterator getExclusionsIterator(){
            return exclusions.iterator();
        }
        public Iterator getPointAdjusterIterator(){
            return pointAdjusters.iterator();
        }
        
        public boolean matchesAllMustMatch( LinkedList keywords ){
            if( keywords.containsAll( mustMatch ))
                return true;
            else return false;
        }

        public int getPointAdjusterValue( LinkedList keywords ){
            int returnValue=0;
            for( Iterator i = getPointAdjusterIterator(); i.hasNext(); ){
                if( keywords.contains( i.next() )) returnValue++;
            }
            return returnValue;
        }
        
        //Checks if the given keywords contain any of the 
        public boolean containsNoExcluded( LinkedList keywords ){
            for( Iterator i = getExclusionsIterator(); i.hasNext(); ){
                if(keywords.contains( i.next() )) return false;
            }
            return true;
        }
        
        public boolean isValid( LinkedList keywords ){
            return matchesAllMustMatch( keywords ) && containsNoExcluded( keywords );
        }
        
        public int mustMatchSize(){
            return mustMatch.size();
        }

        public String getResult(){
            return toString();
        }
        
        public String toString(){
            return entry;
        }
    }
    
    public static LinkedList stringChopper( String input, char deliniator ){
        LinkedList list = new LinkedList();
        
        int nextSpace = 0;
        int previousSpace = 0;
        
        do{
            previousSpace = nextSpace;
            nextSpace = input.indexOf( deliniator, nextSpace + 1 );
            
            if ( nextSpace!= -1 ){
                String stuff = input.substring( previousSpace, nextSpace ).trim().toLowerCase();
                stuff=stuff.replace('?',' ').replace('!',' ');
                if( stuff!=null && !stuff.equals("") )
                    list.add( stuff );
            }
            
        }while( nextSpace != -1 );
        String stuff = input.substring( previousSpace ).toLowerCase();
        stuff=stuff.replace('?',' ').replace('!',' ').trim();
        list.add( stuff );
        
        return list;
    }
    
}

