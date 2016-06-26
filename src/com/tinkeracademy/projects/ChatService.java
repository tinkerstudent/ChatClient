package com.tinkeracademy.projects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

public class ChatService implements Runnable {

	public File userFile;
	
	public File chatFile;
	
	public String chatUser;
	
	public List<String> pendingChats = new ArrayList<String>();
	
	public ScheduledFuture<?> futureTask;
	
	public static final String URL = "http://tinkercloud-1273.appspot.com/chat?channel=spring2016";
	
	public enum ChatStatus {
		YES,
		NO,
		ERROR
	}
	
	public void run() {
		updateServer();
	}
	
	public void startScheduler() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		futureTask = scheduler.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
	}
	
	public ChatStatus initializeUserFile() {
		String userHomeDirectory = System.getProperty("user.home");
		try {
			userFile = new File(userHomeDirectory, "TinkerAcademyUser.tff");
			if (!userFile.exists()) {
				userFile.createNewFile();
				return ChatStatus.NO;
			} else if (userFile.length() == 0) {
				return ChatStatus.NO;
			}
			chatUser = getChatUser();
			if (chatUser == null) {
				return ChatStatus.ERROR;
			}
			return ChatStatus.YES;
		} catch(IOException e) {
			e.printStackTrace();
		}
		return ChatStatus.ERROR;
	}
	
	public ChatStatus initializeChatFile() {
		String userHomeDirectory = System.getProperty("user.home");
		try {
			chatFile = new File(userHomeDirectory, "TinkerAcademyChat.tff");
			if (!chatFile.exists()) {
				chatFile.createNewFile();
				return ChatStatus.NO;
			} else if (chatFile.length() == 0) {
				return ChatStatus.NO;
			}
			return ChatStatus.YES;
		} catch(IOException e) {
			e.printStackTrace();
		}
		return ChatStatus.ERROR;
	}
	
	public ChatStatus initializeChatUser(String userName) {
		ChatStatus chatStatus =  updateUserFileLine(userName);
		if (chatStatus == ChatStatus.YES) {
			chatUser = userName;
		}
		return chatStatus;
	}
	
	public ChatStatus updateChatFileLine(String line) {
		PrintWriter pw = null;
		try {		
			pw = new PrintWriter(new FileWriter(chatFile, true));
			long time = System.currentTimeMillis();
			line = time + "=" + chatUser + ": " + line; 
			pw.println(line);
			pw.flush();
			pendingChats.add(line);
			return ChatStatus.YES;
		} catch(IOException e) {
			System.out.println("Oops:"+e);
			return ChatStatus.ERROR;
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}
	
	public ChatStatus updateUserFileLine(String line) {
		PrintWriter pw = null;
		try {		
			pw = new PrintWriter(new FileWriter(userFile, true));
			pw.println(line);
			pw.flush();
			return ChatStatus.YES;
		} catch(IOException e) {
			System.out.println("Oops:"+e);
			return ChatStatus.ERROR;
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}
	
	public String getChatUser() {
		try {
			FileReader fr = new FileReader(userFile);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			br.close();
			return line;
		} catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public List<String> getChatHistory() {
		List<String> history = null;
		FileReader fr = null;
		try {
			fr = new FileReader(chatFile);
			history = readLines(fr);
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
				
			}
		}
		return history;
	}
	
	public void updateServer() {
		String line = null;
		if (!pendingChats.isEmpty()) {
			line = pendingChats.remove(0);
		}
		if (line == null) {
			getFromServer();
		} else {
			postToServer(line);
		}
	}
	
	public void getFromServer() {
		CloseableHttpClient client = null;
		HttpGet get = new HttpGet(URL);
		CloseableHttpResponse response = null;
		try {
			client = HttpClients.createDefault();
			response = client.execute(get);
			updateLocalChatFileFromServer(response);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
				if (client != null) {
					client.close();
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void postToServer(String line) {
		CloseableHttpClient client = null;
		CloseableHttpResponse response = null;
		HttpPost post = new HttpPost(URL);
		try {
			client = HttpClients.createDefault();
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add( new BasicNameValuePair( "chat", line ));
			UrlEncodedFormEntity paramEntity = new UrlEncodedFormEntity( params );
			post.setEntity(paramEntity);
			response = client.execute(post);
			updateLocalChatFileFromServer(response);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
				if (client != null) {
					client.close();
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void updateLocalChatFileFromServer(HttpResponse response) {
		InputStream inputStream = null;
		try {
			inputStream = response.getEntity().getContent();
			InputStreamReader reader = new InputStreamReader(inputStream);
			List<String> lines = readLines(reader);
			FileWriter fw = new FileWriter(chatFile, false);
			writeLines(fw, lines);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeLines(Writer writer, List<String> lines) {
		PrintWriter pw = new PrintWriter(writer);
		try {
			for (String line : lines) {
				pw.println(line);
			}
		} finally {
			pw.close();
		}
	}
	
	public List<String> readLines(Reader reader) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader bufferedReader = new BufferedReader(reader);
		try {
			String line = bufferedReader.readLine();
			for (;line != null;) {
				int index = line.indexOf("=");
				if (index != -1) {
					line = line.substring(index+1, line.length());
				}
				lines.add(line);
				line = bufferedReader.readLine();
			}
		} finally {
			try {
			bufferedReader.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		return lines;
	}
	
}