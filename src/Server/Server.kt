package Server

import java.net.*
import java.io.*

object
Server {

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val diretorioRecebido = "4_bitsRecebidos_Server.txt" //arquivo a ser criado
        val diretorioPayloadBits ="5_bitsPayload_Server.txt" //arquivo a ser criado
        val diretorioPayloadRecebido = "6_payloadRecebido_Server.txt" //arquivo a ser criado
        val filesize = 1022386

        val serverSocket = ServerSocket(15123)
        //val serverSocket = ServerSocket(6020)

        while(true){
            val socket = serverSocket.accept()

            println("Accepted connection : $socket")

            //recebe arquivo do cliente
            val pduBits :String = recebeArquivo(filesize,socket,diretorioRecebido)
            //println(pduBits.length)
            println("File received\n")

            //Escrita do payload (bits)
            gravarArquivo(diretorioPayloadBits,separaBitsPayload(pduBits))

            //escrita do payload
            gravarArquivo(diretorioPayloadRecebido,bitsToString(separaBitsPayload(pduBits)))

            /*
            //envio
            val ipDest =172.16.254.114
            val socketEnvio = Socket(ipDest, 55555)

            val transferFile = File(diretorioPayloadBits) // arquivo a ser transferido
            val bytearray =
                ByteArray(transferFile.length().toInt()) // vetor onde o arquivo será colocado para ser transferido
            val fin = FileInputStream(transferFile)
            val bin = BufferedInputStream(fin)
            bin.read(bytearray, 0, bytearray.size) // Processo de transformar o arquivo em byte
            val os = socket.getOutputStream()
             */
            socket.close()
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
