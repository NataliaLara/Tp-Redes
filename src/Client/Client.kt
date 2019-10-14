package Client

import java.net.*
import java.io.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.RuntimeException
import java.util.Random
import java.util.*

object Client {

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        //val ipDest ="192.168.0.13"		//IP do servidor - recebido por outra camada
        //val ipDest ="172.16.254.114"

        val macOri= ipOriMac()
        val diretorioPayload ="1_payload.txt" //payload recebido por outra camada
        val diretorioPduOriginal = "2_pduOriginal_Client.txt"
        val diretorioBitsEnviados = "3_pduBitsEnviados_Client.txt"
        val diretorioCamadaSuperior = "0_pduCamadaSuperior_Client.txt"
        val filesize = 1022386

        //cliente sempre online
        //verificar se payload está vazio

        val aplicationSocket = ServerSocket(44444)


        while(true) {

            val socketAplication = aplicationSocket.accept()

            //recebe arquivo de outra camada
            val pduSuperior :String = recebeArquivo(filesize,socketAplication,diretorioCamadaSuperior)
            //println(pduSuperior)

            val infos = pduSuperior.split(" ")

            for (i in infos.indices) {
                //println(infos[i])
            }
            val ipDest=infos[0];
            val macDest = getMacWithArp(ipDest)   //endereco mac de destino

            val socket = Socket(ipDest, 15123)
            //val socket = Socket(ipDest, 8081)

            //Leitura payload
            val payload = infos[1] +" " + infos[2]
            val size = payload.length

            //bits prontos para envio
            val pduBits = bitsFile(
                macToBinary(macDest),
                macToBinary(macOri),
                DecToBinary(Integer.toString(size)),
                toBinary(payload)
            )
            //println(pduBits.length)

            //Escrita do arquivo com a pdu original
            gravarArquivo(
                diretorioPduOriginal, macDest.replace("-", ":") +
                        macOri.replace("-", ":") + Integer.toString(size) + payload
            )

            //Escrita do arquivo com pdu de bits
            gravarArquivo(diretorioBitsEnviados, pduBits)

            val transferFile = File(diretorioBitsEnviados) // arquivo a ser transferido
            val bytearray =
                ByteArray(transferFile.length().toInt()) // vetor onde o arquivo será colocado para ser transferido
            val fin = FileInputStream(transferFile)
            val bin = BufferedInputStream(fin)
            bin.read(bytearray, 0, bytearray.size) // Processo de transformar o arquivo em byte
            val os = socket.getOutputStream()

            colisao()
            println("Sending Files...")
            os.write(bytearray, 0, bytearray.size)
            os.flush()
            socket.close()
            println("File transfer complete")
        }


    }

    private fun lerArquivo(diretorio:String):String{

        var linha: String //conteudo do arquivo
        var arquivo= " "
        try {
            // Le o arquivo
            val ler = FileReader(diretorio)
            val reader = BufferedReader(ler)

            linha = reader.readLine()
            while (linha != null) {
                arquivo= linha
                //println(linha)
                linha = reader.readLine()
            }

        } catch (e: java.lang.IllegalStateException) {

        }catch (e: IOException) {
            e.printStackTrace()
        }
        return arquivo

    }

    private fun gravarArquivo(diretorio:String, mensagem:String){
        //Escrita do payload (bits)
        try {
            val filePayload =  File(diretorio);

            if (!filePayload.exists()) {
                filePayload.createNewFile()
            }
            val fw = FileWriter(filePayload.absoluteFile)
            val bw = BufferedWriter(fw)

            bw.write(mensagem)
            bw.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getMacWithArp(ipAddress: String): String {
        var i = 0
        val run = Runtime.getRuntime()
        val commPing = "ping $ipAddress  -c 3"
        val commArp = "arp -a $ipAddress"

        try {
            run.exec(commPing)
            val p = Runtime.getRuntime().exec(commArp)
            val inn = BufferedReader(InputStreamReader(p.inputStream))
            var line: String? = null
            line =  inn.readLine()
            while (line != null) {
                //Linux
                //println("line"+line)
                val posi =line!!.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                //Windows
                if (i == 3 ) { //mac sempre na terceira linha
                    return posi[2]
                }
                i++
                line =  inn.readLine()

                //Linux
                if(i<3 && line==null && !posi[0].equals("arp:")){
                    return(posi[3].replace(":","-"))

                }

            }

        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return ipOriMac() //envia o ip Origem=Destino
    }

    private fun DecToBinary(decimal: String): String {
        var numero: String
        val string = StringBuilder()

        if (decimal.length < 2)
            string.append("00000000")
        for (i in 0 until decimal.length) {
            numero = Character.toString(decimal[i])

            val num = Integer.toString(Integer.parseInt(numero, 10), 2)
            var j = num.length
            while (j < 8) {
                string.append(0)
                j++
            }
            string.append(num)
        }
        return string.toString()
    }

    private fun HexToBinary(hexa: String): String {

        val bin = Integer.toString(Integer.parseInt(hexa, 16), 2)
        //print(bin)
        val string = StringBuilder()
        var i = bin.length
        while (i < 8) {
            string.append("0")
            i++
        }
        string.append(bin)
        return string.toString()

    }

    private fun macToBinary(mac: String): String {
        val hexas = mac.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val string = StringBuilder()
        for (i in hexas.indices) {
            string.append(HexToBinary(hexas[i]))
        }
        return string.toString()
    }

    private fun obterIpCorreto(): String? { //resolve o problema de multiplas interfaces
        var ipAddress: String? = null
        var net: Enumeration<NetworkInterface>? = null
        try {
            net = NetworkInterface.getNetworkInterfaces()
        } catch (e: SocketException) {
            throw RuntimeException(e)
        }

        while (net!!.hasMoreElements()) {
            val element = net.nextElement()
            val addresses = element.inetAddresses
            while (addresses.hasMoreElements()) {
                val ip = addresses.nextElement()

                if (ip.isSiteLocalAddress) {
                    ipAddress = ip.hostAddress
                }
            }
        }
        //println(ipAddress)
        return(ipAddress)
    }

    fun ipOriMac(): String {
        var ip: InetAddress
        try {
            //ip = InetAddress.getLocalHost()
            ip = InetAddress.getByName(obterIpCorreto()); //resolve problema de multiplas interfaces

            val network = NetworkInterface.getByInetAddress(ip)
            val mac = network.hardwareAddress
            val sb = StringBuilder()
            for (i in mac.indices) {
                sb.append(String.format("%02X%s", mac[i], if (i < mac.size - 1) "-" else ""))
            }
            //System.out.println(sb.toString());
            return sb.toString()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }

        return "41-7f-33-0e-65-b2"
    }

    private fun bitsFile(macDest: String, macOri: String, size: String, payload: String): String {
        return macDest + macOri + size + payload
    }

    private fun colisao () {
        val gerador = Random()
        val resul = gerador.nextInt(1000)
        if (resul<500){
            println("Collision! Waiting...")
            Thread.sleep(3000)
        }
    }

    private fun toBinary(s: String): String { //ascii
        val temp = s
        val bytes = s.toByteArray()

        val binary = StringBuilder()
        for (b in bytes) {
            var `val` = b.toInt()
            for (i in 0..7)
            {
                binary.append(if (`val` and 128 == 0) 0 else 1)
                `val` = `val` shl 1
            }
        }
        return binary.toString()
    }

    private fun recebeArquivo(filesize:Int, socket:Socket, diretorio:String): String{
        var currentTot: Int
        var bytesRead: Int
        val bytearray = ByteArray(filesize) // vetor de bits que receberá o arquivo de bits que o cliente tranferiu
        val `is` = socket.getInputStream() // canal para coletar os dados que viram do cliente para o servidor
        val fos = FileOutputStream(diretorio) // Objeto que aponta para o arquivo que será preenchido
        // com dados copiados do arquivo do cliente

        val bos =
            BufferedOutputStream(fos) // BufferedOutputStream ajuda a gravar dados no arquivo de entrada/saída através de uma matriz de bytes.
        bytesRead = `is`.read(bytearray, 0, bytearray.size)
        currentTot = bytesRead // numero de bits lidos

        do {
            bytesRead = `is`.read(bytearray, currentTot, bytearray.size - currentTot)

            if (bytesRead >= 0) currentTot += bytesRead
        } while (bytesRead > -1) //le apartir do fluxo de entrada e vai atualizando o numero de bits lidos ate nao ter mais dados restantes no fluxo de entrada, ou seja
        // bytesRead ser -1
        val arquivo = StringBuilder()
        for (i in 0 until currentTot) {
            if(!bytearray[i].toString().equals("10")) //nao conta o espaco em branco
                arquivo.append(bytearray[i].toChar())
        }
        bos.write(bytearray, 0, currentTot) //escrevemos os bytes no arquivo
        bos.flush()
        bos.close()
        return arquivo.toString()
    }

}
