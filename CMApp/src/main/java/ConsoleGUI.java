import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import javax.swing.text.*;

public class ConsoleGUI extends JFrame {
    //UI관련 멤버 변수들
    private JTextPane consoleTextPane;
    private JTextArea inputTextArea;
    private PrintStream consolePrintStream;


    //실제 ClientApp용 멤버 변수들
    private CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;

    long lastSyncTime = -1;

    HashMap<String, String> shareMap;



    public ConsoleGUI() {
        setTitle("Console");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        consoleTextPane = new JTextPane();
        consoleTextPane.setEditable(false);

        // JTextPane에 스크롤 기능 추가
        JScrollPane consoleScrollPane = new JScrollPane(consoleTextPane);

        inputTextArea = new JTextArea();
        JScrollPane inputScrollPane = new JScrollPane(inputTextArea);
        inputTextArea.setRows(2);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String input = inputTextArea.getText();
                System.out.println("> " + input);
                processInput(input);
                inputTextArea.setText("");
            }
        });


        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(consoleScrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 콘솔 출력을 JTextPane으로 재지정


        consolePrintStream = new PrintStream(new ConsoleOutputStream(consoleTextPane));
        System.setOut(consolePrintStream);
        //System.setErr(consolePrintStream);

        pack();
        setVisible(true);


        //실질적 기능을 하는 부분
        m_clientStub = new CMClientStub();
        m_eventHandler = new CMClientEventHandler(m_clientStub);
        shareMap = new HashMap<String, String>(); //<파일명, 유저명>
    }

    public CMClientStub getClientStub()
    {
        return m_clientStub;
    }

    public CMClientEventHandler getClientEventHandler()
    {
        return m_eventHandler;
    }

    public void Login()
    {
        String strUserName = null;
        String strPassword = null;
        boolean bRequestResult = false;
        Console console = System.console();
        if(console == null)
        {
            System.err.println("Unable to obtain console.");
        }

        System.out.println("====== login to default server");


        //////
        JTextField userNameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        Object[] message = {
                "User Name:", userNameField,
                "Password:", passwordField
        };
        int option = JOptionPane.showConfirmDialog(null, message, "Login Input", JOptionPane.OK_CANCEL_OPTION);
        //////

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        if (option == JOptionPane.OK_OPTION)
        {
            strUserName = userNameField.getText();
            strPassword = new String(passwordField.getPassword());


            bRequestResult = m_clientStub.loginCM(strUserName, strPassword); //입력받은 아이디, 비밀번호로 default서버에 로그인한다.
            if (bRequestResult)
            {
                System.out.print("user name: " + strUserName);
                System.out.println("successfully sent the login request.");
            }
            else
                System.err.println("failed the login request!");

        }
        System.out.println("======");

    }

    public void Logout()
    {
        boolean bRequestResult = false;
        System.out.println("====== logout from default server");
        bRequestResult = m_clientStub.logoutCM();
        if(bRequestResult)
            System.out.println("successfully sent the logout request.");
        else
            System.err.println("failed the logout request!");
        System.out.println("======");
    }

    public void SetFilePath()
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== set file path");
        String strPath = null;
        System.out.print("file path: ");
        try {
            strPath = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        m_clientStub.setTransferedFileHome(Paths.get(strPath));

        System.out.println("======");
    }

    public void RequestFile()
    {
        boolean bReturn = false;
        String strFileName = null;
        String strFileOwner = null;
        String strFileAppend = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== request a file");
        try {
            System.out.print("File name: ");
            strFileName = br.readLine();
            System.out.print("File owner(enter for \"SERVER\"): ");
            strFileOwner = br.readLine();
            if(strFileOwner.isEmpty())
                strFileOwner = m_clientStub.getDefaultServerName();
            System.out.print("File append mode('y'(append);'n'(overwrite);''(empty for the default configuration): ");
            strFileAppend = br.readLine();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if(strFileAppend.isEmpty())
            bReturn = m_clientStub.requestFile(strFileName, strFileOwner);
        else if(strFileAppend.equals("y"))
            bReturn = m_clientStub.requestFile(strFileName,  strFileOwner, CMInfo.FILE_APPEND);
        else if(strFileAppend.equals("n"))
            bReturn = m_clientStub.requestFile(strFileName,  strFileOwner, CMInfo.FILE_OVERWRITE);
        else
            System.err.println("wrong input for the file append mode!");

        if(!bReturn)
            System.err.println("Request file error! file("+strFileName+"), owner("+strFileOwner+").");

        System.out.println("======");
    }


    public void PushFile()
    {
        //GUI 관련 선언
        JTextField filePathField= new JTextField();

        Object[] message = {
                "File Path:", filePathField
        };
        int option = JOptionPane.showConfirmDialog(null, message, "File Push", JOptionPane.OK_CANCEL_OPTION);


        //기능 관련 선언
        String strFilePath = null;
        String strReceiver = null;
        String strFileAppend = null;
        boolean bReturn = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== push a file");


        strFilePath = filePathField.getText();;
        strReceiver = m_clientStub.getDefaultServerName();


        bReturn = m_clientStub.pushFile(strFilePath, strReceiver);
        if(!bReturn)
            System.err.println("Push file error! file("+strFilePath+"), receiver("+strReceiver+")");
        else
            System.out.println("File transfer success!");

        System.out.println("======");
    }

    public void SendMultipleFiles()
    {
        //GUI 관련 선언
        JTextField nFileNumField= new JTextField();
        JTextField filePathField= new JTextField();

        Object[] message = {
                "nFileNum:", nFileNumField,
                "File Path:", filePathField
        };
        int option = JOptionPane.showConfirmDialog(null, message, "File Push", JOptionPane.OK_CANCEL_OPTION);



        boolean bReturn = false;
        String[] strFiles = null;
        String strFileList = null;
        int nFileNum = -1;
        String strTarget = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== pull/push multiple files");
        strTarget = m_clientStub.getDefaultServerName();


        nFileNum = Integer.parseInt(nFileNumField.getText());
        strFileList = filePathField.getText();
        /*
        try {
            System.out.print("Input receiver name(empty for default): ");
            strTarget = br.readLine();
            if(strTarget.isEmpty())
                strTarget = m_clientStub.getDefaultServerName();

            System.out.print("Number of files: ");
            nFileNum = Integer.parseInt(br.readLine());
            System.out.print("Input file names separated with space: ");
            strFileList = br.readLine();

        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
         */


        strFileList.trim();
        strFiles = strFileList.split("\\s+");
        if(strFiles.length != nFileNum)
        {
            System.out.println("The number of files incorrect!");
            return;
        }

        for(int i = 0; i < nFileNum; i++)
        {
            bReturn = CMFileTransferManager.pushFile(strFiles[i], strTarget, m_clientStub.getCMInfo());
            if(!bReturn)
                System.err.println("Request file error! file("+strFiles[i]+"), owner("+strTarget+").");
            else
                System.out.println(Integer.toString(i + 1) + "/" + Integer.toString(nFileNum) + " success");
        }

        System.out.println("======");
        return;
    }

    public void Synchronization()
    {
        //GUI 관련 선언
        JTextField fileDirField= new JTextField();

        Object[] message = {
                "Directory Path:", fileDirField
        };
        int option = JOptionPane.showConfirmDialog(null, message, "Synchronization", JOptionPane.OK_CANCEL_OPTION);

        boolean bReturn = false;
        String[] strFiles = null;
        String[] absFiles = null;

        String fileDir = null;
        String strFileList = null;
        int nFileNum = -1;
        String strTarget = null;


        strTarget = m_clientStub.getDefaultServerName();

        fileDir = fileDirField.getText();

        File dir = new File(fileDir);
        strFiles = dir.list();//파일 이름만을 저장한 리스트
        nFileNum = strFiles.length;
        //absFiles = strFiles.clone();//절대경로 저장할 리스트를 따로 분리
        absFiles = new String[nFileNum];


        System.out.println("====== Synchronize files");

        for(int i = 0; i < nFileNum; i++)
        {
            String tempAbsFile = fileDir + '/' + strFiles[i];
            File tempFile = new File(tempAbsFile);
            if(lastSyncTime < tempFile.lastModified())
            {
                absFiles[i] = tempAbsFile;
                System.out.println(strFiles[i] + " update detected");
            }
            else
            {
                System.out.println(strFiles[i] + " was not updated");
            }
        }

        System.out.println("Number of files: " + nFileNum);


        //동기화 메시지를 먼저 보내기
        CMDummyEvent due = new CMDummyEvent();
        due.setID(105);
        due.setDummyInfo(String.join(" ", strFiles));//동기화시킬 파일들의 목록을 공백으로 구분하여 전송
        m_clientStub.send(due,m_clientStub.getDefaultServerName());

        if(strFiles.length != nFileNum)
        {
            System.out.println("The number of files incorrect!");
            return;
        }

        nFileNum = absFiles.length;

        for(int i = 0; i < nFileNum; i++)
        {
            if(absFiles[i] == null)
            {
                continue;
            }

            //서버랑 동기화 하는 부분
            bReturn = CMFileTransferManager.pushFile(absFiles[i], strTarget, m_clientStub.getCMInfo());
            if(!bReturn)
            {
                System.err.println("Request file error! file("+strFiles[i]+"), owner("+strTarget+").");
            }
            else
            {
                System.out.println(Integer.toString(i + 1) + "/" + Integer.toString(nFileNum) + " success");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //공유중인 사용자가 있다면 사용자에게 공유하는 이벤트 또한 전송
            String strReceiver = shareMap.get(strFiles[i]);
            if(strReceiver != null)
            {
                System.out.println("Sharing user exists: " + strReceiver);
                due = new CMDummyEvent();
                due.setID(106);
                due.setDummyInfo(strFiles[i] + " " + strReceiver);
                m_clientStub.send(due, m_clientStub.getDefaultServerName());
            }
            else
            {
                System.out.println("There is no user sharing file: " + strFiles[i]);
            }

        }

        //마지막으로 동기화 작업을 실행한 시간을 기록
        lastSyncTime = System.currentTimeMillis();
        System.out.println("======");

        return;
    }

    public void ShareMultipleFiles()
    {
        //GUI 관련 선언
        JTextField receiverField= new JTextField();
        JTextField nFileNumField= new JTextField();
        JTextField filePathField= new JTextField();

        Object[] message = {
                "receiverName:", receiverField,
                "nFileNum:", nFileNumField,
                "File Path:", filePathField
        };
        int option = JOptionPane.showConfirmDialog(null, message, "File Share", JOptionPane.OK_CANCEL_OPTION);


        boolean bReturn = false;
        String[] strFiles = null;
        String strFileList = null;
        int nFileNum = -1;
        String strTarget = null;//어차피 Server로 고정
        String strReceiver = null;//실제로 Server거쳐 받게 될 Client
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== pull/push multiple files");

        strTarget = m_clientStub.getDefaultServerName();//strTarget은 Server로 고정


        strReceiver = receiverField.getText();
        nFileNum = Integer.parseInt(nFileNumField.getText());
        strFileList = filePathField.getText();

        if(strReceiver.isEmpty())
        {
            System.err.println("Receiver is empty");
            return ;
        }


        strFileList.trim();
        strFiles = strFileList.split("\\s+");
        if(strFiles.length != nFileNum)
        {
            System.out.println("The number of files incorrect!");
            return;
        }

        for(int i = 0; i < nFileNum; i++)
        {
            String[] filePaths = strFiles[i].split("/");
            String fileName = filePaths[filePaths.length-1];//더미이벤트에 실어서 보낼 파일 이름

            bReturn = CMFileTransferManager.pushFile(strFiles[i], strTarget, m_clientStub.getCMInfo());
            //bReturn = m_clientStub.pushFile(strFiles[i], strTarget, CMInfo.FILE_OVERWRITE);
            if(!bReturn)
                System.err.println("Request file error! file("+strFiles[i]+"), owner("+strTarget+").");
            else
            {
                //성공했다고 출력
                System.out.println(Integer.toString(i + 1) + "/" + Integer.toString(nFileNum) + " success");

                //더미이벤트 보내기
                CMDummyEvent due = new CMDummyEvent();
                due.setID(106);
                due.setDummyInfo(fileName + " " + strReceiver);
                m_clientStub.send(due, m_clientStub.getDefaultServerName());

                //공유된 파일과 사용자명을 테이블에 저장
                shareMap.put(fileName, strReceiver);
                System.out.println("Successfully make link with " + strReceiver);
            }
        }

        System.out.println("======");
        return;
    }



    public static void main(String[] args) {

        ConsoleGUI client = new ConsoleGUI();
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run()
                    {
                    }
                }
        );

        // 예시 콘솔 출력
        System.out.println("Hello, world!");


        CMClientStub clientStub = client.getClientStub();
        CMClientEventHandler eventHandler = client.getClientEventHandler();
        boolean ret = false;

        //initialize CM
        clientStub.setAppEventHandler(eventHandler);
        // set config home
        clientStub.setConfigurationHome(Paths.get("."));
        // set file-path home
        clientStub.setTransferedFileHome(clientStub.getConfigurationHome().resolve("client-file-path"));


        ret = clientStub.startCM();

        if (ret)
            System.out.println("init success");
        else {
            System.err.println("init error!");
            return;
        }
    }

    public void processInput(String strInput) {
        System.out.println("Input: " + strInput);


        int nCommand = -1;
        try {
            nCommand = Integer.parseInt(strInput);
        } catch (NumberFormatException e) {
            System.out.println("Incorrect command number!\n");
            return;
        }

            switch (nCommand)
            {
                case 100:
                    //login
                    Login();
                    break;
                case 12:
                    Logout();
                    break;
                case 70:
                    SetFilePath();
                    break;
                case 71:
                    RequestFile();
                    break;
                case 72: // push a file
                    PushFile();
                    break;
                case 104: // pull or push multiple files
                    SendMultipleFiles();
                    break;
                case 105: //이상 탐지
                    Synchronization();
                    break;
                case 106:
                    ShareMultipleFiles();
                    break;
                default:
                    System.err.println("Unknown command.");
                    break;
            }


        }
}

// 콘솔 출력을 JTextPane으로 전달하기 위한 OutputStream
class ConsoleOutputStream extends OutputStream {
    private JTextPane textPane;
    private Document document;

    public ConsoleOutputStream(JTextPane textPane) {
        this.textPane = textPane;
        this.document = textPane.getDocument();
    }

    @Override
    public void write(int b) throws IOException {
        SwingUtilities.invokeLater(() -> {
            try {
                document.insertString(document.getLength(), String.valueOf((char) b), null);
                textPane.setCaretPosition(document.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }


}
