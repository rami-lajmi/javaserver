import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class serv extends Thread{

    static final String HTML_START =
            "<html>" +
                    "<head><meta charset=\"utf-8\"><link rel=\'stylesheet\' href=\'https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css\' integrity=\'sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh\' crossorigin=\'anonymous\'><title>HTTP Server in java</title></head>" +
                    "<body><h3>Hello world !</h3>";

    static String monmsg = "";

    static String tabledeb = "<table class=\'table\'> <thead> <tr> <th scope=\'col\'>#</th><th scope=\'col\'>Contenu</th></tr></thead><tbody>";
    static String tablefin = "</tbody></table>";

    static final String HTML_END =
            "<script src=\'https://code.jquery.com/jquery-3.4.1.min.js\'></script><script src=\'https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js\'></script><script src=\'https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js\'></script>" +
            "</body>" +
                    "</html>";

    Socket connectedClient = null;
    BufferedReader inFromClient = null;
    DataOutputStream outToClient = null;


    public serv(Socket client) {
        connectedClient = client;
    }

    public void run() {

        try {

            System.out.println( "The Client "+
                    connectedClient.getInetAddress() + ":" + connectedClient.getPort() + " is connected");

            inFromClient = new BufferedReader(new InputStreamReader (connectedClient.getInputStream()));
            outToClient = new DataOutputStream(connectedClient.getOutputStream());

            String requestString = inFromClient.readLine();
            String headerLine = requestString;

            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            String httpMethod = tokenizer.nextToken();
            String httpQueryString = tokenizer.nextToken();

            StringBuffer responseBuffer = new StringBuffer();
            responseBuffer.append("<b> This is the HTTP Server Home Page.... </b><BR>");
            responseBuffer.append("The HTTP Client request is ....<BR>");

                try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5433/imwd276-server", "imwd276-server", "toor")) {

                    Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM public.messages");
                    while (resultSet.next()) {
                        monmsg = "<tr><th scope=\'row\'>"+resultSet.getString("id")+"</th><td>"+resultSet.getString("content")+"</td></tr>";
                    }

                }
                catch (SQLException e) {
                    System.out.println("Connection failure.");
                    e.printStackTrace();
                }

            while (inFromClient.ready())
            {
                // Read the HTTP complete HTTP Query
                responseBuffer.append(requestString + "<BR>");
                System.out.println(requestString);
                requestString = inFromClient.readLine();
            }

            if (httpMethod.equals("GET")) {
                if (httpQueryString.equals("/")) {
                    // The default home page
                    sendResponse(200, responseBuffer.toString(), false);
                } else {
                    //This is interpreted as a file name
                    String fileName = httpQueryString.replaceFirst("/", "");
                    fileName = URLDecoder.decode(fileName);
                    if (new File(fileName).isFile()){
                        sendResponse(200, fileName, true);
                    }
                    else {
                        sendResponse(404, "<b>The Requested resource not found ...." +
                                "Usage: http://127.0.0.1:3000 or http://127.0.0.1:3000/</b>", false);
                    }
                }
            }
            else sendResponse(404, "<b>The Requested resource not found ...." +
                    "Usage: http://127.0.0.1:3000 or http://127.0.0.1:3000/</b>", false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendResponse (int statusCode, String responseString, boolean isFile) throws Exception {

        String statusLine = null;
        String serverdetails = "Server: Java HTTPServer";
        String contentLengthLine = null;
        String fileName = null;
        String contentTypeLine = "Content-Type: text/html" + "\r\n";
        FileInputStream fin = null;

        if (statusCode == 200)
            statusLine = "HTTP/1.1 200 OK" + "\r\n";
        else
            statusLine = "HTTP/1.1 404 Not Found" + "\r\n";

        if (isFile) {
            fileName = responseString;
            fin = new FileInputStream(fileName);
            //contentLengthLine = "Content-Length: " + Integer.toString(fin.available()) + "\r\n";
            contentLengthLine = "\r\n";
            if (!fileName.endsWith(".htm") && !fileName.endsWith(".html"))
                //contentTypeLine = "Content-Type: \r\n";
                contentTypeLine = "";

        }
        else {
            //responseString = serv.HTML_START + responseString + serv.HTML_END;
            responseString = serv.HTML_START + tabledeb + monmsg + tablefin + serv.HTML_END;
            contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";
        }

        outToClient.writeBytes(statusLine);
        outToClient.writeBytes(serverdetails);
        outToClient.writeBytes(contentTypeLine);
        outToClient.writeBytes(contentLengthLine);
        outToClient.writeBytes("Connection: close\r\n");
        outToClient.writeBytes("\r\n");

        if (isFile) sendFile(fin, outToClient);
        else outToClient.writeBytes(responseString);

        outToClient.close();
    }

    public void sendFile (FileInputStream fin, DataOutputStream out) throws Exception {
        byte[] buffer = new byte[1024] ;
        int bytesRead;

        while ((bytesRead = fin.read(buffer)) != -1 ) {
            out.write(buffer, 0, bytesRead);
        }
        fin.close();
    }

    public static void main (String args[]) throws Exception {

        ServerSocket Server = new ServerSocket (3000, 10, InetAddress.getByName("127.0.0.1"));
        System.out.println ("TCPServer Waiting for client on port 3000");

        while(true) {
            Socket connected = Server.accept();
            (new serv(connected)).start();
        }
    }
}
