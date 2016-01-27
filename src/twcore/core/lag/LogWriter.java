package twcore.core.lag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
    This class provides functionality to create a log file.  Each log entry is
    of a certain format specified by the date pattern.  The default pattern is
    as follows:

    EEE MMM dd yyyy HH:mm:ss zzz:  message

    EEE is the first 3 letters of the day
    MMM is the first 3 letters of the month
    dd is the date
    yyyy is the year
    HH is the hours in 24 hour time
    mm is the minutes
    ss is the seconds
    zzz is the time zone
    message is the entry to record.

    To use the LogWriter, the user should create one instance of it and then pass
    it to the classes that will use it to log.  These classes will use the
    write(String) method to record lines of the log.  This method is synchronized
    so as to avoid concurent disk accesses.  Once the user is done with the log
    file, the close() method should be called.

    @author Cpt.Guano!
    @version 1.0, 20/12/03
*/

public class LogWriter {
    public static final String DEFAULT_DATE_PATTERN = "EEE MMM dd yyyy HH:mm:ss zzz";

    private SimpleDateFormat dateFormat;
    private FileWriter logFile;
    private String fileName;
    private LogWriterState state;

    /**
        This constructor creates a LogWriter associated with a certain filename,
        a given date pattern and an optional append.

        @param fileName is the name of the log file to open.
        @param datePattern is the date pattern to use to format the date with.
        @param append is if the new log entries will be appended to the end of an
        existing log file or not.
        @throws IllegalArgumentException if the filename is null or an empty
        string.
    */
    public LogWriter(String fileName, String datePattern, boolean append) {
        if (fileName == null || fileName.equals(""))
            throw new IllegalArgumentException("ERROR: Invalid file name.");

        openFile(fileName, append);
        this.fileName = fileName;
        dateFormat = new SimpleDateFormat(datePattern);
        state = new LogWriterState();
    }

    /**
        This constructor creates a LogWriter associated with a certain filename,
        the default date pattern and optional append.

        @param fileName is the name of the log file to open.
        @param append is if the new log entries will be appended to the end of an
        existing log file or not.
    */
    public LogWriter(String fileName, boolean append) {
        this(fileName, DEFAULT_DATE_PATTERN, append);
    }

    /**
        This constructor creates a LogWriter associated with a certain filename,
        a datePattern given by the user and append set to false.

        @param fileName is the name of the log file to open.
        @param datePattern is the date pattern to use to format the date with.
    */
    public LogWriter(String fileName, String datePattern) {
        this(fileName, datePattern, false);
    }

    /**
        This constructor creates a LogWriter associated with a certain filename,
        the default date pattern and with append set to false.

        @param fileName is the name of the log file to open.
    */
    public LogWriter(String fileName) {
        this(fileName, DEFAULT_DATE_PATTERN, false);
    }

    /**
        This method returns the file name of the log file.

        @return the fileName of the log file is returned.
    */
    public String getFileName() {
        return fileName;
    }

    /**
        This method writes a line to the logFile.  The line is of the following
        format:

        date:  message

        date is formatted according to the datePattern.  This method is
        synchronized so as to prevent concurrent disk accesses.

        @param message is the message to write to the log file.
        @throws InvalidStateException if the log file is closed.
        @throws RuntimeException if there is an error writing to the file.
    */

    public synchronized void write(String message) {
        if (state.isCurrentState(LogWriterState.CLOSED_STATE))
            throw new InvalidStateException("ERROR: Unable to write to file.  File stream is closed.");

        if (state.isCurrentState(LogWriterState.ENABLED_STATE)) {
            try {
                logFile.write(dateFormat.format(new Date() + ":  " + message));
                logFile.flush();
            } catch (IOException e) {
                throw new RuntimeException("ERROR: Unable to write to file.");
            }
        }
    }

    /**
        This method sets the logWriter to enabled or disabled.  It may not be
        called if the LogWriter is closed.

        @param enabled sets the state to Enabled if it is true or Disabled if it
        is false.
        @throws InvalidStateException if the LogWriter is closed.
    */
    public void setEnabled(boolean enabled) {
        if (state.isCurrentState(LogWriterState.CLOSED_STATE))
            throw new InvalidStateException("ERROR: Log file is closed.");

        if (enabled)
            state.setCurrentState(LogWriterState.ENABLED_STATE);
        else
            state.setCurrentState(LogWriterState.DISABLED_STATE);
    }

    /**
        This method flushes the stream and closes the file.

        @throws InvalidStateException if the log file is already closed.
    */
    public void close() {
        if (state.isCurrentState(LogWriterState.CLOSED_STATE))
            throw new InvalidStateException("ERROR: Log file is already closed.");

        try {
            logFile.close();
            state.setCurrentState(LogWriterState.CLOSED_STATE);
        } catch (IOException e) {
            throw new RuntimeException("ERROR: Unable to close log file.");
        }
    }

    /**
        This private method opens a file for logging.  Based on the append
        parameter, the file will be either opened for append or for write over.

        @param fileName is the name of the file to open.
        @param append is true if the user wishes to append to the end of the file.
        @throws RuntimeException if the file fails to open.
    */
    private void openFile(String fileName, boolean append) {
        try {
            File file = new File(fileName);
            logFile = new FileWriter(file, append);
        } catch (IOException e) {
            throw new RuntimeException("ERROR: Unable to open log file.");
        }
    }

    /**
                    root
            open          closed
        enabled disabled
    */
    private class LogWriterState extends State {
        public static final String OPEN_STATE = "Open";
        public static final String CLOSED_STATE = "Closed";
        public static final String ENABLED_STATE = "Enabled";
        public static final String DISABLED_STATE = "Disabled";

        public LogWriterState() {
            addState(OPEN_STATE);
            addState(CLOSED_STATE);
            addState(OPEN_STATE, ENABLED_STATE);
            addState(OPEN_STATE, DISABLED_STATE);
            setCurrentState(OPEN_STATE);
        }
    }
}