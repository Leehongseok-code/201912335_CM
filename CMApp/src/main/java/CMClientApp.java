import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.stub.*;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

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

        bRequestResult = m_clientStub.loginCM(strUserName, strPassword);
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

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        CMClientApp client = new CMClientApp();
        CMClientStub clientStub = client.getClientStub();
        CMClientEventHandler eventHandler = client.getClientEventHandler();
        boolean ret = false;

        //initialize CM
        clientStub.setAppEventHandler(eventHandler);
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
