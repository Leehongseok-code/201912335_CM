import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.manager.CMDBManager;
import kr.ac.konkuk.ccslab.cm.stub.*;

public class CMServerEventHandler implements CMAppEventHandler {
    private CMServerStub m_serverStub;

    public CMServerEventHandler(CMServerStub serverStub)
    {
        m_serverStub = serverStub;
    }

    public void processEvent(CMEvent cme)
    {
        switch(cme.getType())
        {
            case CMInfo.CM_SESSION_EVENT:
                processSessionEvent(cme);
                break;
            case CMInfo.CM_INTEREST_EVENT:
                processInterestEvent(cme);
                break;
            default:
                return;
        }
    }

    private void processSessionEvent(CMEvent cme)
    {
        CMConfigurationInfo conInfo =
                m_serverStub.getCMInfo().getConfigurationInfo();

        CMSessionEvent se = (CMSessionEvent) cme;
        switch(se.getID())
        {
            case CMSessionEvent.LOGIN:
                System.out.println("["+se.getUserName()+"] requests login." );
                if(conInfo.isLoginScheme())
                {
                    boolean ret =
                            CMDBManager.authenticateUser(se.getUserName(), se.getPassword(),
                                    m_serverStub.getCMInfo());
                    if(!ret)
                    {
                        System.out.println("[" + se.getUserName() + "] authentication fails!");
                        m_serverStub.replyEvent(se,0);
                    }
                    else
                    {
                        System.out.println("[" + se.getUserName() + "] authentication succeeded.");
                        m_serverStub.replyEvent(se, 1);
                    }
                }
                break;

            case CMSessionEvent.LOGOUT:
                System.out.println("["+se.getUserName()+"] logs out.");
                break;


            default:
                return;
        }
    }

    private void processInterestEvent(CMEvent cme)
    {
        CMInterestEvent ie = (CMInterestEvent) cme;
        switch(ie.getID())
        {
            case CMInterestEvent.USER_ENTER:
                System.out.println("["+ie.getUserName()+"] enters group("+ie.getCurrentGroup()+") in session("
                        +ie.getHandlerSession()+").");
                break;
            case CMInterestEvent.USER_LEAVE:
                System.out.println("["+ie.getUserName()+"] leaves group("+ie.getHandlerGroup()+") in session("
                        +ie.getHandlerSession()+").");
                break;
            default:
                return;
        }
    }

}
