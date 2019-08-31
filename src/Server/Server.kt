package Server

import java.net.*
import java.io.*

object Server {

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val diretorioRecebido = "3_bitsRecebidos_Server.txt" //arquivo a ser criado
        val diretorioPayloadBits ="4_bitsPayload_Server.txt" //arquivo a ser criado
        val diretorioPayloadRecebido = "5_payloadRecebido_Server.txt" //arquivo a ser criado
        val filesize = 1022386
        var bytesRead: Int
        var currentTot: Int

        val serverSocket = ServerSocket(15123)

        while(true){
            val socket = serverSocket.accept()

            println("Accepted connection : $socket")

            val bytearray = ByteArray(filesize) // vetor de bits que receberá o arquivo de bits que o cliente tranferiu
            val `is` = socket.getInputStream() // canal para coletar os dados que viram do cliente para o servidor
            val fos = FileOutputStream(diretorioRecebido) // Objeto que aponta para o arquivo que será preenchido
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
            val pduBits = StringBuilder()
            for (i in 0 until currentTot) {
                if(!bytearray[i].toString().equals("10")) //nao conta o espaco em branco
                    pduBits.append(bytearray[i].toChar())
            }
            bos.write(bytearray, 0, currentTot) //escrevemos os bytes no arquivo
            bos.flush()
            bos.close()
            println("File received\n")
            //println(pduBits)
            //println(pduBits.length)

            //Escrita do payload (bits)
            gravarArquivo(diretorioPayloadBits,separaBitsPayload(pduBits.toString()))

            //println(separaBitsPayload(pduBits.toString()))
            //println(bitsToString(separaBitsPayload(pduBits.toString())))

            //escrita do payload
            gravarArquivo(diretorioPayloadRecebido,bitsToString(separaBitsPayload(pduBits.toString())))

            socket.close()
        }

    }

    //METODOS

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
        s = pdu.substring(64, pdu.length)  //mesma maquina
        //s = pdu.substring(112, pdu.length) //dois computadores
        return s
    }

    private fun bitsToString(str: String): String {
        var payload = str
        val string = StringBuilder()
        var i = 0
        while (i < payload.length) {
            val c = Integer.parseInt(payload.substring(i, i + 8), 2).toChar()
            string.append(c)
            i = i + 8
        }

        return string.toString()
    }

    /*
    private fun BitToString(str: String): String {
        var payload = str
        val string = StringBuilder()
        payload = separaBitsPayload(str)
        var i = 0
        while (i < payload.length) {
            val c = Integer.parseInt(payload.substring(i, i + 8), 2).toChar()
            string.append(c)
            i = i + 8
        }
        return string.toString()
    }*/

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

}
