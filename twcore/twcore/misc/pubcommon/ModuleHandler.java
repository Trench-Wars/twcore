package twcore.misc.pubcommon;

import java.util.*;
import java.io.*;
import twcore.core.*;

public class ModuleHandler
{
  private AdaptiveClassLoader loader;
  private HashMap moduleList;
  private File moduleLocation;
  private String moduleGroup;
  private BotAction m_botAction;

  /**
   * This method initializes a new ModuleHandler class.
   *
   * @param modulePath is the path of the module .class files.
   * @param moduleGroup is the name of the group of modules.  All of the
   * module files must begin with this string.
   * @param botAction is the botAction of the bot.
   */

  public ModuleHandler(BotAction botAction, String modulePath, String moduleGroup)
  {
    m_botAction = botAction;
    initializeLoader(modulePath);
    this.moduleGroup = moduleGroup;
    moduleList = new HashMap();
  }

  /**
   * This method loads a module into the handler.
   *
   * @param moduleName is the name of the module to load.
   */

  public void loadModule(String moduleName)
  {
    String lowerName = moduleName.toLowerCase();

    if(moduleList.containsKey(lowerName))
      throw new IllegalArgumentException("Module: " + lowerName + " is already loaded.");

    try
    {
      if(loader.shouldReload())
        loader.reinstantiate();
      Module module = (Module) loader.loadClass("twcore.bots." + moduleGroup + "." + moduleGroup + moduleName).newInstance();
      module.initializeModule(m_botAction);
      moduleList.put(moduleName, module);
    }
    catch(Exception e)
    {
      Tools.printStackTrace(e);
      throw new RuntimeException("ERROR: Could not load " + moduleName);
    }
  }

  /**
   * This method unloads a module.
   *
   * @param moduleName is the name of the module to unload.
   */

  public void unloadModule(String moduleName)
  {
    String lowerName = moduleName.toLowerCase();

    if(!moduleList.containsKey(moduleName))
      throw new IllegalArgumentException("ERROR: Module: " + moduleName + " is not loaded.");
    moduleList.remove(moduleName);
  }

  /**
   * This method checks to see if a module is loaded.
   *
   * @param moduleName is the name of the module to load.
   * @param true is returned if the module is loaded.
   */

  public boolean isLoaded(String moduleName)
  {
    return moduleList.containsKey(moduleName.toLowerCase());
  }

  /**
   * This method checks to see if a module is able to be loaded.
   *
   * @param moduleName is the module name to check.
   * @return true is returned if the module can be loaded.
   */

  public boolean isModule(String moduleName)
  {
    String fileName = moduleGroup + moduleName.toLowerCase() + ".class";

    String[] moduleNames = moduleLocation.list(new ModuleFilenameFilter());
    for(int index = 0; index < moduleNames.length; index++)
    {
      if(fileName.equals(moduleNames[index]))
        return true;
    }
    return false;
  }

  /**
   * This method gets a loaded Module instance from the module list.
   *
   * @param moduleName is the name of the module to get.
   */

  public Module getModule(String moduleName)
  {
    String lowerName = moduleName.toLowerCase();

    if(!moduleList.containsKey(lowerName))
      throw new IllegalArgumentException("ERROR: " + moduleName + " is not loaded.");
    return (Module) moduleList.get(lowerName);
  }

  /**
   * This method gets a sorted list of the modules that the module handler is
   * able to load.  The files must begin with the module group name and end with
   * .class.  It must also not have a $ in the name.
   *
   * @return a sorted collection of module names is returned.
   */

  public Collection getModuleNames()
  {
    String[] fileNames = moduleLocation.list(new ModuleFilenameFilter());
    int beginIndex = moduleGroup.length();
    String fileName;
    Vector modules = new Vector();

    for(int index = 0; index < fileNames.length; index++)
    {
      fileName = fileNames[index];
      modules.add(fileName.substring(beginIndex, fileName.length() - 6));
    }
    Collections.sort(modules);
    return modules;
  }

  /**
   * This method handles all of the subspace events.
   *
   * @param event is the event to handle.
   */

  public void handleEvent(SubspaceEvent event)
  {
    Collection collection = moduleList.values();
    Iterator iterator = collection.iterator();
    Module module;

    while(iterator.hasNext())
    {
      module = (Module) iterator.next();
      module.handleEvent(event);
    }
  }

  /**
   * This method initializes the Adaptive Class Loader so that modules may be
   * loaded.  The modules should be located in the modulePath directory.
   *
   * @param modulePath is the path where the moduleHandler will look for the
   * modules.
   */

  private void initializeLoader(String modulePath)
  {
    Vector repository = new Vector();

    repository.add( new File( m_botAction.getGeneralSettings().getString( "Core Location" ) ) );
    moduleLocation = new File(modulePath);
    repository.add(moduleLocation);
    loader = new AdaptiveClassLoader(repository, getClass().getClassLoader());
  }

  /**
   * This class is used to filter the files in modulePath.  It selects files
   * that begin with moduleGroup, that end in .class and that do not have $
   * in their names.
   */

  private class ModuleFilenameFilter implements FilenameFilter
  {
    public boolean accept(File file, String name)
    {
      int minLength = moduleGroup.length() + 6;
      return name.startsWith(moduleGroup) && name.endsWith(".class") &&
          name.length() > minLength && name.indexOf('$') == -1;
    }
  }
}