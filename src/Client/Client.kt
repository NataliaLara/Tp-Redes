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
        val ipDest ="192.168.0.13"		//IP do servidor - recebido por outra camada
        val macDest = getMacWithArp(ipDest)   //endereco mac de destino
        val macOri= ipOriMac()
        val diretorioBitsEnviados = "bitsEnviados_Client.txt" //arquivo a ser criado
        val diretorioPayload ="payload.txt" //payload recebido por outra camada
        val socket = Socket(ipDest, 15123)

        //Leitura payload
        var linha: String //conteudo do arquivo
        var payload= " "
        try {
            // Le o arquivo
            val ler = FileReader(diretorioPayload)
            val reader = BufferedReader(ler)

            linha = reader.readLine()
            while (linha != null) {
                payload = linha
                //println(payload)
                linha = reader.readLine()
            }

        } catch (e: java.lang.IllegalStateException) {

        }catch (e: IOException) {
            e.printStackTrace()
        }
        val size = payload.length

        //bits prontos para envio
        val bits = bitsFile(macToBinary(macDest), macToBinary(macOri), DecToBinary(Integer.toString(size)), toBinary(payload))

        //Escrita do arquivo com bits
        try {
            val fileBits =  File(diretorioBitsEnviados);

            // Se o arquivo nao existir, ele gera
            if (!fileBits.exists()) {
                fileBits.createNewFile()
            }

            // Prepara para escrever no arquivo
            val fw = FileWriter(fileBits.absoluteFile)
            val bw = BufferedWriter(fw)

            // Escreve e fecha arquivo
            bw.write(bits)
            bw.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }

        val transferFile = File(diretorioBitsEnviados) // arquivo a ser transferido
        val bytearray =
            ByteArray(transferFile.length().toInt()) // vetor de bits onde o arquivo será colocado para ser transferido
        val fin = FileInputStream(transferFile)
        val bin = BufferedInputStream(fin)
        bin.read(bytearray, 0, bytearray.size) // Processo de transformar o arquivo em binario
        val os = socket.getOutputStream()
        colisao()
        println("Sending Files...")
        os.write(bytearray, 0, bytearray.size)
        os.flush()
        socket.close()
        println("File transfer complete")
    }

    //METODOS

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
                if (i == 3) { //mac sempre na terceira linha
                    val ende = line!!.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    return ende[2]
                }
                i++
                line =  inn.readLine()
            }

        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return ""
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

    private fun obterIpCorreto(): String? { //corrige o erro de multiplas interfaces

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
            ip = InetAddress.getByName(obterIpCorreto());

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

}
