package twcore.bots.zbot;

import java.util.TimerTask;

/**
 * <p>Title:</p> DetailedTimerTask
 * <p>Description:</p> This class keeps track of how long is left till the
 * next advert.
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>For SSCU Trench Wars
 * @author Cpt.Guano!
 * @version 1.0
 */
public abstract class DetailedTimerTask extends TimerTask
{
  private long startTime;
  private long lifetime;

  /**
   * This method initializes the AdvertTimer to a certain length of time (in
   * milliseconds).
   *
   * @param timerLength is the time till the next advert.
   */
  public DetailedTimerTask(long lifetime)
  {
    startTime = System.currentTimeMillis();
    this.lifetime = lifetime;
  }

  /**
   * This method gets the lifetime of the entry.
   *
   * @return the number of milliseconds that the entry will remain in the set
   * for is returned.
   */
  public long getLifetime()
  {
    return lifetime;
  }

  /**
   * This method gets the amount of time remaining before this object expires.
   *
   * @return the number of milliseconds remaining before the object expires
   * is returned.
   */
  public long getTimeRemaining()
  {
    return lifetime - getTimeElapsed();
  }

  /**
   * This method checks to see if the timer has expired.
   *
   * @return true is returned if the timer has expired.
   */
  public boolean isExpired()
  {
    return getTimeRemaining() <= 0;
  }

  /**
   * This method gets the amount of time that the object has been in existance
   * for.
   *
   * @return the amount of time that has elapsed since the object has been
   * created is returned.
   */
  public long getTimeElapsed()
  {
    return System.currentTimeMillis() - startTime;
  }

  /**
   * This method converts the time remaining into a string of the form:
   * "MM mins and SS secs.".  If the timer has expired then
   * "0 mins and 0 secs" is returned.
   *
   * @return a string representing the time remaining is returned.
   */
  public String toString()
  {
    long timeRemaining = getTimeRemaining();
    int mins;
    int secs;

    if(timeRemaining <= 0)
      return "0 mins and 0 secs";
    secs = (int) (timeRemaining / 1000 % 60);
    mins = (int) (timeRemaining / 1000 / 60);

    return StringTools.pluralize(mins, "min") + " and " + StringTools.pluralize(secs, "sec");
  }

  public abstract void run();
}
