import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.stub.*;

public class CMClientEventHandler implements CMAppEventHandler {
    private CMClientStub m_clientStub;

    public CMClientEventHandler(CMClientStub clientStub)
    {
        m_clientStub = clientStub;
    }

    public void processEvent(CMEvent cme)
    {
        switch(cme.getType())
        {
            case CMInfo.CM_SESSION_EVENT:
                processSessionEvent(cme);
                break;
            case CMInfo.CM_DATA_EVENT:
                processDataEvent(cme);
            default:
                return;
        }
    }

    private void processSessionEvent(CMEvent cme)
    {
        CMSessionEvent se = (CMSessionEvent) cme;
        switch(se.getID())
        {
            case CMSessionEvent.LOGIN_ACK:
                if(se.isValidUser() == 0)
                {
                    System.err.println("This client fails authentication by the default server!");
                }
                else if(se.isValidUser() == -1)
                {
                    System.err.println("This client is already in the login-user list!");
                }
                else
                {
                    System.out.println("This client successfully logs in to the default server.");
                }
                break;
            default:
                return;
        }
    }

    private void processDataEvent(CMEvent cme)
    {
        CMDataEvent de = (CMDataEvent) cme;
        switch(de.getID())
        {
            case CMDataEvent.NEW_USER:
                System.out.println("["+de.getUserName()+"] enters group("+de.getHandlerGroup()+") in session("
                        +de.getHandlerSession()+").");
                break;
            case CMDataEvent.REMOVE_USER:
                System.out.println("["+de.getUserName()+"] leaves group("+de.getHandlerGroup()+") in session("
                        +de.getHandlerSession()+").");
                break;
            default:
                return;
        }
    }

}
