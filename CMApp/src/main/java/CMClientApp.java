import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.stub.*;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;
import kr.ac.konkuk.ccslab.cm.manager.*;


public class CMClientApp {
    private CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;

    public CMClientApp()
    {
        m_clientStub = new CMClientStub();
        m_eventHandler = new CMClientEventHandler(m_clientStub);
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
