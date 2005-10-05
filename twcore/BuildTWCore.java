import java.io.*;
import java.util.*;


/**
 * Tool for recursively compiling all or part of TWCore.
 * 
 * In order for this utility to run properly, you must have your PATH variable
 * set to the Java bin directory where javac is located, or manually set the
 * binDir variable here.
 * 
 * @author  Stefan / Mythrandir
 */
public class BuildTWCore {

    String extraCP = ":twcore/misc/googleapi.jar:twcore/misc/aim.jar:twcore/misc/mysql-connector-java-3.1.10-bin.jar";
    String binDir = "";             // Location of Java bin directory
    String bldCmd = "";             // Combination of bin dir and the command being used (automatically set)
    Runtime runtime;                // Runtime process
    
    // Command line flags
    boolean bbuildCore = false;     // True to build core
    boolean bbuildAllMisc = false;  // True to build misc
    boolean bbuildAllBots = false;  // True to build bots
    boolean bclearAll = false;      // True to remove all class files and twcore.jar after compile
    boolean bclearNBATT = false;    // True to remove all NetBeans project .nbattr files after compile
    LinkedList botList;             // List of specific bots to compile
    LinkedList miscList;            // List of specific parts of misc to compile
    String currentOS;               // Operating system running
    
    long startTime;                 // Time when the compile began
    
    
    /**
     * Creates a new instance of BuildTWCore.
     */
    public BuildTWCore(String[] args) {
        // get runtime environment
        runtime = Runtime.getRuntime();
        botList = new LinkedList();
        miscList = new LinkedList();
        currentOS = System.getProperty("os.name").toLowerCase();
        if (currentOS.startsWith("windows")) extraCP = extraCP.replace(':', ';'); 
        handleArguments(args);
    }
    
    
    /**
     * Handles the arguments supplied on the command line, setting various options
     * for the build.
     * @param args Arguments supplied
     */
    public void handleArguments(String[] args) {
        String s;
        for (int i=0; i<args.length; i++) {
            s = args[i];
            
            if (s.equals("all")) {
                bbuildCore = true;
                bbuildAllMisc = true;
                bbuildAllBots = true;
            };
            
            if (s.equals("core")) bbuildCore = true;
            else if (s.equals("misc")) bbuildAllMisc = true;
            else if (s.startsWith("misc")) miscList.add("twcore/" + s);
            else if (s.equals("bots")) bbuildAllBots = true;
            else if (s.equals("clear")) bclearAll = true;
            else if (s.equals("clearnb")) bclearNBATT = true;
            else if (s.equals("?") || s.equals("help") || s.equals("-help") || s.equals("/?") || s.equals("\\?")) {
                System.out.println("Usage:");
                System.out.println("  build all    (builds the entire bot)");
                System.out.println("  build core   (builds the core directory)");
                System.out.println("  build misc   (builds the entire misc directory)");
                System.out.println("  build misc/<dirname>   (builds only that directory within misc, not recursive)");
                System.out.println("  build bots   (builds the entire bots directory)");
                System.out.println("  build <botname> <botname> <botname>   (builds only the specified bots)");
                System.out.println("  build clear  (clears all jar and class files after compile)");
                System.out.println("  build clear clearnb (clears all jar, class and netbeans proj files after compile)");
                System.out.println();
                System.out.println("Examples:");
                System.out.println("build misc matchbot pubbot");
                System.out.println("build core misc/      (will only build files in misc/)");
                System.out.println("build core misc/database");

            } else botList.add(s);
        }
        
        if (args.length == 0) {
            bbuildCore = true;
            bbuildAllMisc = true;
            bbuildAllBots = true;
        }
    }
    
    
    /**
     * Executes a new external process and records any output returned from it. 
     * @param s Command and arguments to execute
     * @throws Exception
     */
    public void fullExec(String[] s) throws Exception {
        Process p = runtime.exec(s);
        InputStream stderr = p.getErrorStream();
        InputStreamReader isr = new InputStreamReader(stderr);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ( (line = br.readLine()) != null) { System.out.println(line); };
        p.waitFor();
    }

    
    /**
     * Executes a new external process and records any output returned from it.
     * Wrapper for fullExec(String[]). 
     * @param s Command and arguments to execute
     * @throws Exception
     */
    public void fullExec(String s) throws Exception {
        fullExec(new String[] {s});
    }

    
    /**
     * Creates a file list using the contents of the provided directory.
     * @param dir Directory to list
     * @param fList File to be created
     * @return True if the directory is not empty
     * @throws Exception
     */
    public boolean createFileList(File dir, File fList) throws Exception {
        boolean isnotempty = false;
        if (fList.exists()) fList.delete();
        fList.createNewFile();
        
        PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter(fList)));
        File[] coreList = dir.listFiles();
        
        for (int i=0;i<coreList.length;i++)
            if (coreList[i].getName().endsWith(".java")) {
                out.println(coreList[i].getPath());
                isnotempty = true;
            }
        
        out.close();
        
        return isnotempty;
    }
    

    /**
     * Recursively deletes all files specified in the filelist and their children,
     * if some are directories.
     * @param f File list to start the recursion from
     */
    public void recursiveDelete(File f) {
        if (f.isDirectory()) {
            File[] flist = f.listFiles();
            for (int i=0; i<flist.length; i++) {
                if (flist[i].isDirectory()) 
                    recursiveDelete(flist[i]);
                else
                    if (flist[i].isFile()) {
                        flist[i].delete();
                    }
                    
            }
            f.delete();
        }
    }
    

    /**
     * Recursively deletes class files (and NetBeans project .nbattrs files if specified).
     * @param f File list to start the recursion from
     */
    public void recursiveDeleteClass(File f) {
        if (f.isDirectory()) {
            File[] flist = f.listFiles();
            for (int i=0; i<flist.length; i++) {
                if (flist[i].isDirectory()) 
                    recursiveDeleteClass(flist[i]);
                else
                    if (flist[i].isFile()) 
                        if (flist[i].getName().endsWith(".class") && !flist[i].getName().equalsIgnoreCase("BuildTWCore.class")) 
                            flist[i].delete();
                        else if (flist[i].getName().equals(".nbattrs") && bclearNBATT) flist[i].delete();
                    
            }
        }
    }
            

    /**
     * Recursively compiles the specified directory and all child directories.
     * @param f Directory to begin recursion from
     * @param dig True if recursion should continue
     * @param bot True if the directory being compiled belongs to a bot
     */
    public void recursiveCompile(File f, boolean dig, boolean bot) {
        if (f.isDirectory() && !f.getName().endsWith("CVS") ) {

            // Compile all the files located in here into temp
            try {
                System.out.println("Building " + f.getPath());
                boolean hasContent = createFileList(f, new File("flist.txt"));
                if (hasContent) {               	
                    bldCmd = binDir + "javac";
                    
                    if (!bot)
                        fullExec(new String[] {bldCmd, "-classpath", "twcore.jar" + extraCP, "-sourcepath", f.getPath(), "-d", "temp", "@flist.txt"});
                    else
                        fullExec(new String[] {bldCmd, "-classpath", "twcore.jar" + extraCP, "-sourcepath", f.getPath(), "@flist.txt"});
                }
            } catch (Exception e) { System.out.println("error... " + e.getMessage()); }
            
            
            File[] flist = f.listFiles();
            
            if (dig)
                for (int i=0; i<flist.length; i++)
                    if (flist[i].isDirectory()) {
						recursiveCompile(flist[i], true, bot);
                    }
        }
    }    
    
    
    /**
     * Compiles all files in the core directory. 
     */
    public void buildCore() {
        System.out.println("Building core...");
        try {

            // Create a temporary directory for storage
            File tempDir = new File("temp");
            tempDir.mkdir();

            // Compile
            createFileList(new File("twcore/core"), new File("flist.txt"));
            bldCmd = binDir + "javac";
            fullExec(new String[] { bldCmd, "-sourcepath", "core", "-d", "temp", "@flist.txt"});
            
            // Create the .jar (not as messy as many class files)
                bldCmd = binDir + "jar";
            fullExec(new String[] { bldCmd, "cf", "twcore.jar", "-C", "temp", "."});
            
            recursiveDelete(tempDir);
            
        } catch (Exception e) {
            System.out.println("Couldn't build core: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Finished building core");
        System.out.println();
    }


    /**
     * Compiles all files in the misc directory, and recursively any in child directories.
     */
    public void buildMisc() {
        try {
            boolean doneSomething = false;
            
            // Create a temporary directory for storage
            File tempDir = new File("temp");
            tempDir.mkdir();

            // Compile
            if (bbuildAllMisc) {
                recursiveCompile(new File("twcore/misc"), true, false);
                doneSomething = true;
            } else {
                ListIterator i = miscList.listIterator();
                String s;
                while (i.hasNext()) {
                    s = (String)i.next();
                    if (s.endsWith("/") || s.endsWith("\\")) s = s.substring(0, s.length()-1);
                    recursiveCompile(new File(s), false, false);
                    doneSomething = true;
                }
            }

            // Update the .jar with the misc additions
            if (doneSomething) {
                	bldCmd = binDir + "jar";
                fullExec(new String[] {bldCmd, "uf", "twcore.jar", "-C", "temp", "."});
            }
            recursiveDelete(tempDir);
            
        } catch (Exception e) {
            System.out.println("Couldn't build misc: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Finished building misc");
        System.out.println();
    }

    
    /**
     * Recursively compiles all bots in the bots directory. 
     */
    public void buildBots() { 
        File[] botDirs = new File("twcore/bots").listFiles();
        boolean hasContent;
        
        for (int i=0; i < botDirs.length; i++) {
            if ((bbuildAllBots) || (botList.contains(botDirs[i].getName()))) {
                try {                    
                    hasContent = createFileList(botDirs[i], new File("flist.txt"));
                    // Because the recursive compile handles it, no need to compile here
					if (hasContent)
                        recursiveCompile(botDirs[i], true, true);
                } catch (Exception e) {
                    System.out.println("Error.... " + e.getMessage());
                }
            }
            
        }

    }
    
    
    /**
     * Deletes twcore.jar and all class files. 
     */
    public void clearAll() {
        try {
            System.out.println("Deleting twcore.jar");
            File twcoreJar = new File("twcore.jar");
            if (twcoreJar.exists()) twcoreJar.delete();
                        
            System.out.println("Deleting *.class in /");
            recursiveDeleteClass(new File("."));

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println();
        System.out.println("twcore.jar and all .class cleared.");
    }
    

    /**
     * Begins the compiling process. 
     */
    public void gogogo() {
        startTime = System.currentTimeMillis();
        if (bbuildCore) buildCore();
        buildMisc();
        buildBots();
        if (bclearAll) clearAll();
        System.out.println();
        System.out.println("Finished. (" + (System.currentTimeMillis()-startTime) + " ms)");
        
        // clear temp file
        File tmpFile = new File ("flist.txt");
        if (tmpFile.exists()) tmpFile.delete();
    }


    /**
     * Creates a new instance of BuildTWCore, and begins the compile.
     * @param args Command line arguments passed
     */
    public static void main( String args[] ){
        BuildTWCore b = new BuildTWCore(args);
        b.gogogo();
    }
    
}

