import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Parses HelpResponses.txt file to either BBCode or HTML
 * 
 * @author Maverick
 */

public class HelpResponsesParser {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HelpResponsesParser parser = new HelpResponsesParser();
		
		if(args.length > 0 && args[0].toLowerCase().equals("bbcode")){
			if(args.length == 2) {
				parser.parseBBCode(args[1]);
			} else {
				parser.parseBBCode("HelpResponses.txt");
			}
				
		} else if(args.length > 0 && args[0].toLowerCase().equals("html")) {
			if(args.length == 2) {
				parser.parseHTML(args[1]);
			} else {
				parser.parseHTML("HelpResponses.txt");
			}
		} else {
			parser.showHelp();
		}

	}

	private void showHelp() {
		String help = 	"Parses the HelpResponses.txt to a readable format." + "\n" +
						" " + "\n" +
						"Syntax: HELPRESPONSESPARSER [command] [file]" + "\n" +
						" " + "\n" +
						"[command] can be one of: " + "\n" +
						"          BBCODE  Parses the HelpResponses to readable BBCode." + "\n" +
						"          HTML    Parses the HelpResponses to readable HTML." + "\n" +
						" " + "\n" +
						"[file]    specifies the HelpResponses file to parse. Defaults to HelpResponses.txt .";
		System.out.print(help);
	}
	
	private void parseBBCode(String file) {
		System.out.println("Parsing to BBCode...");
		System.out.println("Opening "+file+"...");
		
		File HelpResponsesFile = new File(file);
		if(this.checkFile(HelpResponsesFile) == false) {
			return;
		}
		
		System.out.println("Parsing "+file+" and writing to HelpResponses.bbcode ...");
		// Read and parse the file
		try {
            FileInputStream fstream = new FileInputStream(HelpResponsesFile);
            FileOutputStream ostream = new FileOutputStream("HelpResponses.bbcode");
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            PrintWriter out = new PrintWriter(ostream);
            String line;

            while((line = br.readLine()) != null) {
            	String[] pieces = line.split("\\|");
            	String[] keywords = pieces[0].split(" ");
            	
            	String keywordsOutput = "";
            	String notmatchingKeywordsOutput = "";
            	String extraKeywordsOutput = "";
            	for(String keyword:keywords) {
            		if(keyword.startsWith("&")) {
            			keywordsOutput += keyword.substring(1) + " ";
            		} else if(keyword.startsWith("-")) {
            			notmatchingKeywordsOutput += keyword.substring(1) + " ";
            		} else if(keyword.startsWith("+")) {
            			extraKeywordsOutput += keyword.substring(1) + " ";
            		} else {
            			keywordsOutput += keyword;
            		}
            	}
            	
            	out.print("[b]"+keywordsOutput+"[/b] ");
            	if(extraKeywordsOutput.length()>0) {
            		out.print("([color=lightblue]"+extraKeywordsOutput+"[/color]) ");
            	} 
            	if(notmatchingKeywordsOutput.length()>0) {
            		out.print("[[color=red]"+notmatchingKeywordsOutput+"[/color]] ");
            	}
            	out.print("\n");
            	out.println("[i]"+pieces[1]+"[/i]");
            	out.println("");
            }
            
			br.close();
			fstream.close();
			out.close();
			ostream.close();
		} catch(FileNotFoundException fnfe) {
			
		} catch(IOException ioe) {
			
		}
	}
	
	private void parseHTML(String file) {
		System.out.println("Parsing to HTML...");
		System.out.println("Opening "+file+"...");
		
		File HelpResponsesFile = new File(file);
		if(this.checkFile(HelpResponsesFile) == false) {
			return;
		}
		
		System.out.println("Parsing "+file+" and writing to HelpResponses.html ...");
		// Read and parse the file
		try {
            FileInputStream fstream = new FileInputStream(HelpResponsesFile);
            FileOutputStream ostream = new FileOutputStream("HelpResponses.html");
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            PrintWriter out = new PrintWriter(ostream);
            String line;
            
            out.println("<html><head></head><body>");
            out.println("<div id=\"topbar\"><a href=\"\" onClick=\"closebar(); return false\">[X]</a>");
            out.println("<b>Keywords</b> <font color=\"blue\">Extra keywords</font> &nbsp;&nbsp; <font color=\"red\">Not matching keywords</font>");
            out.println("</div>");

            while((line = br.readLine()) != null) {
            	String[] pieces = line.split("\\|");
            	String[] keywords = pieces[0].split(" ");
            	
            	String keywordsOutput = "";
            	String notmatchingKeywordsOutput = "";
            	String extraKeywordsOutput = "";
            	for(String keyword:keywords) {
            		if(keyword.startsWith("&")) {
            			keywordsOutput += keyword.substring(1) + " ";
            		} else if(keyword.startsWith("-")) {
            			notmatchingKeywordsOutput += keyword.substring(1) + " ";
            		} else if(keyword.startsWith("+")) {
            			extraKeywordsOutput += keyword.substring(1) + " ";
            		} else {
            			keywordsOutput += keyword;
            		}
            	}
            	
            	out.print("<p><b>"+keywordsOutput+"</b> ");
            	if(extraKeywordsOutput.length()>0) {
            		out.print("(<font color=\"blue\">"+EscapeChars.toDisableTags(extraKeywordsOutput)+"</font>) ");
            	} 
            	if(notmatchingKeywordsOutput.length()>0) {
            		out.print("[<font color=red>"+EscapeChars.toDisableTags(notmatchingKeywordsOutput)+"</font>] ");
            	}
            	out.print("<br/>\n");
            	out.println("<em>"+EscapeChars.toDisableTags(pieces[1])+"</em></p>\n");
            }
            
            out.println("</body></html>");
            
			br.close();
			fstream.close();
			out.close();
			ostream.close();
		} catch(FileNotFoundException fnfe) {
			
		} catch(IOException ioe) {
			
		}
	}
	

	// Helper functions
	private boolean checkFile(File file) {
		if(file.exists() == false) {
			System.out.println("Can't find "+file+". Aborting.");
			return false;
		}
		if(file.isDirectory()) {
			System.out.println(file+" is a directory. Aborting.");
			return false;
		}
		if(file.isFile() == false || file.canRead() == false) {
			System.out.println("Can't read "+file+". Aborting.");
			return false;
		}
		return true;
	}
}
