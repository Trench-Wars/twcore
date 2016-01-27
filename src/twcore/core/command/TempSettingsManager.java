package twcore.core.command;

import static twcore.core.OperatorList.ER_LEVEL;
import static twcore.core.OperatorList.OWNER_LEVEL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twcore.core.BotAction;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
    This class provides an easy way to allow an op to modify temporary settings.
    The TempSettingsManager (TSM) interfaces with the bot and keeps track of all the
    TempSettings. Settings are identified by name and are no longer case-sensitive.
    There are now multiple ways to interface chat messages with the TSM:
    <ul>
       <li> Register !get and !set with a supplied CommandInterpreter
       <li> Pass the chat message to it with handleEvent
       <li> Filter the chat messages yourself and call c_Set and c_Get directly
    </ul>
    As of this version, the types of settings you can store are integers, doubles,
    booleans, strings, players, and enumerated types in the form of a list of
    strings. On the command side, you can assign values using the form
    <code>!set name1=value1 name2=value2 ...</code> For strings that contain spaces
    or non-alphanumeric characters, you can enclose the value in quotes. If at
    certain points during the execution of your bot you do not want settings to
    be changed from the command interface, you can use the locking system to either
    lock settings individually or you can lock the entire TSM at once. If your
    bot needs to be notified when a bot operator modifies one of the settings
    through the command interface, have it implement TSChangeListener and subscribe
    with the TSM to get the settingChanged callback.

    @author D1st0rt
    @version 07.08.08
*/
public class TempSettingsManager
{
    private BotAction m_botAction;
    private int m_opLevel = ER_LEVEL;
    private HashMap<String, TempSetting> m_settings;
    private boolean m_locked;
    private Vector<TSChangeListener> m_listeners;
    private String[] customHelp;
    private Pattern pattern;

    /**
        Creates a new instance of TempSettingsManager.
        @param botAction The BotAction event to send chat messages with
        @param cmd The CommandInterpreter to register commands with
        @param opLevel The minimum Operator Level to access the settings
    */
    public TempSettingsManager(BotAction botAction, CommandInterpreter cmd, int opLevel)
    {
        this(botAction, opLevel);
        registerCommands(cmd);
    }

    /**
        Creates a new instance of TempSettingsManager. Use this Constructor when
        you don't want to use a CommandInterpreter. Note that if you do use this
        constructor you will have to pass screen your messages and pass the commands
        manually to cSet() and c_Get().
        @param botAction The BotAction event to send chat messages with
        @param opLevel The minimum Operator Level to access the settings
    */
    public TempSettingsManager(BotAction botAction, int opLevel)
    {
        this(botAction);

        if(opLevel >= 0 && opLevel <= OWNER_LEVEL)
        {
            m_opLevel = opLevel;
        }
    }

    /**
        Creates a new instance of TempSettingsManager.
        @param botAction The BotAction event to send chat messages with
    */
    public TempSettingsManager(BotAction botAction)
    {
        m_botAction = botAction;
        m_listeners = new Vector<TSChangeListener>();
        m_settings = new HashMap<String, TempSetting>();
        m_locked = false;
        customHelp = null;
        pattern = Pattern.compile("(\\w+)=((\\w+)|(\\-\\d+)|\"([^\"]+)\")");
    }

    /**
        Sets the required access level for an operator to view and modify
        the settings.
        @param opLevel the operator level (as defined in OperatorList)
    */
    public void setOperatorLevel(int opLevel)
    {
        if(opLevel >= 0 && opLevel <= OWNER_LEVEL)
        {
            m_opLevel = opLevel;
        }
    }

    /**
        Registers the commands !get and !set with the provided CommandInterpreter
        @param cmd the CommandInterpreter to use
    */
    public void registerCommands(CommandInterpreter cmd)
    {
        cmd.registerCommand("!get", Message.PRIVATE_MESSAGE, this, "c_Get", m_opLevel);
        cmd.registerCommand("!set", Message.PRIVATE_MESSAGE, this, "c_Set", m_opLevel);
    }

    /**
        Adds a setting into the list, enabling it for use
        @param type The type of setting to be added
        @param name The name of the setting to be added
        @param defval The default value of this setting (what it will be initially)
    */
    public void addSetting(SType type, String name, String defval)
    {
        switch(type)
        {
        case STRING:
            StringSetting sset = new StringSetting(name, defval);
            m_settings.put(name.toLowerCase(), sset);
            break;

        case INT:
            int idefault = Integer.parseInt(defval);
            IntSetting iset = new IntSetting(name, idefault);
            m_settings.put(name.toLowerCase(), iset);
            break;

        case DOUBLE:
            double ddefault = Double.parseDouble(defval);
            DoubleSetting dset = new DoubleSetting(name, ddefault);
            m_settings.put(name.toLowerCase(), dset);
            break;

        case BOOLEAN:
            String arg = defval.toLowerCase();
            boolean bdefault = (arg.equals("true")) || arg.equals("t") || arg.equals("on") || arg.equals("yes") || arg.equals("y");
            BoolSetting bset = new BoolSetting(name, bdefault);
            m_settings.put(name.toLowerCase(), bset);
            break;

        case ENUM:
            EnumSetting eset = new EnumSetting(name, defval);
            m_settings.put(name.toLowerCase(), eset);
            break;

        case PLAYER:
            PlayerSetting pset = new PlayerSetting(name, m_botAction);
            m_settings.put(name.toLowerCase(), pset);
            break;

        default:
            Tools.printLog("Could not add setting " + name + " (unknown type)");
        }
    }

    /**
        Adds a String setting into the list, enabling it for use
        @param name The name of the setting to be added
        @param defval The default value of this setting (what it will be initially)
    */
    public void addSetting(String name, String defval)
    {
        StringSetting sset = new StringSetting(name, defval);
        m_settings.put(name.toLowerCase(), sset);
    }

    /**
        Adds an Integer setting into the list, enabling it for use
        @param name The name of the setting to be added
        @param defval The default value of this setting (what it will be initially)
    */
    public void addSetting(String name, int defval)
    {
        IntSetting iset = new IntSetting(name, defval);
        m_settings.put(name.toLowerCase(), iset);
    }

    /**
        Adds a Double setting into the list, enabling it for use
        @param name The name of the setting to be added
        @param defval The default value of this setting (what it will be initially)
    */
    public void addSetting(String name, double defval)
    {
        DoubleSetting dset = new DoubleSetting(name, defval);
        m_settings.put(name.toLowerCase(), dset);
    }

    /**
        Adds a Boolean setting into the list, enabling it for use
        @param name The name of the setting to be added
        @param defval The default value of this setting (what it will be initially)
    */
    public void addSetting(String name, boolean defval)
    {
        BoolSetting bset = new BoolSetting(name, defval);
        m_settings.put(name.toLowerCase(), bset);
    }

    /**
        Adds an Enumerated setting into the list, enabling it for use
        @param name The name of the setting to be added
        @param values The available opsions for this setting
        @param defval The default value of this setting (what it will be initially)
    */
    public void addSetting(String name, String[] values, String defval)
    {
        EnumSetting eset = new EnumSetting(name, defval);
        eset.setOptions(values);
        m_settings.put(name.toLowerCase(), eset);
    }


    /**
        Adds a Player setting into the list, enabling it for use. The default
        value for this setting is null.
        @param name The name of the setting to be added
    */
    public void addSetting(String name)
    {
        PlayerSetting pset = new PlayerSetting(name, m_botAction);
        m_settings.put(name.toLowerCase(), pset);
    }

    /**
        Removes a setting from the list, no longer enabling it for use.
    */
    public void removeSetting(String name)
    {
        m_settings.remove(name);
    }


    /**
        This is used for when you want to restrict an integer setting within a certain range of numbers
        @param name The name of the setting to restrict
        @param min The minimum value to allow
        @param max The maximum value to allow
    */
    public void restrictSetting(String name, int min, int max)
    {
        TempSetting t = m_settings.get(name.toLowerCase());

        if(t == null)
            Tools.printLog("TempSet: Could not restrict setting " + name + " (doesn't exist)");
        else if(! (t instanceof IntSetting))
            Tools.printLog("TempSet: Could not restrict setting " + name + " (not an int setting)");
        else
        {
            IntSetting iset = (IntSetting)t;
            iset.restrict(min, max);
        }
    }

    /**
        This is used for when you want to restrict a double setting within a certain range of numbers
        @param name The name of the setting to restrict
        @param min The minimum value to allow
        @param max The maximum value to allow
    */
    public void restrictSetting(String name, double min, double max)
    {
        TempSetting t = m_settings.get(name.toLowerCase());

        if(t == null)
            Tools.printLog("TempSet: Could not restrict setting " + name + " (doesn't exist)");
        else if(!(t instanceof DoubleSetting))
            Tools.printLog("TempSet: Could not restrict setting " + name + " (not a double setting)");
        else
        {
            DoubleSetting dset = (DoubleSetting)t;
            dset.restrict(min, max);
        }
    }

    /**
        This is used for when you want to restrict a Player setting within a
        certain range of frequencies (inclusive)
        @param name The name of the setting to restrict
        @param min The minimum frequency &gt;= 0 to allow
        @param max The maximum frequency &lt;= 9999 to allow
    */
    public void restrictPlayerSettingFreq(String name, int min, int max)
    {
        TempSetting t = m_settings.get(name.toLowerCase());

        if(t == null)
            Tools.printLog("TempSet: Could not restrict setting " + name + " (doesn't exist)");
        else if(!(t instanceof PlayerSetting))
            Tools.printLog("TempSet: Could not restrict setting " + name + " (not a Player setting)");
        else
        {
            PlayerSetting pset = (PlayerSetting)t;
            pset.restrictFreq(min, max);
        }
    }

    /**
        Sets whether or not a setting can be set to a player in a particular
        ship, where ship ranges from 0 (warbird) to 7 (shark).
        @param name The name of the setting to restrict
        @param ship the ship to change the value for
        @param ok whether the ship is allowed or not
    */
    public void restrictPlayerSettingShip(String name, int ship, boolean ok)
    {
        TempSetting t = m_settings.get(name.toLowerCase());

        if(t == null)
            Tools.printLog("TempSet: Could not restrict setting " + name + " (doesn't exist)");
        else if(!(t instanceof PlayerSetting))
            Tools.printLog("TempSet: Could not restrict setting " + name + " (not a Player setting)");
        else
        {
            PlayerSetting pset = (PlayerSetting)t;
            pset.setShipAllowed(ship, ok);
        }
    }



    /**
        Gets a setting by name, this is for when you need to access the value in the code of your bot.
        @param name The name of the setting to retrieve
        @return The setting contained in an Object, ready for casting :D
    */
    public Object getSetting(String name)
    {
        TempSetting t = m_settings.get(name.toLowerCase());

        if(t == null)
        {
            Tools.printLog("TempSet: Could not retrieve setting " + name + " (doesn't exist)");
            return null;
        }

        return t.getValue();
    }

    /**
        This changes the value of a setting, with an optional message being returned
        @param name The name of the setting to change
        @param arg The new value you want for the setting
        @return A message describing the success or failure of the set attempt
    */
    public Result setValue(String name, String arg)
    {
        Result result = new Result();
        TempSetting t = m_settings.get(name.toLowerCase());

        if(t == null)
            result.response = "Setting " + name + " does not exist";
        else
            result = t.setValue(arg);

        return result;
    }

    /**
        This takes an incoming chat message and screens it for use in the TempSettingsManager.
        It will pass the message to the appropriate function when necessary.
        @param event the chat message to check for delivery
    */
    public void handleEvent(Message event)
    {
        String sender = m_botAction.getPlayerName(event.getPlayerID());

        if(event.getMessageType() == Message.PRIVATE_MESSAGE && sender != null)
        {
            if(m_botAction.getOperatorList().getAccessLevel(sender) >= m_opLevel)
            {
                String command = event.getMessage();

                if(command.toLowerCase().startsWith("!get "))
                    c_Get(sender, command.substring(5));
                else if(command.toLowerCase().startsWith("!set "))
                    c_Set(sender, command.substring(5));
            }
        }
    }

    /**
        This is the command function registered with the CommandInterpreter under !set
        @param name The name of the player that sent the message
        @param message The parameters sent along after "!set"
    */
    public void c_Set(String name, String message)
    {
        if(message.equalsIgnoreCase("help"))
        {
            if(customHelp != null)
            {
                m_botAction.privateMessageSpam(name, customHelp);
            }
            else
            {
                String[] help = new String[] {
                    "Use the !set command to change temporary bot settings",
                    "Syntax: !set <name1>=<value1> <name2>=<value2> ...",
                    "Modifiable Settings (* means locked):"
                };

                String[] sets = new String[m_settings.size()];
                Iterator<TempSetting> setsIter = m_settings.values().iterator();

                for(int x = 0; x < sets.length; x++)
                    sets[x] = ((TempSetting)setsIter.next()).getInfo();

                m_botAction.privateMessageSpam(name, help);
                m_botAction.privateMessageSpam(name, sets);
            }
        }
        else if(!m_locked)
        {
            Matcher regex = pattern.matcher(message);

            while(regex.find())
            {
                //String old = ""+ getSetting(regex.group(1));
                String val = regex.group(2);

                if(val.startsWith("\"") && val.endsWith("\""))
                    val = val.substring(1, val.length() - 1);

                Result r = setValue(regex.group(1), val);
                m_botAction.sendPrivateMessage(name, r.response);

                if(r.changed)
                {
                    TempSetting t = m_settings.get(regex.group(1).toLowerCase());

                    for(TSChangeListener l : m_listeners)
                        l.settingChanged(t.getName(), t.getValue());
                }
            }
        }
        else
            m_botAction.sendPrivateMessage(name, "Settings are currently locked.");
    }

    /**
        This is the command function registered with the CommandInterpreter under !get
        @param name The name of the player that sent the message
        @param message The parameters sent along after "!get"
    */
    public void c_Get(String name, String message)
    {
        message = message.trim();

        if(message.length() == 0 || message.equalsIgnoreCase("all"))
        {
            for(String setname : m_settings.keySet())
            {
                TempSetting t = m_settings.get(setname);
                m_botAction.sendPrivateMessage(name, t.getName() + "=" + t.getValue());
            }
        }
        else
        {
            TempSetting t = m_settings.get(message.toLowerCase());

            if(t == null)
                m_botAction.sendPrivateMessage(name, "Setting " + message + " does not exist");
            else
                m_botAction.sendPrivateMessage(name, t.getName() + "=" + t.getValue());
        }
    }

    /**
        This locks/unlocks settings from being modified through the command interface (!set)
        @param name The name of the setting to set locked or unlocked
        @param locked Whether settings should be locked or unlocked
    */
    public void setLocked(String name, boolean locked)
    {
        TempSetting t = m_settings.get(name.toLowerCase());

        if(t == null)
            Tools.printLog("TempSet: Setting " + name + " does not exist");
        else
            t.setLocked(locked);
    }

    /**
        This locks/unlocks all settings from being modified through the command interface (!set)
        Individual command locks are preserved, but if you setAllLocked(true), it overrides
        any non-locked commands.
        @param locked Whether settings should be locked or unlocked
    */
    public void setAllLocked(boolean locked)
    {
        m_locked = locked;
    }

    /**
        Registers a TSChangeListener with the TempSettingsManager. Any time a setting is changed
        through the command interface, the registered listeners will receive the settingChanged
        callback.
        @param t the listener to add
    */
    public void addTSChangeListener(TSChangeListener t)
    {
        if(!m_listeners.contains(t))
            m_listeners.add(t);
    }

    /**
        Removes a TSChangeListener from receiving settingChanged callbacks.
        @param t the listener to remove
    */
    public void removeTSChangeListener(TSChangeListener t)
    {
        if(m_listeners.contains(t))
            m_listeners.remove(t);
    }

    /**
        Allows the bot to specify a custom message for !set help. To revert back
        to the default, pass a null object. For no message, pass a new String[0]
        @param customHelp the custom help message to display
    */
    public void setCustomHelp(String[] customHelp)
    {
        this.customHelp = customHelp;
    }
}
