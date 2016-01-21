package twcore.core.command;

/**
    This interface provides a callback for whenever a setting is modified
    through the command interface (!set ...)

    @author D1st0rt
    @version 06.02.08
*/
public interface TSChangeListener
{
    /**
        Notification that a setting has changed
        @param name the name of the setting
        @param value the new value of the setting
    */
    public void settingChanged(String name, Object value);
}
