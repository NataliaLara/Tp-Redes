using System;
using System.Text;
using System.Net; 
using System.Net.Sockets;
using System.IO;
using System.Threading;
using System.Collections.Generic;

namespace transportProtocol{
    class Program{
        static void Main(string[] args){
            int choice = 3;
            Console.WriteLine("Pick your type connection: (1) Connection Oriented - (2) Non Connection Oriented - (3) EXIT");
            choice = Int32.Parse(Console.ReadLine());
            if(choice == 1){
                TransportEngine te = new TransportEngine(choice);
                te.registerLog("Intialize Connection Oriented Process");
                te.listening();
                te.registerLog("exit Connection Oriented process");
            }else if(choice == 2){
                TransportEngine te = new TransportEngine(choice);
                Console.WriteLine("UDP");
                te.registerLog("Intialize Non Connection Oriented Process");
                te.listening();
                te.registerLog("exit Non Connection Oriented process");
            }else{
                Console.WriteLine("exit process...");
                return;
            }
        }
    }

    class TransportEngine{
        private int windowSize = 1000;
        private String dstIP;
        private String srcIP;
        private String srcPort;
        private String dstPort;
        private String data = "";
        private int segSize = 0;
        private int seq = 1;
        private int typeConn = 1;
        private String ackSyn = "000";
        //000 = SYN client
        //001 = SYN-ACK server
        //010 = ACK client
        //011 = FIM SYN
        //100 = FIM ACK+ SYN
        //101 = FIM ACK
        
        //**PDU EXAMPLE TEST*/
        //192.162.1.5 128.50.13.10 500 200 DADO

        public TransportEngine(int typeConn){
            this.typeConn = typeConn;
        }
        
        public void listening(){
            try{
                StreamWriter transDownClean = null;
                while(true){
                    Console.WriteLine("Listening");
                    if(new System.IO.FileInfo(@"../logout.txt").Length > 0){
                        StreamWriter wr = new StreamWriter(@"../redeTop.txt");
                        String PDU = "";
                        windowSize -= 100;

                        PDU = (srcIP+" "+dstIP+" "
                                +srcPort+" "+dstPort+" "
                                +seq+" "+windowSize+" 011");
                        
                        registerLog("SEND FIM-SYN");
                        wr.WriteLine(PDU);
                        wr.Close();
                        
                        StreamWriter logoutClean = new StreamWriter(@"../logout.txt");
                        logoutClean.Flush();
                        logoutClean.Close();

                        if(typeConn != 1){
                            System.Environment.Exit(0);
                        }
                    }
                    if(new System.IO.FileInfo(@"../transTop.txt").Length > 0){
                        StreamReader transTop = new StreamReader(@"../transTop.txt");
                        StreamWriter wr = new StreamWriter(@"../redeTop.txt");
                        String PDU = transTop.ReadToEnd();
                        List<String> labels = new List<String>(PDU.Split(" "));
                        
                        srcIP = labels[0];
                        dstIP= labels[1];
                        srcPort = labels[2];
                        dstPort = labels[3];
                        
                        //controle de fluxo até liberar o time out
                        if(windowSize < 0){
                            Thread.Sleep(1000);
                        }

                        if(this.typeConn == 1){
                            labels.Insert(4, seq.ToString());
                            labels.Insert(5, windowSize.ToString());
                            labels.Insert(6, ackSyn);

                            if(labels.Count > 7){
                                data = "";
                                foreach (String s in (labels.GetRange(7, labels.Count-7))){
                                    data += s+" ";
                                }
                            }

                            //three hand shake control
                            switch(ackSyn){
                                case "010":
                                    registerLog("SEND DATA");
                                    PDU = (srcIP+" "+dstIP+" "
                                          +srcPort+" "+dstPort+" "
                                          +seq+" "+windowSize+" "+ackSyn+" "+data);
                                    wr.WriteLine(PDU);
                                    wr.Close();
                                break;
                                default: //000 - SYN
                                    registerLog("INITIATE CONN");
                                    registerLog("SEND SYN");
                                    PDU = (srcIP+" "+dstIP+" "
                                          +srcPort+" "+dstPort+" "
                                          +seq+" "+windowSize+" 000");
                                    wr.WriteLine(PDU);
                                    wr.Close();
                                break;
                            }
                        }else{
                            //bypass
                            segSize = (int)(new System.IO.FileInfo(@"../transTop.txt")).Length;
                            labels.Insert(4, segSize.ToString());
                            if(labels.Count > 5){
                                data = "";
                                foreach (String s in (labels.GetRange(5, labels.Count-5))){
                                    data += s+" ";
                                }
                            }
                            
                            registerLog("SEND DATA TO APP");
                            PDU = (srcIP+" "+dstIP+" "
                                  +srcPort+" "+dstPort+" "
                                  +segSize+" "+data);
                            wr.WriteLine(PDU);
                            wr.Close();
                        }
                        transTop.Close();
                        
                        //limpa o arquivo
                        StreamWriter transTopClean = new StreamWriter(@"../transTop.txt");
                        transTopClean.Flush();
                        transTopClean.Close();
                    }
                    if(new System.IO.FileInfo(@"../transDown.txt").Length > 0){
                        StreamWriter wr;
                        StreamReader transDown = new StreamReader(@"../transDown.txt");
                        String PDU = transDown.ReadToEnd();
                        List<String> labels = new List<String>(PDU.Split(" "));
                        srcIP = labels[0];
                        dstIP= labels[1];
                        srcPort = labels[2];
                        dstPort = labels[3];
                        
                        windowSize += 100;

                        //controle de fluxo até o time out
                        if(windowSize < 0){
                            Thread.Sleep(1000);
                        }

                        if(this.typeConn == 1){                          
                            windowSize = Convert.ToInt32(labels[5]);
                            seq = Convert.ToInt32(labels[4])+1;
                            ackSyn = labels[6];
                            if(labels.Count > 7){
                                data = "";
                                foreach (String s in (labels.GetRange(7, labels.Count-7))){
                                    data += s+" ";
                                }
                            }

                            //three hand shake control
                            switch(ackSyn){
                                case "001"://SYN-ACK
                                    windowSize -= 100;
                                    PDU = (srcIP+" "+dstIP+" "
                                          +srcPort+" "+dstPort+" "
                                          +seq+" "+windowSize+" 010"+" "+data);
                                    registerLog("RECEIVE SYN+ACK");
                                    registerLog("SEND ACK+DATA");
                                    wr = new StreamWriter(@"../redeTop.txt");
                                    wr.WriteLine(PDU);
                                    wr.Close();
                                break;
                                case "010"://ACK
                                    PDU = (srcIP+" "+dstIP+" "
                                          +srcPort+" "+dstPort+" "+data);
                                    registerLog("RECEIVE DATA");
                                    registerLog("SEND DATA TO APP");
                                    wr = new StreamWriter(@"../appDown.txt");
                                    wr.WriteLine(PDU);
                                    wr.Close();
                                break;
                                case "011":
                                    windowSize -= 100;
                                    PDU = (srcIP+" "+dstIP+" "
                                          +srcPort+" "+dstPort+" "
                                          +seq+" "+windowSize+" 100");
                                    registerLog("RECEIVE FIM SYN");
                                    registerLog("SEND FIM ACK+SYN");
                                    wr = new StreamWriter(@"../redeTop.txt");
                                    wr.WriteLine(PDU);
                                    wr.Close();
                                break;
                                case "100":
                                    PDU = (srcIP+" "+dstIP+" "
                                          +srcPort+" "+dstPort+" "
                                          +seq+" "+windowSize+" 101");
                                    registerLog("RECEIVE FIM+ACK");
                                    registerLog("FIM ACK & FIM CONN");

                                    transDownClean = new StreamWriter(@"../transDown.txt");
                                    transDownClean.Flush();
                                    transDownClean.Close();

                                    System.Environment.Exit(0);
                                break;
                                default: //00 - SYN
                                    windowSize -= 100;
                                    PDU = (srcIP+" "+dstIP+" "
                                          +srcPort+" "+dstPort+" "
                                          +seq+" "+windowSize+" 001");
                                    registerLog("RECEIVE SYN");
                                    registerLog("SEND SYN+ACK");
                                    wr = new StreamWriter(@"../redeTop.txt");
                                    wr.WriteLine(PDU);
                                    wr.Close();
                                break;
                            }
                        }else{                           
                            //bypass
                            segSize = (int)(new System.IO.FileInfo(@"../transDown.txt")).Length;
                            labels.RemoveAt(4);
                            labels.Insert(4, segSize.ToString());
                            
                            if(labels.Count > 5){
                                data = "";
                                foreach (String s in (labels.GetRange(5, labels.Count-5))){
                                    data += s+" ";
                                }
                            }

                            PDU = (srcIP+" "+dstIP+" "
                                  +srcPort+" "+dstPort+" "
                                  +segSize+" "+data);
                            registerLog("RECEIVE DATA");
                            registerLog("SEND TO APP");
                            wr = new StreamWriter(@"../appDown.txt");
                            wr.WriteLine(PDU);
                            wr.Close();
                        }
                        transDown.Close();
                        
                        //limpa o arquivo
                        transDownClean = new StreamWriter(@"../transDown.txt");
                        transDownClean.Flush();
                        transDownClean.Close();
                    }
                    Thread.Sleep(2000);
                } 
            }
            catch (System.Exception){
                
            }
        }
        public string binaryToString(string data){
            List<Byte> byteList = new List<Byte>();
        
            for (int i = 0; i < data.Length; i += 8)
            {
                byteList.Add(Convert.ToByte(data.Substring(i, 8), 2));
            }
            return Encoding.ASCII.GetString(byteList.ToArray());
        }

        public string stringToBinary(string data){
            StringBuilder sb = new StringBuilder();
        
            foreach (char c in data.ToCharArray())
            {
                sb.Append(Convert.ToString(c, 2).PadLeft(8, '0'));
            }
            return sb.ToString();
        }

        public void registerLog(String msg){
            if(typeConn == 1) msg = "TCP: "+msg;
            else msg = "UDP: "+msg;
            DateTime now = DateTime.Now;
            String nowDate = ""+now.Date.Year+"_"+now.Date.Month+"_"+now.Date.Day;
            String fileName = "log_"+nowDate+".txt";
            StreamWriter wr = new StreamWriter(@fileName, true);
            wr.WriteLine(msg);
            wr.Close();
        }
    }
}
