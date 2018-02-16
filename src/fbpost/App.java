package fbpost;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

//import facebook4j.Comment;

// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.SQLException;

public class App {
	//==============================================
	// the usual
	//==============================================
	public static final String name = "FBPostBot";
	public static final String author = "jboby93";
	public static final String version = "2.0";
	public static final String build_date = "2/15/2018";
	private static final int log_level = -1; //set to -1 for no logging messages sent to stdout
	public static final String NL = System.getProperty("line.separator");
	
	//social media connections
	private static FBPage page = null;

	//9-20: page debug mode, so entire app can be tested without worrying about making posts to the page
	private static boolean debugMode = false; //if true, fb posts are sent to stdout instead of the page

	//==============================================
	// main()
	//==============================================
	public static void main(String args[]) {
		try {
			openLogFile();
			log(name + " - v" + version + " (" + build_date + ")");
			
			Map<String, String> pagesList = new HashMap<String, String>();
			if((new File("pages.lst")).exists()) {
				String pagesListIn[] = readFile("pages.lst").split("\n");
				for(String p : pagesListIn) {
					String k = p.split("=")[0].trim();
					String v = p.replace(k + "=", "").trim();
					pagesList.put(k, v);
				}
			}

			if(args.length > 0) {
				log("main(): got " + args.length + " arguments from command line");

				// build list of actions to perform as arguments are parsed
				ArrayList<FBAction> actions = new ArrayList<FBAction>();

				// authfile to apply to commands (only needs defined once at beginning, except for share actions which override it)
				String authfile = "null";

				try {
					//handle args
					for(int i = 0; i < args.length; i++) {
						//int a = actions.size() - 1; //get index of current action

						log("main(): argument '" + args[i] + "'", 0);

						FBAction action = null;

						// if starts with -, new action starts
						// if starts with --, is parameter toward the current action
						
						switch(args[i]) {
						case "-auth":
							if(i + 1 >= args.length) {
								//missing
								log(" - missing argument for authfile");
							} else {
								authfile = args[++i];
								log(" - authfile: " + authfile);
							}
							break;
						case "-extend":
							//just extends the loaded access token
							action = new ExtendTokenAction();

							break;
						case "-heartbeat":
							action = new HeartbeatAction();

							String pname = authfile.replace("auth/", "").replace(".ini", "");
							if(pagesList.containsKey(pname)) {
								action.setParam("pageid", pagesList.get(pname));
								log(" - heartbeat for page " + pname + " (id " + action.getParamStr("pageid") + ")");
							} else {
								action.setParam("pageid", "126951917963098"); //hardcode value for hgb74
							}
							break;
						case "-post":
							// --text=""
							// --image=[filename, optional]
							//docommenton = "null";

							action = new PostAction();
							action.setParam("poststyle", "default");

							i++;
							log("args[" + i + "] = " + args[i]);
							while(i < args.length && args[i].substring(0, 2).equals("--")) {
								switch(args[i]) {
								case "--text":
									action.setParam("text", args[++i]);
									//params.put("text", args[++i]);
									log(" - text: args[" + i + "] = " + action.getParamStr("text"));
									break;
								case "--file":
									//get post text from file
									action.setParam("file", args[++i]);
									log(" - text: from file args[" + i + "] = " + args[i]);

									break;
								case "--image":
									action.setParam("image", args[++i]);
									log(" - image: args[" + i + "] = " + action.getParamStr("imgfile"));
									break;
								case "--system":
									action.setParam("poststyle", "system");
									log(" - posting as system message");
									break;
								case "--dev":
									action.setParam("poststyle", "dev");
									log(" - posting as developer message");
									break;
								}

								i++;
							}
							break;
						case "-comment":
							//--postid=[]
							//--text=""
							//--image=[filename, optional]
							//
							action = new CommentAction();

							i++;
							while(i < args.length && args[i].substring(0, 2).equals("--")) {
								switch(args[i]) {
								case "--postid":
									action.setParam("postid", args[++i]);
									log(" - postid: args[" + i + "] = " + action.getParamStr("postid"));
									break;
								case "--page":
									String pageid_c = args[++i];
									if(pagesList.containsKey(pageid_c)) {
										action.setParam("pageid", pagesList.get(pageid_c));
										log(" - commenting on post on page " + pageid_c + " (id " + action.getParamStr("pageid") + ")");
									} else {
										action.setParam("pageid", pageid_c);
										log(" - commenting on post on page with id " + action.getParamStr("pageid"));
									}
									break;
								case "--text":
									action.setParam("text", args[++i]);
									log(" - text: args[" + i + "] = " + action.getParamStr("text"));
									break;
								case "--file":
									//get post text from file
									action.setParam("file", args[++i]);
									log(" - text: from file args[" + i + "] = " + args[i]);
									break;
								case "--image":
									action.setParam("image", args[++i]);
									log(" - image: args[" + i + "] = " + action.getParamStr("image"));
									break;
								case "--system":
									action.setParam("poststyle", "system");
									log(" - posting as system message");
									break;
								case "--dev":
									action.setParam("poststyle", "dev");
									log(" - posting as developer message");
									break;
								}

								i++;
							}
							break;
						case "-share":
							// --text - optional text to post with the url
							// --url  - url to share

							action = new ShareAction();
							action.setParam("poststyle", "default");

							i++;
							log("args[" + i + "] = " + args[i]);
							while(i < args.length && args[i].substring(0, 2).equals("--")) {
								switch(args[i]) {
								case "--text":
									action.setParam("text", args[++i]);
									log(" - text: args[" + i + "] = " + action.getParamStr("text"));
									break;
								case "--file":
									//get post text from file
									action.setParam("file", args[++i]);
									log(" - text: from file args[" + i + "] = " + args[i]);
									break;
								case "--url":
									action.setParam("url", args[++i]);
									log(" - url: args[" + i + "] = " + action.getParamStr("url"));
									break;
								case "--to":
									String s_to = args[++i];

									if(pagesList.containsKey(s_to)) {
										action.addToParamList("to", pagesList.get(s_to));
										log(" - sharing to page " + s_to + " (id " + pagesList.get(s_to) + ")");
									} else {
										action.addToParamList("to", s_to);
										log(" - sharing to page with id " + s_to);
									}
									//shareto.add(args[++i]);
									//log(" - to: will share link to page identified as " + args[i]);
									break;
								case "--as":
									String s_as = args[++i];

									if(pagesList.containsKey(s_as)) {
										action.addToParamList("as", pagesList.get(s_as));
										log(" --- sharing as page " + s_as + " (id " + pagesList.get(s_as) + ")");
									} else {
										action.addToParamList("as", s_as);
										log(" --- sharing as page with id " + s_as);
									}
									//shareto.add(args[++i]);
									//log(" - to: will share link to page identified as " + args[i]);
									break;
								case "--system":
									action.setParam("poststyle", "system");
									log(" - posting as system message");
									break;
								case "--dev":
									action.setParam("poststyle", "dev");
									log(" - posting as developer message");
									break;
								}

								i++;
							}
							break;
						case "-getpost":
							action = new GetPostAction();
							action.setParam("format", "text");

							i++;
							while(i < args.length && args[i].substring(0, 2).equals("--")) {
								switch(args[i]) {
								case "--postid":
									action.setParam("postid", args[++i]);
									log(" - postid: args[" + i + "] = " + action.getParamStr("postid"));
									break;
								case "--page":
									String pageid_gp = args[++i];
									if(pagesList.containsKey(pageid_gp)) {
										action.setParam("pageid", pagesList.get(pageid_gp));
										log(" - getting post on page " + pageid_gp + " (id " + action.getParamStr("pageid") + ")");
									} else {
										action.setParam("pageid", pageid_gp);
										log(" - getting post on page with id " + action.getParamStr("pageid"));
									}
									break;
								case "--json":
									action.setParam("format", "json");
									break;
								}

								i++;
							}
							break;
						case "-getcomments":
							action = new GetCommentsAction();
							action.setParam("format", "text");

							i++;
							while(i < args.length && args[i].substring(0, 2).equals("--")) {
								switch(args[i]) {
								case "--postid":
									action.setParam("postid", args[++i]);
									log(" - postid: args[" + i + "] = " + action.getParamStr("postid"));
									break;
								case "--page":
									String pageid_gc = args[++i];
									if(pagesList.containsKey(pageid_gc)) {
										action.setParam("pageid", pagesList.get(pageid_gc));
										log(" - getting comments from post on page " + pageid_gc + " (id " + action.getParamStr("pageid") + ")");
									} else {
										action.setParam("pageid", pageid_gc);
										log(" - getting comments from post on page with id " + action.getParamStr("pageid"));
									}
									break;
								case "--json":
									action.setParam("format", "json");
									break;
								}

								i++;
							}
							break;
						case "-getreacts": //requires page and post id
							action = new GetReactsAction();

							i++;
							while(i < args.length && args[i].substring(0, 2).equals("--")) {
								switch(args[i]) {
								case "--postid":
									action.setParam("postid", args[++i]);
									log(" - postid: args[" + i + "] = " + action.getParamStr("postid"));
									break;
								case "--page":
									String pageid_gr = args[++i];
									if(pagesList.containsKey(pageid_gr)) {
										action.setParam("pageid", pagesList.get(pageid_gr));
										log(" - analyzing post on page " + pageid_gr + " (id " + action.getParamStr("pageid") + ")");
									} else {
										action.setParam("pageid", pageid_gr);
										log(" - analyzing post on page with id " + action.getParamStr("pageid"));
									}
									break;
								}

								i++;
							}
							break;
						case "-postsreport": //requires page and date range
							action = new GetPostsAction();
							action.setParam("format", "text");

							i++;
							Calendar r_cal = Calendar.getInstance();

							while(i < args.length && args[i].substring(0, 2).equals("--")) {
								switch(args[i]) {
								case "--page":
									String pageid_r = args[++i];
									if(pagesList.containsKey(pageid_r)) {
										action.setParam("pageid", pagesList.get(pageid_r));
										log(" - analyzing page " + pageid_r + " (id " + action.getParamStr("pageid") + ")");
									} else {
										action.setParam("pageid", pageid_r);
										log(" - analyzing page with id " + action.getParamStr("pageid"));
									}
									break;
								case "--start":
									String rStart[] = args[++i].split("-");
									r_cal.set(Integer.parseInt(rStart[2]), Integer.parseInt(rStart[0]) - 1, Integer.parseInt(rStart[1]), 0, 0, 0);
									action.setParam("starttime", r_cal.getTime());

									break;
								case "--end":
									String rEnd[] = args[++i].split("-");
									r_cal.set(Integer.parseInt(rEnd[2]), Integer.parseInt(rEnd[0]) - 1, Integer.parseInt(rEnd[1]), 23, 59, 59);
									action.setParam("endtime", r_cal.getTime());

									break;
								case "--html":
									action.setParam("format", "html");
									break;
								case "--json":
									action.setParam("format", "json");
									break;
								case "--saveas":
									action.setParam("saveas", args[++i]);
									log(" - report will be saved to " + args[i]);
									break;
								case "--sort":
									action.setParam("sort", true);
									log(" - posts will be sorted by reaction counts");
									break;
								}

								i++;
							}

							break;
						case "-help":
						case "--help":
						case "-?":
							// print help text
							break;
						} //end switch (args)

						if(action != null) {
							action.setAuthFile(authfile);
							actions.add(action);
						}

						//i++; //since every switch must have a parameter, we need to advance the loop by two
					} //end for

					log("");
					log("done parsing args");
					log("");

					page = new FBPage();

					log(actions.size() + " actions to process:");
					for(int ac = 0; ac < actions.size(); ac++) {
						if(actions.get(ac).validate()) {
							int rtn = actions.get(ac).execute(page);

							// check response
							if(rtn == 0) {
								log(" - action " + (ac + 1) + " succeeded with code " + rtn);

								switch(actions.get(ac).getType()) {
								case FBAction.ACTION_POST:
									log("   posted to page with id " + actions.get(ac).getResponseStr("postid"));
									println(actions.get(ac).getResponseStr("postid"));
									break;
								case FBAction.ACTION_COMMENT:
									log("   commented on post " + actions.get(ac).getParamStr("postid") + " with id " + actions.get(ac).getResponseStr("commentid"));
									println(actions.get(ac).getResponseStr("commentid"));
									break;
								case FBAction.ACTION_SHARE:
									ArrayList<String> shareids = (ArrayList<String>)actions.get(ac).getResponse("postids");
									ArrayList<String> topages = (ArrayList<String>)actions.get(ac).getParam("to");

									for(int p = 0; p < topages.size(); p++) {
										println(topages.get(p) + "_" + shareids.get(p));
									}
									break;
								case FBAction.ACTION_GETPOSTS:
									println(actions.get(ac).getResponseStr("output"));
									break;
								case FBAction.ACTION_GETREACTS:
									FBPage.FBPostReactions reacts = (FBPage.FBPostReactions)actions.get(ac).getResponse("reacts");
									println(reacts.toTermString());
									//println(reacts.asJSON().toString());
									break;
								case FBAction.ACTION_GETCOMMENTS:
									ArrayList<FBPage.FBComment> comments = (ArrayList<FBPage.FBComment>)actions.get(ac).getResponse("comments");
									if(actions.get(ac).getParamStr("format").equals("json")) {
										println(page.getJsonForComments(actions.get(ac).getParamStr("postid"), comments).toString());
									} else {
										for(FBPage.FBComment comment : comments) {
											println(comment.toString());
										}
									}
									break;
								case FBAction.ACTION_GETSINGLEPOST:
									FBPage.FBPost post = (FBPage.FBPost)actions.get(ac).getResponse("post");
									if(actions.get(ac).getParamStr("format").equals("json"))
										println(post.asJSON().toString());
									else
										println(post.toString());
									break;
								case FBAction.ACTION_EXTENDTOKEN:
									//nothing to do here
									break;
								case FBAction.ACTION_HEARTBEAT:
									if(actions.get(ac).getResponse("results") == null) {
										println("heartbeat failed -- see log");
									} else {
										ArrayList<String> results = (ArrayList<String>)actions.get(ac).getResponse("results");
										println("heartbeat successful -- " + results.size() + " posts found in last 24 hours on page with id " + actions.get(ac).getParamStr("pageid"));
									}
									break;
								}
							} else {
								log(" - action " + (ac + 1) + " failed and returned error code " + rtn);
							}
						} else {
							log(" - action " + (ac + 1) + " failed validation; missing or invalid parameters");
						}
					}
				} catch(Exception e) {
					log("main(): exception thrown in argument-parsing block");
					logStackTrace(e);
				}
			} else {
				println("no arguments provided; --help for options");
			} //end if (args check)
		} catch(Exception e) {
			App.logStackTrace(e);
		}

		log("main(): exiting");
		closeLogFile();
	} //end main()	
	
	//==============================================
	// usual I/O functions
	//==============================================
	public static void print(String s) { System.out.print(s); }
	public static void println(String s) { System.out.println(s); }
	public static void printStackTrace(Exception e) {
		StringWriter stack = new StringWriter();
		e.printStackTrace(new PrintWriter(stack));
		println("[stack]: " + stack.toString());
	}
	public static String readFile(String file) throws IOException {
		try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line = null;
			StringBuilder sb = new StringBuilder();
			String ls = System.getProperty("line.separator");

			while((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append(ls);
			}
			
			reader.close();
			return sb.toString();
		} catch(IOException e) {
			throw e;
		}
	} //end readFile()
	public static String readLine(String prompt) { return readLine(prompt, false); }
	public static String readLine(String prompt, boolean newlineAfterPrompt) {
		System.out.print(prompt);
		if(newlineAfterPrompt) System.out.println("");
		
		return readLine();
	}
	public static String readLine() {
		try {
			Scanner s = new Scanner(System.in);
			String l = s.nextLine();
			//s.close();
			return l;
		} catch(Exception e) {
			return "readLine(): an exception occurred here? " + e.getMessage();
		}
	}
	public static boolean confirm(String prompt) {
		String response = readLine(prompt + " [y/N]: ");
		return response.toLowerCase().contains("y");
	}
	
	public static void about() {
		println(name + " - v" + version + " (" + build_date + ")");
		//println("developer: " + author);
		println("");
	}

	public static String Left(String str, int length) {
		return str.substring(0, Math.min(length, str.length()));
	}

	public static String Right(String str, int length) {
		return str.substring(str.length() - 1 - length);
	}

	public static boolean fileExists(String f) {
		return (new File(f)).exists();
	}
	public static void writeFile(String file, String text) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			//write the text
			bw.write(text);

			bw.flush();
			bw.close();
		} catch(IOException e) {
			throw e;
		}
	} //end writeFile()
	public static void writeFileImmediately(String file, String text) throws IOException {
		try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
			pw.write(text);

			pw.flush();
			pw.close();
		} catch(IOException e) {
			throw e;
		}
	}

	public static void writeLines(String file, ArrayList<String> lines) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			//write the text
			for(String s : lines) {
				bw.write(s);
				bw.write("\n");
			}

			bw.flush();
			bw.close();
		} catch(IOException e) {
			throw e;
		}
	} //end writeLines()

	public static void appendToFile(String file, String text) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
			//write the text
			bw.write(text);

			bw.flush();
			bw.close();
		} catch(IOException e) {
			throw e;
		}
	}
	
	public static void appendLinesToFile(String file, ArrayList<String> lines) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
			//write the text
			for(String s : lines) {
				bw.write(s);
				bw.newLine();
			}

			bw.flush();
			bw.close();
		} catch(IOException e) {
			throw e;
		}
	}

	public static int tryParseInt(String tryThis, int defaultValue) {
		try {
			int r = Integer.parseInt(tryThis);
			return r;
		} catch(Exception e) {
			log("tryParseInt(): cannot convert " + tryThis + " to int; returning default of " + defaultValue);
			return defaultValue;
		}
	}

	/**************************************************************************************************
	 * LOGGING STUFF
	\**************************************************************************************************/
	private static String log_file = "[null]";
	private static boolean logFileOpen = false;
	private static PrintWriter log;
	public static void openLogFile() {
		if(!logFileOpen) {
			//make dir if it doesn't exist
			//File logsDir = new File("../../logs"); //put in /cai/logs
			
			//if(logsDir.exists() && logsDir.isDirectory()) {
			//	//good
			//} else {
			//	logsDir.mkdir();
			//}
			
			String f = "log.txt";

			//if the file exists, delete it so we can start clean
			File check = new File(f);
			if(check.exists()) { check.delete(); }

			//String f = "../../logs/" + sessionID + "-" + getTimeStampForFileName() + ".log";
			try {
				log = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
				log_file = f;
				logFileOpen = true;
			} catch(IOException e) {
				log("openLogFile(): IOException opening log file " + f + ":" + e.getMessage());
			}
		} else {
			log("openLogFile(): a log file is already opened!");
		}
	} //end openLogFile()
	
	public static void writeLogFile(String msg) {
		if(logFileOpen) {
			log.println(msg);
			log.flush();
		} else {
			//no log file is open!
		}
	} //end writeLogFile()
	
	public static void closeLogFile() {
		if(logFileOpen) {
			log.close();
			logFileOpen = false;
			log("closeLogFile(): log file " + log_file + " closed");
			log_file = "[null]";
		}
	} //end closeLogFile()
	
	public static void log(String msg) {
		log(msg, 0);
		//System.out.println(getTimeStamp() + " " + msg);
	}
	
	public static void log(String msg, int level) {
		String l = "";
		switch(level) {
		case 0: l = " "; break;
		case 1: l = " [V1] "; break;
		case 2: l = " [V2] "; break;
		case 3: l = " [V3] "; break; 
		}
		if(log_level >= level) {
			System.out.println(getTimeStamp() + l + msg);
		}
		if(logFileOpen) writeLogFile(getTimeStampForLogging() + l + msg);
		//if(iDebug) iPause();
	} //end log()
	
	public static void logStackTrace(Exception e) {
		StringWriter stack = new StringWriter();
		e.printStackTrace(new PrintWriter(stack));
		log("[stack]: " + stack.toString());
	}

	public static String getStackTrace(Exception e) {
		StringWriter stack = new StringWriter();
		e.printStackTrace(new PrintWriter(stack));
		return stack.toString();
	}
	
	// [hh:mm:ss]
	public static String getTimeStamp() {
		Calendar c = Calendar.getInstance();
		return "[" + (c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + c.get(Calendar.HOUR_OF_DAY) : c.get(Calendar.HOUR_OF_DAY)).toString() + 
				":" + (c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE)).toString() + 
				":" + (c.get(Calendar.SECOND) < 10 ? "0" + c.get(Calendar.SECOND) : c.get(Calendar.SECOND)).toString() + "]"; 
	}
	
	// hhmmss
	public static String getTimeStampForFileName() {
		Calendar c = Calendar.getInstance();
		
		return (c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + c.get(Calendar.HOUR_OF_DAY) : c.get(Calendar.HOUR_OF_DAY)).toString() + 
			   (c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE)).toString() + 
			   (c.get(Calendar.SECOND) < 10 ? "0" + c.get(Calendar.SECOND) : c.get(Calendar.SECOND)).toString();
	}

	// [mm-dd-yyyy hh:mm:ss]
	public static String getTimeStampForLogging() {
		Calendar c = Calendar.getInstance();
		return "[" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.YEAR) + " " + 
			(c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + c.get(Calendar.HOUR_OF_DAY) : c.get(Calendar.HOUR_OF_DAY)).toString() + 
			":" + (c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE)).toString() + 
			":" + (c.get(Calendar.SECOND) < 10 ? "0" + c.get(Calendar.SECOND) : c.get(Calendar.SECOND)).toString() + "]";
	}

	// yyyy-mm-dd
	public static String getTimeStampForReportFilename() {
		Calendar c = Calendar.getInstance();
		return c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);
	}

	public static long getUNIXTimestamp() {
		return Calendar.getInstance().getTimeInMillis() / 1000;
	}

	public static String getDateString() {
		Calendar c = Calendar.getInstance();
		
		String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
		int hour = (c.get(Calendar.HOUR) == 0 ? 12 : c.get(Calendar.HOUR));

		return months[c.get(Calendar.MONTH)] + " " + c.get(Calendar.DAY_OF_MONTH) + ", " + c.get(Calendar.YEAR) + " " + 
			hour + ":" + (c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE)).toString() +
			(c.get(Calendar.AM_PM) == Calendar.AM ? "am" : "pm") + " " +
			TimeZone.getDefault().getDisplayName(TimeZone.getDefault().inDaylightTime(new Date()), TimeZone.SHORT);
	}

	//==============================================
	// Utility functions
	//==============================================
	//random number generator -- range: [0, max)
	public static int rand(int max) { return (int)(Math.random() * max); }
	public static double randDouble(double max) { return (Math.random() * max); }

	//string join function as in PHP
	// (http://stackoverflow.com/questions/1515437/java-function-for-arrays-like-phps-join)
	public static String join(String r[],String d) {
		if (r.length == 0) return "";
	    StringBuilder sb = new StringBuilder();
	    int i;
	    for(i=0;i<r.length-1;i++)
	    	sb.append(r[i]+d);
	    return sb.toString()+r[i];
	}

	public static String join(ArrayList<String> r, String d) {
		if(r.size() == 0) return "";

		StringBuilder sb = new StringBuilder();
		int i;
		for(i = 0; i < r.size() - 1; i++) {
			sb.append(r.get(i) + d);
		}

		return sb.toString() + r.get(i);
	}
} //end class App
