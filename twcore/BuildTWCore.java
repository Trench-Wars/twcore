/*
 * BuildTWCore.java
 *
 * Created on March 14, 2004, 9:48 PM
 */

import java.io.*;
import java.util.*;


/**
 *
 * @author  Stefan / Mythrandir
 */
public class BuildTWCore {

    String extraCP = ":twcore/misc/googleapi.jar:twcore/misc/aim.jar:twcore/misc/mysql-connector-java-3.1.10-bin.jar";
    String binDir = "";  // Location of bin directory
    String bldCmd = "";
    Runtime runtime;
    
    // flags
    boolean bbuildCore = false;
    boolean bbuildAllMisc = false;
    boolean bbuildAllBots = false;
    boolean bclearAll = false;
    boolean bclearNBATT = false;
    LinkedList botList;
    LinkedList miscList;
    String currentOS;
    
    long startTime;
    
    
    /** Creates a new instance of BuildTWCore */
    public BuildTWCore(String[] args) {
        // get runtime environment
        runtime = Runtime.getRuntime();
        botList = new LinkedList();
        miscList = new LinkedList();
        currentOS = System.getProperty("os.name").toLowerCase();
        if (currentOS.startsWith("windows")) extraCP = extraCP.replace(':', ';'); 
        handleArguments(args);
    }
    
    
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
                System.out.println("usage:");
                System.out.println("  build all   (builds the entire bot)");
                System.out.println("  build core   (builds the core directory)");
                System.out.println("  build misc   (builds the entire misc directory)");
                System.out.println("  build misc/<dirname>   (builds only that directory within misc, not recursive)");
                System.out.println("  build bots   (builds the entire bots directory)");
                System.out.println("  build <botname> <botname> <botname>   (builds only the specified bots)");
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
    
    public void fullExec(String[] s) throws Exception {
        Process p = runtime.exec(s);
        InputStream stderr = p.getErrorStream();
        InputStreamReader isr = new InputStreamReader(stderr);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ( (line = br.readLine()) != null) { System.out.println(line); };
        p.waitFor();
    }
    
    public void fullExec(String s) throws Exception {
        fullExec(new String[] {s});
    }

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
    };
    
    
    public void recursiveDelete(File f) {
        if (f.isDirectory()) {
            File[] flist = f.listFiles();
            for (int i=0; i<flist.length; i++) {
                if (flist[i].isDirectory()) 
                    recursiveDelete(flist[i]);
                else
                    if (flist[i].isFile()) {
                        flist[i].delete();
                    };
                    
            }
            f.delete();
        }
    }
    
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
    
    
    public void buildCore() {
        System.out.println("Building core...");
        try {
            // create a temporary directory
            File tempDir = new File("temp");
            tempDir.mkdir();
            
            // compile all the files into there

            createFileList(new File("twcore/core"), new File("flist.txt"));
                bldCmd = binDir + "javac";
            fullExec(new String[] { bldCmd, "-sourcepath", "core", "-d", "temp", "@flist.txt"});
            
            // create the jar
                bldCmd = binDir + "jar";
            fullExec(new String[] { bldCmd, "cf", "twcore.jar", "-C", "temp", "."});
            
            recursiveDelete(tempDir);
            
        } catch (Exception e) {
            System.out.println("Couldn't build core: " + e.getMessage());
            e.printStackTrace();
        };
        System.out.println("Finished building core");
    }
    
        
    public void recursiveCompile(File f, boolean dig, boolean bot) {
        if (f.isDirectory() && !f.getName().endsWith("CVS") ) {
            // compile all the files located in here into temp
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
    
    
    public void buildMisc() {
        try {
            boolean doneSomething = false;
            
            // create a temporary directory
            File tempDir = new File("temp");
            tempDir.mkdir();
            
            // compile all the files into there            
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
                };
            };

            // create the jar
            if (doneSomething) {
                	bldCmd = binDir + "jar";
                fullExec(new String[] {bldCmd, "uf", "twcore.jar", "-C", "temp", "."});
            }
            recursiveDelete(tempDir);
            
        } catch (Exception e) {
            System.out.println("Couldn't build misc: " + e.getMessage());
            e.printStackTrace();
        };
    };
    
    public void buildBots() { 
        File[] botDirs = new File("twcore/bots").listFiles();
        boolean hasContent;
        
        for (int i=0; i < botDirs.length; i++) {
            if ((bbuildAllBots) || (botList.contains(botDirs[i].getName()))) {
                try {
                    
                    hasContent = createFileList(botDirs[i], new File("flist.txt"));
					if (hasContent) {
						System.out.println("Building " + botDirs[i].getName());
						fullExec(new String[] {"javac", "-classpath", "twcore.jar" + extraCP, "-sourcepath", botDirs[i].getPath(), "@flist.txt"});
					}
					recursiveCompile(botDirs[i], true, true);
                } catch (Exception e) {
                    System.out.println("Error.... " + e.getMessage());
                }
            }
            
        }

    }
    
    
    
    // use this to clear all classes and jars that have been built
    public void clearAll() {
        // delete twcore.jar
        try {
            System.out.println("deleting twcore.jar");
            File twcoreJar = new File("twcore.jar");
            if (twcoreJar.exists()) twcoreJar.delete();
            
            
            System.out.println("deleting *.class in /");
            recursiveDeleteClass(new File("."));

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println("All cleared");
    }
    
    
    public void gogogo() {
        startTime = System.currentTimeMillis();
        if (bbuildCore) buildCore();
        buildMisc();
        buildBots();
        if (bclearAll) clearAll();
        System.out.println("Finished. (" + (System.currentTimeMillis()-startTime) + " ms)");
        
        // clear temp file
        File tmpFile = new File ("flist.txt");
        if (tmpFile.exists()) tmpFile.delete();
    }

    
    public static void main( String args[] ){
        BuildTWCore b = new BuildTWCore(args);
        b.gogogo();
    }
    
}

