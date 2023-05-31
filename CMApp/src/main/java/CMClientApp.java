import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.stub.*;


import java.io.File;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.*;
import java.lang.Thread;
import java.util.HashMap;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;

import kr.ac.konkuk.ccslab.cm.manager.*;
import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;


public class CMClientApp {
    private CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;

    HashMap<String, String> shareMap;

    public CMClientApp()
    {
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
        System.out.print("user name: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            strUserName = br.readLine();
            if(console == null)
            {
                System.out.print("password: ");
                strPassword = br.readLine();
            }
            else
                strPassword = new String(console.readPassword("password: "));
        } catch (IOException e) {
            e.printStackTrace();
        }

        bRequestResult = m_clientStub.loginCM(strUserName, strPassword); //입력받은 아이디, 비밀번호로 default서버에 로그인한다.
        if(bRequestResult)
            System.out.println("successfully sent the login request.");
        else
            System.err.println("failed the login request!");
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
        String strFilePath = null;
        String strReceiver = null;
        String strFileAppend = null;
        boolean bReturn = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== push a file");

        try {
            System.out.print("File path name: ");
            strFilePath = br.readLine();
            System.out.print("File receiver (enter for \"SERVER\"): ");
            strReceiver = br.readLine();
            if(strReceiver.isEmpty())
                strReceiver = m_clientStub.getDefaultServerName();
            System.out.print("File append mode('y'(append);'n'(overwrite);''(empty for the default configuration): ");
            strFileAppend = br.readLine();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if(strFileAppend.isEmpty())
            bReturn = m_clientStub.pushFile(strFilePath, strReceiver);
        else if(strFileAppend.equals("y"))
            bReturn = m_clientStub.pushFile(strFilePath,  strReceiver, CMInfo.FILE_APPEND);
        else if(strFileAppend.equals("n"))
            bReturn = m_clientStub.pushFile(strFilePath,  strReceiver, CMInfo.FILE_OVERWRITE);
        else
            System.err.println("wrong input for the file append mode!");

        if(!bReturn)
            System.err.println("Push file error! file("+strFilePath+"), receiver("+strReceiver+")");
        else
            System.out.println("File transfer success!");

        System.out.println("======");
    }

    public void SendMultipleFiles()
    {
        boolean bReturn = false;
        String[] strFiles = null;
        String strFileList = null;
        int nFileNum = -1;
        String strTarget = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== pull/push multiple files");
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
        boolean bReturn = false;
        String[] strFiles = null;
        String[] absFiles = null;

        String fileDir = null;
        String strFileList = null;
        int nFileNum = -1;
        String strTarget = null;



        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== Synchronize files");
        try {
            System.out.print("Input receiver name(empty for default): ");
            strTarget = br.readLine();
            if(strTarget.isEmpty())
                strTarget = m_clientStub.getDefaultServerName();

            System.out.print("Path to synchronize: ");
            fileDir = br.readLine();//전송할 루트 폴더

            File dir = new File(fileDir);
            strFiles = dir.list();//파일 이름만을 저장한 리스트
            absFiles = strFiles.clone();//절대경로 저장할 리스트를 따로 분리
            nFileNum = strFiles.length;

            for(int i = 0; i < nFileNum; i++)
            {
                absFiles[i] = fileDir + '/' + strFiles[i];
                System.out.println(strFiles[i]);
            }

            System.out.println("Number of files: " + nFileNum);

        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

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

        for(int i = 0; i < nFileNum; i++)
        {
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

        System.out.println("======");


        return;
    }

    public void ShareMultipleFiles()
    {
        boolean bReturn = false;
        String[] strFiles = null;
        String strFileList = null;
        int nFileNum = -1;
        String strTarget = null;//어차피 Server로 고정
        String strReceiver = null;//실제로 Server거쳐 받게 될 Client
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("====== pull/push multiple files");
        try {
            System.out.print("Input receiver name: ");
            strTarget = m_clientStub.getDefaultServerName();//strTarget은 Server로 고정
            strReceiver = br.readLine();

            if(strReceiver.isEmpty())
            {
                System.err.println("Receiver is empty");
                return ;
            }

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

            //bReturn = CMFileTransferManager.pushFile(strFiles[i], strTarget, m_clientStub.getCMInfo());
            bReturn = m_clientStub.pushFile(strFiles[i], strTarget, CMInfo.FILE_OVERWRITE);
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
        Scanner scanner = new Scanner(System.in);
        CMClientApp client = new CMClientApp();
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

        ////

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String strInput = null;
        int nCommand = -1;


        while(true)
        {
            try {
                strInput = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            try {
                nCommand = Integer.parseInt(strInput);
            } catch (NumberFormatException e) {
                System.out.println("Incorrect command number!");
                continue;
            }

            switch (nCommand) {
                case 100:
                    //login
                    client.Login();
                    break;
                case 12:
                    client.Logout();
                    break;
                case 70:
                    client.SetFilePath();
                    break;
                case 71:
                    client.RequestFile();
                    break;
                case 72: // push a file
                    client.PushFile();
                    break;
                case 104: // pull or push multiple files
                    client.SendMultipleFiles();
                    break;
                case 105: //이상 탐지
                    client.Synchronization();
                    break;
                case 106:
                    client.ShareMultipleFiles();
                    break;
                default:
                    System.err.println("Unknown command.");
                    break;
            }
        }
        /*
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
         */

    }
}
