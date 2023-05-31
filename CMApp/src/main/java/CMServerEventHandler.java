import java.io.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.*;
import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.manager.CMDBManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.stub.*;


public class CMServerEventHandler implements CMAppEventHandler {
    private CMServerStub m_serverStub;
    private boolean m_bDistFileProc;	// for distributed file processing

    HashMap<String, HashMap<String, String>> shareMap; //<파일명, 공유중안 사용자> 쌍

    public CMServerEventHandler(CMServerStub serverStub)
    {
        m_serverStub = serverStub;
        shareMap = new HashMap<String, HashMap<String, String>>();//{보내는사람: {파일이름:공유받을사람}}

    }


    public void processEvent(CMEvent cme)
    {
        switch (cme.getType()) {
            case CMInfo.CM_SESSION_EVENT -> processSessionEvent(cme);
            case CMInfo.CM_INTEREST_EVENT -> processInterestEvent(cme);
            case CMInfo.CM_FILE_EVENT -> processFileEvent(cme);
            case CMInfo.CM_DUMMY_EVENT -> {
                if(cme.getID() == 105)
                    processSyncEvent(cme);
                else if(cme.getID() == 106)
                    processShareEvent(cme);
            }
            default -> {
                return;
            }
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

    private void processFileEvent(CMEvent cme)
    {
        CMFileEvent fe = (CMFileEvent) cme;


        switch(fe.getID())
        {
            case CMFileEvent.REQUEST_PERMIT_PULL_FILE:
                System.out.println("["+fe.getFileReceiver()+"] requests file("+fe.getFileName()+").");
                System.err.print("["+fe.getFileReceiver()+"] requests file("+fe.getFileName()+").\n");
                System.err.print("The pull-file request is not automatically permitted!\n");
                System.err.print("To change to automatically permit the pull-file request, \n");
                System.err.print("set the PERMIT_FILE_TRANSFER field to 1 in the cm-server.conf file\n");
                break;
            case CMFileEvent.REPLY_PERMIT_PULL_FILE:
                if(fe.getReturnCode() == -1)
                {
                    System.err.print("["+fe.getFileName()+"] does not exist in the owner!\n");
                }
                else if(fe.getReturnCode() == 0)
                {
                    System.err.print("["+fe.getFileSender()+"] rejects to send file("
                            +fe.getFileName()+").\n");
                }
                break;
            case CMFileEvent.REQUEST_PERMIT_PUSH_FILE:
                System.out.println("["+fe.getFileSender()+"] wants to send a file("+fe.getFilePath()+
                        ").");
                System.err.print("The push-file request is not automatically permitted!\n");
                System.err.print("To change to automatically permit the push-file request, \n");
                System.err.print("set the PERMIT_FILE_TRANSFER field to 1 in the cm-server.conf file\n");
                break;
            case CMFileEvent.REPLY_PERMIT_PUSH_FILE:
                if(fe.getReturnCode() == 0)
                {
                    System.err.print("["+fe.getFileReceiver()+"] rejected the push-file request!\n");
                    System.err.print("file path("+fe.getFilePath()+"), size("+fe.getFileSize()+").\n");
                }
                break;
            case CMFileEvent.START_FILE_TRANSFER:
            case CMFileEvent.START_FILE_TRANSFER_CHAN:
                System.out.println("["+fe.getFileSender()+"] is about to send file("+fe.getFileName()+").");
                break;
            case CMFileEvent.END_FILE_TRANSFER:
            case CMFileEvent.END_FILE_TRANSFER_CHAN:
                System.out.println("["+fe.getFileSender()+"] completes to send file("+fe.getFileName()+", "
                        +fe.getFileSize()+" Bytes).");
                String strFile = fe.getFileName();
                if(m_bDistFileProc)
                {
                    processFile(fe.getFileSender(), strFile);
                    m_bDistFileProc = false;
                }
                break;
            case CMFileEvent.REQUEST_DIST_FILE_PROC:
                System.out.println("["+fe.getFileReceiver()+"] requests the distributed file processing.");
                m_bDistFileProc = true;
                break;
            case CMFileEvent.CANCEL_FILE_SEND:
            case CMFileEvent.CANCEL_FILE_SEND_CHAN:
                System.out.println("["+fe.getFileSender()+"] cancelled the file transfer.");
                break;
            case CMFileEvent.CANCEL_FILE_RECV_CHAN:
                System.out.println("["+fe.getFileReceiver()+"] cancelled the file request.");
                break;
        }
        return;
    }

    private void processFile(String strSender, String strFile)
    {
        CMConfigurationInfo confInfo = m_serverStub.getCMInfo().getConfigurationInfo();
        String strFullSrcFilePath = null;
        String strModifiedFile = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        byte[] fileBlock = new byte[CMInfo.FILE_BLOCK_LEN];

        long lStartTime = System.currentTimeMillis();

        // change the modified file name
        strModifiedFile = "m-"+strFile;
        strModifiedFile = confInfo.getTransferedFileHome().toString()+File.separator+strSender+
                File.separator+strModifiedFile;

        // stylize the file
        strFullSrcFilePath = confInfo.getTransferedFileHome().toString()+File.separator+strSender+
                File.separator+strFile;
        File srcFile = new File(strFullSrcFilePath);
        long lFileSize = srcFile.length();
        long lRemainBytes = lFileSize;
        int readBytes = 0;

        try {
            fis = new FileInputStream(strFullSrcFilePath);
            fos = new FileOutputStream(strModifiedFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        try {

            while( lRemainBytes > 0 )
            {
                if( lRemainBytes >= CMInfo.FILE_BLOCK_LEN )
                {
                    readBytes = fis.read(fileBlock);
                }
                else
                {
                    readBytes = fis.read(fileBlock, 0, (int)lRemainBytes);
                }

                fos.write(fileBlock, 0, readBytes);
                lRemainBytes -= readBytes;
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // add some process delay here
        for(long i = 0; i < lFileSize/50; i++)
        {
            for(long j = 0; j < lFileSize/50; j++)
            {
                //
            }
        }

        long lEndTime = System.currentTimeMillis();
        System.out.println("processing delay: "+(lEndTime-lStartTime)+" ms");

        // send the modified file to the sender
        CMFileTransferManager.pushFile(strModifiedFile, strSender, m_serverStub.getCMInfo());

        return;
    }

    private void processSyncEvent(CMEvent cme)
    {
        CMDummyEvent due = (CMDummyEvent) cme;
        String sender = due.getSender();

        String[] strServerFiles = null;
        String[] absServerFiles = null;
        String fileServerDir = null;
        String strServerFileList = null;
        int nServerFileNum = -1;


        fileServerDir = "./server-file-path/" + sender;
        //서버의 디렉토리에 있는 파일 리스트 불러오기
        File serverDir = new File(fileServerDir);
        strServerFiles = serverDir.list();

        nServerFileNum = strServerFiles.length;


        //클라이언트에서 받은 파일 리스트 쪼개서 불러오기
        String strClientFileList = null;
        String[] strClientFiles = null;
        int nClienFiletNum = -1;

        strClientFileList = due.getDummyInfo();
        strClientFileList.trim();
        strClientFiles = strClientFileList.split(" ");


        List<String> strClientList = new ArrayList<>(Arrays.asList(strClientFiles));
        for(int i = 0; i < nServerFileNum; i++)
        {
            if(strClientList.contains(strServerFiles[i])==false)
            {
                //동기화 요청한 client에서 삭제된 파일은 서버에서도 삭제
                String absServerFile = fileServerDir + "/" + strServerFiles[i];
                System.out.println("delete: " + absServerFile);
                File file = new File(absServerFile);
                file.delete();
            }
            else
            {
                //추후에 수정된 파일만 지우고 다시보내게끔 elseif로 고치면 됨!
                String absServerFile = fileServerDir + "/" + strServerFiles[i];
                System.out.println("delete: " + absServerFile);
                File file = new File(absServerFile);
                file.delete();
            }
        }

        return ;
    }

    private void processShareEvent(CMEvent cme)
    {
        String fileName = null;
        String strTarget = null;
        String strFile = null;

        CMDummyEvent due = (CMDummyEvent) cme;
        String sender = due.getSender();
        System.out.println(due.getDummyInfo());
        String[] commands = due.getDummyInfo().split(" ");
        fileName = commands[0];
        strTarget = commands[1];

        //파일을 전송하기 전에 전송받을 파일명을 클라이언트에게 미리 알려, 업데이트된 파일을 지우도록 지시(나중에 더미메시지에 업데이트날짜도 실어보내야됨)
        CMDummyEvent newDue = new CMDummyEvent();
        newDue.setID(106);
        newDue.setDummyInfo(fileName);
        m_serverStub.send(newDue, strTarget);

        strFile = "./server-file-path/" + sender + "/" + fileName;//
        System.out.println(strFile);

        boolean bReturn = false;
        bReturn = CMFileTransferManager.pushFile(strFile, strTarget, m_serverStub.getCMInfo());
        //bReturn = m_serverStub.pushFile(strFile, strTarget, CMInfo.FILE_OVERWRITE);
        if(!bReturn)
            System.err.println("Request file error!");
        else
            System.out.println("file success");
        return ;
    }

    void DeleteFile(String strFile)
    {
        String fileClientDir = "./server-file-path";
        String absServerFile = fileClientDir + "/" + strFile;
        System.out.println("delete: " + absServerFile);
        File file = new File(absServerFile);
        file.delete();
    }

}
