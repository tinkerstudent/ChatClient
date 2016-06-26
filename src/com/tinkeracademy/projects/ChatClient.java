package com.tinkeracademy.projects;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.tinkeracademy.projects.ChatService.ChatStatus;

public class ChatClient implements ActionListener, Runnable {

	public JButton button;
	
	public JTextArea chatHistory;
	
	public JTextArea chatMessage;
	
	ChatService chatService;
	
	ChatStatus initStatus = ChatStatus.NO;
	
	public ChatClient() {
		chatService = new ChatService(this);
	}
	
	public static void main(String[] args) {
		ChatClient chatClient = new ChatClient();
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				chatClient.createAndShowGUI();
			}
		});
	}
	
	public void update() {
		SwingUtilities.invokeLater(this);
	}

	@Override
	public void run() {
		updateChatHistory();
	}
	
	public void createAndShowGUI() {
		Window.show();
		Window.addLabel("Chat Application Developed By: Tinker Academy v1.0");
		chatHistory = Window.addTextArea("", 13, 10, false);
		chatMessage = Window.addTextArea("<Enter Your Name>", 4, 10, true);
		button = Window.addButton("Send");
		button.addActionListener(this);
		initStatus = chatService.initializeChatFile();
		if (initStatus == ChatStatus.ERROR) {
			showError();
		} else {
			initStatus = chatService.initializeUserFile();
			if (initStatus == ChatStatus.ERROR) {
				showError();
			} else if (initStatus == ChatStatus.YES) {
				updateChatHistory();
				clearChatMessage();
			} else if (initStatus == ChatStatus.NO) {
				promptChatUser();
			}
		}
		chatService.startScheduler();
	}
	
	public void updateChatHistory() {
		List<String> history = chatService.getChatHistory();
		String entries = null;
		for (String entry : history) {
			if (entries == null) {
				entries = entry;
			} else {
				entries = entries + "\n" + entry;
			}
		}
		chatHistory.setText(entries);
	}
	
	public void clearChatMessage() {
		chatMessage.setText("");
	}
	
	public void promptChatUser() {
		chatMessage.setText(getUserNamePrompt());
	}
	
	public void promptChatMessage() {
		chatMessage.setText(getChatPrompt());
	}
	
	public String getUserNamePrompt() {
		String txt = "<Enter Your Name>";
		return txt;
	}
	
	public String getChatPrompt() {
		String txt = "<Type in Chat Message>";
		return txt;
	}
	
	public void showError() {
		chatMessage.setText(getErrorText());
	}
	
	public String getErrorText() {
		String txt = "Oops! Error! Call Super.., no wait, Call Bat.., no wait, just Call me!";
		return txt;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == button) {
			doMessageAction();
		}
	}
	
	public void doMessageAction() {
		if (initStatus == ChatStatus.NO) {
			String chatName = chatMessage.getText();
			System.out.println("Your name is " + chatName);
			ChatStatus chatUserStatus = chatService.initializeChatUser(chatName);
			if (chatUserStatus == ChatStatus.YES) {
				initStatus = ChatStatus.YES;
				promptChatMessage();
			}
		} else if (initStatus == ChatStatus.YES) {
			String chatText = chatMessage.getText();
			System.out.println(chatText);
			chatService.updateChatFileLine(chatText);
			updateChatHistory();
			clearChatMessage();
		}
	}

}