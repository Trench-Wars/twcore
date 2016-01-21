package twcore.core.lvz;


/**
    Lvz Display Layers

    @author D1st0rt
    @version 06.01.21
*/
public enum Layer
{
    BelowAll,
    AfterBackground,
    AfterTiles,
    AfterWeapons,
    AfterShips,
    AfterGauges,
    AfterChat,
    TopMost;

    /**
        Gets a Layer object from an ordinal (index) value
        @param ordinal the ordinal of the desired layer
        @return the layer for that ordinal, or null
    */
    public Layer fromOrdinal(byte ordinal)
    {
        Layer l = null;

        if(ordinal == 0)
            l = BelowAll;
        else if(ordinal == 1)
            l = AfterBackground;
        else if(ordinal == 2)
            l = AfterTiles;
        else if(ordinal == 3)
            l = AfterWeapons;
        else if(ordinal == 4)
            l = AfterShips;
        else if(ordinal == 5)
            l = AfterGauges;
        else if(ordinal == 6)
            l = AfterChat;
        else if(ordinal == 7)
            l = TopMost;

        return l;
    }
};

