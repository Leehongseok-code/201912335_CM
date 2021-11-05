package kr.ac.konkuk.ccslab.cm.event.handler;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CMFileSyncEventHandler extends CMEventHandler {

    public CMFileSyncEventHandler(CMInfo cmInfo) {
        super(cmInfo);
        m_nType = CMInfo.CM_FILE_SYNC_EVENT_HANDLER;
    }

    @Override
    public boolean processEvent(CMEvent event) {
        boolean processResult;
        CMFileSyncEvent fse = (CMFileSyncEvent)event;
        int eventId = fse.getID();
        switch(eventId) {
            case CMFileSyncEvent.START_FILE_LIST:
                processResult = processSTART_FILE_LIST(fse);
                break;
            case CMFileSyncEvent.START_FILE_LIST_ACK:
                processResult = processSTART_FILE_LIST_ACK(fse);
                break;
            case CMFileSyncEvent.FILE_ENTRIES:
                processResult = processFILE_ENTRIES(fse);
                break;
            case CMFileSyncEvent.FILE_ENTRIES_ACK:
                processResult = processFILE_ENTRIES_ACK(fse);
                break;
            case CMFileSyncEvent.END_FILE_LIST:
                processResult = processEND_FILE_LIST(fse);
                break;
            case CMFileSyncEvent.END_FILE_LIST_ACK:
                processResult = processEND_FILE_LIST_ACK(fse);
                break;
            default:
                System.err.println("CMFileSyncEventHandler::processEvent(), invalid event id(" + eventId + ")!");
                return false;
        }

        return processResult;
    }

    // called at the server
    private boolean processSTART_FILE_LIST(CMFileSyncEvent fse) {

        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processSTART_FILE_LIST() called..");
            System.out.println("event = "+fse);
        }

        String userName = fse.getUserName();
        // get the file-sync manager
        CMFileSyncManager fsManager = (CMFileSyncManager) m_cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        // get server sync home for userName
        Path serverSyncHome = fsManager.getServerSyncHome(userName);
        // check and create the server sync home
        if(Files.notExists(serverSyncHome)) {
            try {
                Files.createDirectories(serverSyncHome);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // create the ack event
        CMFileSyncEvent ackFse = new CMFileSyncEvent();
        ackFse.setID(CMFileSyncEvent.START_FILE_LIST_ACK);
        ackFse.setSender( fse.getReceiver() );  // server name
        ackFse.setReceiver( userName );
        ackFse.setUserName( userName );
        ackFse.setNumTotalFiles( fse.getNumTotalFiles() );
        ackFse.setReturnCode(1);    // always success

        // send the ack event to the client

        return CMEventManager.unicastEvent(ackFse, userName, m_cmInfo);
    }

    // processed at the client
    private boolean processSTART_FILE_LIST_ACK(CMFileSyncEvent fse) {

        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processSTART_FILE_LIST_ACK() called..");
            System.out.println("event = "+fse);
        }

        String server = fse.getSender();

        // create a FILE_ENTRIES event
        CMFileSyncEvent newfse = new CMFileSyncEvent();
        newfse.setID(CMFileSyncEvent.FILE_ENTRIES);
        newfse.setSender( fse.getReceiver() );  // user name
        newfse.setReceiver( server );  // server name
        newfse.setUserName( fse.getUserName() );    // user name
        newfse.setNumFilesCompleted(0); // initialized to 0
        // get numFiles and fileEntryList
        newfse = setNumFilesAndEntryList(newfse, 0);

        return CMEventManager.unicastEvent(newfse, server, m_cmInfo);
    }

    private CMFileSyncEvent setNumFilesAndEntryList(CMFileSyncEvent newfse, int startListIndex) {
        // get current number of bytes except the entry list
        int curByteNum = newfse.getByteNum();
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.setNumFilesAndEntryList() called..");
            System.out.println("curByteNum before adding entries = " + curByteNum);
        }
        // set variables before the while loop
        String userName = newfse.getSender();
        List<Path> pathList = m_cmInfo.getFileSyncInfo().getPathList();
        List<Path> subList = new ArrayList<>();
        int index = startListIndex;
        int numFiles = 0;
        CMFileSyncManager fsManager = (CMFileSyncManager) m_cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        Path clientSyncHome = fsManager.getClientSyncHome();
        int startPathIndex = clientSyncHome.getNameCount();
        // create sub-list that will be added as the file-entry-list to the event
        while( curByteNum < CMInfo.MAX_EVENT_SIZE && index < pathList.size() ) {
            Path path = pathList.get(index);
            if(CMInfo._CM_DEBUG)
                System.out.println("absolute path = " + path);
            // change the absolute path to the relative path
            Path relativePath = path.subpath(startPathIndex, path.getNameCount());
            if(CMInfo._CM_DEBUG)
                System.out.println("relative path = " + relativePath);

            curByteNum += CMInfo.STRING_LEN_BYTES_LEN
                    + relativePath.toString().getBytes().length
                    + Long.BYTES
                    + Long.BYTES;
            if( curByteNum < CMInfo.MAX_EVENT_SIZE ) {
                subList.add(path);  // add the absolute path because it will be used to get meta-data.
                numFiles++;
                index++;
            }
            else {
                break;
            }
        }

        // set numFiles
        newfse.setNumFiles(numFiles);
        // make an entry list from the subList
        List<CMFileSyncEntry> fileEntryList = subList.stream()
                .map(path -> { CMFileSyncEntry fileEntry = new CMFileSyncEntry();
                    try {
                        fileEntry.setPathRelativeToHome( path.subpath(startPathIndex, path.getNameCount()) )
                                .setSize(Files.size(path))
                                .setLastModifiedTime( Files.getLastModifiedTime(path) );
                        if(CMInfo._CM_DEBUG)
                            System.out.println("fileEntry = " + fileEntry);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                    return fileEntry;
                }).collect(Collectors.toList());

        if(fileEntryList.isEmpty())
            System.err.println("fileEntryList is empty.");
        else
            newfse.setFileEntryList(fileEntryList);

        return newfse;
    }

    // processed at the server
    private boolean processFILE_ENTRIES(CMFileSyncEvent fse) {

        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processFILE_ENTRIES() called..");
            System.out.println("event = "+fse);
        }

        String userName = fse.getUserName();
        int returnCode;
        int numFilesCompleted;
        // set the entry list of the event to the entry hashtable
        m_cmInfo.getFileSyncInfo().getFileEntryListHashtable().put(userName, fse.getFileEntryList());
        List<CMFileSyncEntry> entryList = m_cmInfo.getFileSyncInfo().getFileEntryListHashtable().get(userName);
        if(entryList == null) {
            numFilesCompleted = fse.getNumFilesCompleted();
            returnCode = 0;
        }
        else {
            numFilesCompleted = fse.getNumFilesCompleted() + fse.getNumFiles();
            returnCode = 1;
        }
        System.out.println("numFilesCompleted = " + numFilesCompleted);
        System.out.println("returnCode = " + returnCode);

        // create FILE_ENTRIES_ACK event
        CMFileSyncEvent fseAck = new CMFileSyncEvent();
        fseAck.setID(CMFileSyncEvent.FILE_ENTRIES_ACK);
        fseAck.setSender( fse.getReceiver() );  // server
        fseAck.setReceiver( fse.getSender() );  // client
        fseAck.setUserName( fse.getUserName() );
        fseAck.setNumFilesCompleted( numFilesCompleted );   // updated
        fseAck.setNumFiles( fse.getNumFiles() );

        // send the ack event
        return CMEventManager.unicastEvent(fseAck, userName, m_cmInfo);
    }

    private boolean processFILE_ENTRIES_ACK(CMFileSyncEvent fse) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processFILE_ENTRIES_ACK() called..");
            System.out.println("event = "+fse);
        }

        // from here
        return false;
    }

    private boolean processEND_FILE_LIST(CMFileSyncEvent fse) {
        return false;
    }

    private boolean processEND_FILE_LIST_ACK(CMFileSyncEvent fse) {
        return false;
    }
}
