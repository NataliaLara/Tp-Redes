
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author vinicius
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        String MAC = getMacWithArp("192.168.0.21");
        if(MAC != null){
            System.out.println("MAC Address: "+MAC);
        }else{
            System.out.println("error getting MAC");
        }
        
    }
    
    public static String getMacWithArp(String ipAddress){
        
        Runtime run = Runtime.getRuntime();
        String commPing = "ping "+ipAddress+"  -c 3";
        String commArp = "arp -a "+ipAddress;
        
        try {
            
            run.exec(commPing);
            
            Process p = Runtime.getRuntime().exec(commArp);
            BufferedReader inn = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            
            while ((line = inn.readLine()) != null) {
               System.out.println(line);
                String []words = line.split(" ");
                for(String s: words){
                    if(s.split(":").length == 6){
                        return s;
                    }
                }
            }
            
        } catch (IOException ex) {
            
            ex.printStackTrace();
            
        }
        return null;
    }
    
}
