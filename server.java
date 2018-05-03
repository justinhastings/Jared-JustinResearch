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
public class server {
    private JButton resultsButton;
    private JButton configureButton;
    private JButton exit;
    private JButton score;
    private JPanel server;
    private static boolean CGI_MODE = false;


    public static void main(String[] args) {
        // Determine which command is called
        if (args[0].equals("-cmd")) {
            if (args[1].equals("configure")) {
                configure();
            } else if (args[1].equals("results")) {
                results();
            } else if (args[1].equals("exit")) {
                exit();
            } else if (args[1].equals("score")) {
                score();
            } else {
                System.out.println("Command not valid. Try again");
            }
        } // Used to enter CGI simulated mode
        else if (args[0].equals("-cgi")) {
            CGI_MODE = true;
            JFrame frame = new JFrame("serverGUI");
            frame.getContentPane().add(new server().server);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        } // Alerts user that an invalid command was input
        else {
            System.out.println("Command not valid. Try again");
        }
    }

    public server() {
        if(CGI_MODE) {
            server = new JPanel();
            server.setLayout(new GridLayout(1, 0, 10, 0));
            configureButton = new JButton("configure");
            resultsButton = new JButton("results");
            score = new JButton("score");
            exit = new JButton("exit");
            server.add(configureButton);
            server.add(resultsButton);
            server.add(score);
            server.add(exit);


            configureButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    configure();
                    JOptionPane.showMessageDialog(null, "'configure' has processed", "Message:", JOptionPane.INFORMATION_MESSAGE);
                }
            });


            resultsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    results();
                    JOptionPane.showMessageDialog(null, "'results' has processed", "Message:", JOptionPane.INFORMATION_MESSAGE);
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

            score.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    score();
                    JOptionPane.showMessageDialog(null, "'score' has processed", "Message:", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }
    }

    public static void configure() {
        String fileName = "/evs/config/config.txt";
        String line = null;
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            File fout = new File("/evs/usb/configOut.txt");
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            // Creates a file that will be passed to the client that contains the configuration information
            while((line = bufferedReader.readLine()) !=null){
                String[] eachAttr = line.split(" ");
                try {
                    if (Integer.parseInt(eachAttr[0]) >= 0 && Integer.parseInt(eachAttr[2]) >= 0) {
                        bw.write(line);
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

    public static void results() {
        Map<Integer, Integer> talleyMap = loadPrevTalley();
        String talleyVotesFile = "/evs/usb/talleyVotes.txt";
        String line = null;
        try{
            File prevTally = new File("/evs/serverFiles/prevTally.txt");
            // A check to see if there has already been a tally conducted this round, and if so make sure that this is
            // tally does not match the previous.
            if(prevTally.exists()){
                if(checkRepeatedInvocations(talleyVotesFile,prevTally.getPath())) {
                    return;
                }
            }
            FileOutputStream fos = new FileOutputStream(prevTally);
            BufferedWriter bwPrevFile = new BufferedWriter(new OutputStreamWriter(fos));

            FileReader fileReader = new FileReader(talleyVotesFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            File fout = new File("/evs/config/tally.txt");

            FileOutputStream fos2 = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos2));

            // Keep track of tallies in a map
            while((line = bufferedReader.readLine()) != null){
                try {
                    int id = Integer.parseInt(line);
                    bwPrevFile.write(line); // should we do this here?
                    bwPrevFile.newLine();
                    if(talleyMap.containsKey(id)){
                        talleyMap.put(id, talleyMap.get(id)+1);
                    }else{
                        talleyMap.put(id,1);
                    }
                }catch(NumberFormatException e){

                }
            }
            Iterator it = talleyMap.entrySet().iterator();

            // Write the final tally to the tally.txt file stored in the config folder on the server
            while(it.hasNext()){
                Map.Entry pair = (Map.Entry)it.next();
                bw.write(pair.getKey().toString()+ " " + pair.getValue().toString());
                bw.newLine();
            }
            bw.close();
            bwPrevFile.close();
            // Alert the server that there is a DOS attack and tally.txt was not produced
            if (!fout.exists()) {
                File alert = new File ("/evs/alert/alert.txt");
                FileOutputStream sAlert = new FileOutputStream(alert);
                BufferedWriter alertBW = new BufferedWriter(new OutputStreamWriter(sAlert));
                alertBW.write("dos\n");
                alertBW.close();
            }


        }catch (FileNotFoundException e){
            System.out.println("Unable to open file '" + talleyVotesFile + "'");
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void exit() {

        File tally = new File ("/evs/config/tally.txt");
        File stally = new File ("/evs/scoring/tally.txt");
        File alert = new File ("/evs/alert/alert.txt");
        File score = new File ("/evs/scoring/score.txt");
        File prevTally = new File("/evs/serverFiles/prevTally.txt");
        tally.delete();
        stally.delete();
        alert.delete();
        score.delete();
        prevTally.delete();
        
    }

    public static void score() {
        int bteam = 0;
        int rteam = 0;
        int dosAlert = 0;
        int dosNoAlert = 0;
        int dosFalseAlert = 0;

        try {
            File fout = new File("/evs/scoring/score.txt");
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            String tally = "/evs/config/tally.txt";
            File tallyf = new File (tally);
            String ctally = "/evs/scoring/tally.txt";
            File alert = new File ("/evs/alert/alert.txt");
            if (tallyf.exists()) {
                // Checks if tally.txt is correct for the round. If it is the EVS team receives two points, if it is not
                // the red team receives one point.
                if (checkRepeatedInvocations(tally,ctally)) {
                    bteam += 2;
                    // If alert.txt is created and tally.txt was actually correct the red team receives a point for
                    // triggering a false alarm
                    if (alert.exists()) {
                        dosFalseAlert++;
                    }
                } else {
                    rteam++;
                    // If tally.txt is incorrect and no DOS alert is triggered then the red team receives a point for
                    // performing a DOS attack undetected
                    if (!alert.exists()) {
                        dosNoAlert++;
                    }
                }

            } else {
                // The EVS team receives a point for detecting that there was a DOS attack when appropriate
                if (alert.exists()) {
                    dosAlert++;
                }
            }
            bw.write("T\t\tB\tR\n");
            bw.write("1\t\t" + bteam + "\t0\n");
            bw.write("0\t\t0\t" + rteam + "\n");
            bw.write("DOS(NA)\t0\t" + dosNoAlert + "\n");
            bw.write("DOS(A)\t" + dosAlert + "\t0\n");
            bw.write("DOS(FA)\t0\t" + dosFalseAlert + "\n");
            bw.close();
        } catch (FileNotFoundException e) {
            System.out.println("Unable to open file\n");
        } catch(IOException e){
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

    public static Map<Integer,Integer> loadPrevTally(){
        Map<Integer, Integer> prevTalleyMap = new HashMap<>();
        String line = null;
        try {
            String prevtalleyVotesFile = "/evs/serverFiles/prevTally.txt";
            File file = new File(prevtalleyVotesFile);
            if (!file.exists()) {
                return prevTalleyMap;
            }
            FileReader fileReader = new FileReader(prevtalleyVotesFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while((line = bufferedReader.readLine()) != null){
                try {
                    int id = Integer.parseInt(line);
                    if(prevTalleyMap.containsKey(id)){
                        prevTalleyMap.put(id, prevTalleyMap.get(id)+1);
                    }else{
                        prevTalleyMap.put(id,1);
                    }
                }catch(NumberFormatException e){

                }
            }

        }catch(FileNotFoundException e){

        }catch (IOException e){

        }
        return prevTalleyMap;

    }

}
