package twcore.core.lvz;



/**
    Lvz Display Modes

    @author D1st0rt
    @version 06.01.21
*/
public enum Mode
{
    ShowAlways,
    EnterZone,
    EnterArena,
    Kill,
    Death,
    ServerControlled;

    /**
        Gets a Mode object from an ordinal (index) value
        @param ordinal the ordinal of the desired mode
        @return the mode for that ordinal, or null
    */
    public Mode fromOrdinal(short ordinal)
    {
        Mode m = null;

        if(ordinal == 0)
            m = Mode.ShowAlways;
        else if(ordinal == 1)
            m = Mode.EnterZone;
        else if(ordinal == 2)
            m = Mode.EnterArena;
        else if(ordinal == 3)
            m = Mode.Kill;
        else if(ordinal == 4)
            m = Mode.Death;
        else if(ordinal == 5)
            m = Mode.ServerControlled;

        return m;
    }
};
