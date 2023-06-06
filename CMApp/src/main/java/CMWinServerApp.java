import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.border.Border;
import javax.swing.text.*;
import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.stub.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;


public class CMWinServerApp extends JFrame {
    private JTextArea consoleTextArea;
    private JTextArea inputTextArea;
    public JTextArea fileTextArea;

    //실질적 기능 하는 부분
    private CMServerStub m_serverStub;
    private CMServerEventHandler m_eventHandler;

    public CMWinServerApp() {
        setTitle("Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setRows(2);


        fileTextArea = new JTextArea();
        fileTextArea.setEditable(false);



        JScrollPane consoleScrollPane = new JScrollPane(consoleTextArea);
        //JScrollPane fileScrollPane = new JScrollPane(fileTextArea);


        inputTextArea = new JTextArea();
        inputTextArea.setRows(2);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            String input = inputTextArea.getText();
            processInput(input);
            inputTextArea.setText("");

            String strFile = "./server-file-path/" + input;

            File file = new File(strFile);
            String[] strServerFiles = file.list();
            int nFiles = strServerFiles.length;
            fileTextArea.setText("");
            for(int i = 0; i < nFiles; i++)
            {
                fileTextArea.append(strServerFiles[i]);
                fileTextArea.append("\n");
            }
        });

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JScrollPane(inputTextArea), BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);
        //inputPanel.add(new JScrollPane(fileTextArea), BorderLayout.EAST);

        JPanel filePane = new JPanel(new BorderLayout());
        filePane.add(fileTextArea, BorderLayout.CENTER);
        filePane.add(inputPanel, BorderLayout.SOUTH);
        //JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, consoleScrollPane, fileScrollPane);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, consoleScrollPane, filePane);
        splitPane.setResizeWeight(0.5);

        //mainPanel.add(inputPanel, BorderLayout.SOUTH);
        //add(inputPanel, BorderLayout.SOUTH);

        add(splitPane, BorderLayout.CENTER);

        redirectSystemOutput();

        pack();
        setVisible(true);



        //실질적 기능 하는 부분
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

    public void processInput(String input) {

    }

    public void redirectSystemOutput() {
        PrintStream printStream = new PrintStream(new CustomOutputStream(consoleTextArea));
        System.setOut(printStream);
    }

    public static void main(String[] args) {
        CMWinServerApp server = new CMWinServerApp();
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                    }
                }
        );

        System.out.println("Hello Server");

        CMServerStub cmStub = server.getServerStub();
        cmStub.setAppEventHandler(server.getServerEventHandler());

        // set config home
        cmStub.setConfigurationHome(Paths.get("."));
        // set file-path home
        cmStub.setTransferedFileHome(cmStub.getConfigurationHome().resolve("server-file-path"));

        cmStub.startCM();
    }

    private static class CustomOutputStream extends OutputStream {
        private JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) throws IOException {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }
}
