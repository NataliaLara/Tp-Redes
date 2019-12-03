package Server

import Client.Client
import java.net.*
import java.io.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object
Server {
    var diretorioLog="logFisica.txt"
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val diretorioRecebido = "4_bitsRecebidos_Server.txt" //arquivo a ser criado
        val diretorioPayloadBits ="5_bitsPayload_Server.txt" //arquivo a ser criado
        val diretorioPayloadRecebido = "6_payloadRecebido_Server.txt" //arquivo a ser criado
        val filesize = 1022386
        val diretoriorRedeTop ="fisicaTop.txt"
        var diretorioTransDown ="redeDown.txt"
        val port=15123

        val serverSocket = ServerSocket(port)

        while(true){
            var socket = serverSocket.accept()

            //println("Accepted connection : $socket")
            log("Accepted connection : $socket")
            var ipOrigem = socket.inetAddress.hostAddress
            //recebe arquivo do cliente
            val pduBits :String = recebeArquivo(filesize,socket,diretorioRecebido)
            //println(pduBits.length)

            //println("File received")
            log("File received")

            //Escrita do payload (bits)
            gravarArquivo(diretorioPayloadBits,separaBitsPayload(pduBits))

            //escrita do payload
            gravarArquivo(diretorioPayloadRecebido,bitsToString(separaBitsPayload(pduBits)))

            //escrita request
            //gravarArquivo(diretorioRedeTop,bitsToString(separaBitsPayload(pduBits)))


            gravarArquivo(diretorioTransDown,bitsToString(separaBitsPayload(pduBits)))
            var j = 0
            var response:String = " "
            var infos = response.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().toList()

            /*while (j<1) {
                response = lerArquivo(diretorioTransDown)
                var infos = response.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().toList()
                j = infos.size
            }*/
            //println("Response Receive")
            log("Response Receive")
           //socket.close()
            Thread.sleep(1000)
            socket = Socket(ipOrigem, 55555)
            //val socket2 = serverSocket.accept()

            val transferFile = File(diretorioTransDown) // arquivo a ser transferido
            val bytearray =
                ByteArray(transferFile.length().toInt()) // vetor onde o arquivo será colocado para ser transferido
            val fin = FileInputStream(transferFile)
            val bin = BufferedInputStream(fin)
            bin.read(bytearray, 0, bytearray.size) // Processo de transformar o arquivo em byte
            //val os = socketResponse.getOutputStream()
            val os = socket.getOutputStream()

            os.write(bytearray, 0, bytearray.size)
            os.flush()
            //socketResponse.close()

            log("Sending Response...")
            socket.close();

            println("\n")
            //val limparArquivo = File(diretorioTransDown)
            //limparArquivo.writeText("");

        }

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

    private fun separaBitsPayload(pdu: String): String{
        val s: String
        //s = pdu.substring(64, pdu.length)  //mesma maquina
        s = pdu.substring(112, pdu.length) //dois computadores
        return s
    }

    private fun separaBitsMacOri(pdu:String) : String{ //origem da outra maquina, destino dessa
        val s: String
        s = pdu.substring(48, 96) //dois computadores
        return s
    }

    private fun bitsToMac(str: String) :String{
        var mac = str
        val string = StringBuilder()
        var i = 0
        while (i < mac.length && (i+8)<=mac.length) {
            val c = java.lang.Integer.toHexString(Integer.parseInt(mac.substring(i, i + 8),2))
            //Byte.parseByte(,2)
            //println(c)
            string.append(c+":")
            i = i + 8
        }
        string.deleteCharAt(string.length-1)
        return string.toString()
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

    private fun lerArquivoLog(diretorio:String):String{

        var linha: String //conteudo do arquivo
        var arquivo= " "
        try {
            // Le o arquivo
            val ler = FileReader(diretorio)
            val reader = BufferedReader(ler)

            linha = reader.readLine()
            while (linha != null) {
                arquivo= arquivo+linha+"\n"
                //println(linha)
                linha = reader.readLine()
            }

        } catch (e: java.lang.IllegalStateException) {

        }catch (e: IOException) {
            e.printStackTrace()
        }
        return arquivo

    }

    private fun log(instrucao:String){
        val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val date = Date()
        var log= lerArquivoLog(diretorioLog)

        println(dateFormat.format(date) +" "+instrucao)
        //Thread.sleep(500)
        gravarArquivo(diretorioLog,log+"Server Physical Layer "+dateFormat.format(date)+" " +instrucao)
    }

    private fun bitsToString(str: String): String { //ascii
        var payload = str
        val string = StringBuilder()
        var i = 0
        while (i < payload.length && (i+8)<=payload.length) {
            val c = Integer.parseInt(payload.substring(i, i + 8), 2).toChar()
            //println(c)
            string.append(c)
            i = i + 8
        }
        return string.toString()
    }

}
