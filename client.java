import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by JaredRothstein on 5/1/18.
 */
public class client {
    private JPanel clientGUIPanel;
    private JButton loadButton;
    private JButton runButton;
    private JButton exit;
    public static Map<Integer,Integer> configInfo;
    private static boolean CGI_MODE = false;


    public static void main(String[] args) {
        if (args[0].equals("-cmd")) {
            if (args[1].equals("load")) {
                load();
            } else if (args[1].equals("run")) {
                run();
            } else if (args[1].equals("exit")) {
                exit();
            } else {
                System.out.println("Command not valid. Try again");
            }
        } else if (args[0].equals("-cgi")) {
            CGI_MODE = true;
            JFrame frame = new JFrame("clientGUI");
            frame.getContentPane().add(new client().clientGUIPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        } else {
            System.out.println("Command not valid. Try again");
        }

    }

    public client() {
        if(CGI_MODE) {
            clientGUIPanel = new JPanel();
            clientGUIPanel.setLayout(new GridLayout(1, 0, 10, 0));
            loadButton = new JButton("load");
            runButton = new JButton("run");
            exit = new JButton("exit");
            clientGUIPanel.add(loadButton);
            clientGUIPanel.add(runButton);
            clientGUIPanel.add(exit);

            loadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    load();
                    JOptionPane.showMessageDialog(null, "'load' has processed", "Message:", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            runButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    run();
                    JOptionPane.showMessageDialog(null, "'run' has processed", "Message:", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            exit.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    exit();
                    System.exit(0);
                    JOptionPane.showMessageDialog(null, "'exit' has processed", "Message:", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }
    }

    public static void load() {
        String fileName = "/evs/usb/configOut.txt";
        String line = null;
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            File fout = new File("/evs/clientFiles/configInfo.txt");
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            while((line = bufferedReader.readLine()) != null){
                String[] params = line.split(" ");
                try {
                    int id = Integer.parseInt(params[0]);
                    int votes = Integer.parseInt(params[2]);
                    if (id >= 0 && votes>=0) {
                        bw.write(id + " " + votes);
                        bw.newLine();
                    }
                }catch(NumberFormatException e){

                }
            }
            bw.close();
        }catch(FileNotFoundException e){
            System.out.println("Unable to open file '" + fileName + "'");
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void run() {
        initializeConfigInfo();
        String votes = "/evs/votes/votes.txt";
        String line = null;
        try {

            File prevVotes = new File("/evs/clientFiles/prevVotes.txt");
            if(prevVotes.exists()){
                if(checkRepeatedInvocations(votes,prevVotes.getPath())) {
                    return;
                }
            }
            FileOutputStream fos = new FileOutputStream(prevVotes);
            BufferedWriter bwPrevFile = new BufferedWriter(new OutputStreamWriter(fos));

            File talleyVotes = new File("/evs/usb/talleyVotes.txt");
            if(!talleyVotes.exists()){
                talleyVotes.createNewFile();
            }
            FileWriter talleyfos = new FileWriter(talleyVotes,true);
            BufferedWriter talleybw = new BufferedWriter(talleyfos);
            FileReader votesfileReader = new FileReader(votes);
            BufferedReader bufferedReader = new BufferedReader(votesfileReader);
            while((line = bufferedReader.readLine()) != null){
                try {
                    int id = Integer.parseInt(line);
                    if (id>=0 && configInfo.containsKey(id)) {
                        bwPrevFile.write(id+"");
                        bwPrevFile.newLine();
                        talleybw.write(id+"");
                        talleybw.newLine();
                    }
                }catch(NumberFormatException e){

                }
            }
            bwPrevFile.close();
            talleybw.close();
        }catch(FileNotFoundException e){
            System.out.println("Unable to open file '" + votes + "'");
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void exit() {

        File configInfo = new File ("/evs/clientFiles/configInfo.txt");
        File prevVotes = new File ("/evs/clientFiles/prevVotes.txt");
        configInfo.delete();
        prevVotes.delete();
        
    }

    public static void initializeConfigInfo(){
        configInfo = new HashMap<>();
        String fileName = "evs/clientFiles/configInfo.txt";
        String line = null;
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while((line = bufferedReader.readLine()) != null) {
                String[] params = line.split(" ");
                try {
                    int id = Integer.parseInt(params[0]);
                    int votes = Integer.parseInt(params[1]);
                    configInfo.put(id,votes);
                }catch(NumberFormatException e){

                }
            }
        }catch(FileNotFoundException e){
            System.out.println("Unable to open file '" + fileName + "', load command was not executed prior to run command");
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static boolean checkRepeatedInvocations(String votes,String preVotes){
        String line1 = null, line2 = null;
        try {
            FileReader votesReader = new FileReader(votes);
            FileReader prevVotesfileReader = new FileReader(preVotes);
            BufferedReader votesbufferedReader = new BufferedReader(votesReader);
            BufferedReader prevbufferedReader = new BufferedReader(prevVotesfileReader);
            line1 = votesbufferedReader.readLine();
            line2 = prevbufferedReader.readLine();
            while(line1!=null && line2!=null && line1.equals(line2)){
                line1 = votesbufferedReader.readLine();
                line2 = prevbufferedReader.readLine();
            }
            if(line1==null && line2==null)
                return true;
        }catch (FileNotFoundException e){

        }
        catch (IOException e){

        }
        return false;
    }
}
