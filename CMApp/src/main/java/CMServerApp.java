import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.stub.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;

public class CMServerApp {
    private CMServerStub m_serverStub;
    private CMServerEventHandler m_eventHandler;

    public CMServerApp()
    {
        m_serverStub = new CMServerStub();
        m_eventHandler = new CMServerEventHandler(m_serverStub);
    }

    public CMServerStub getServerStub(){
        return m_serverStub;
    }

    public CMServerEventHandler getServerEventHandler()
    {
        return m_eventHandler;
    }

    public static void main(String[] args)
    {

        CMServerApp server = new CMServerApp();
        CMServerStub cmStub = server.getServerStub();
        cmStub.setAppEventHandler(server.getServerEventHandler());


        // set config home
        cmStub.setConfigurationHome(Paths.get("."));
        // set file-path home
        cmStub.setTransferedFileHome(cmStub.getConfigurationHome().resolve("server-file-path"));

        cmStub.startCM();
    }
}
