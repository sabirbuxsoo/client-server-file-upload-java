import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

/**
 * Author: Sabir Buxsoo 
 * Assignment: 1
 * Server Class
 * Implements multithreading for multiple client requests
 */
public class Server {

    // Main Method
    public static void main(String[] args) {
        try {
            // Open Socket on port 1337
            ServerSocket server_socket = new ServerSocket(1999);

            System.out.println("Server started on port 1999");
            while (true) {
                new Thread(new ClientWorker(server_socket.accept())).start();
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

class ClientWorker implements Runnable {

    // Initialize private variables
    private Socket target_socket;
    private DataInputStream din;
    private DataOutputStream dout;
    private String received; // Command received from the Client
    private String[] rec; // Split client Command
    private String receiver; // Get client command line command
    private String password = "password123"; // Password for requests to Secured Folder
    private String username;
    //Authentication
    private User user = new User();

    public ClientWorker(Socket recv_socket) {
        try {
            target_socket = recv_socket;
            din = new DataInputStream(target_socket.getInputStream());
            dout = new DataOutputStream(target_socket.getOutputStream());

        } catch (IOException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        long threadId = Thread.currentThread().getId();

        try{

            //Handling User Login to the Server
            dout.writeUTF("Please enter your username");
            dout.flush();

            username = din.readUTF();
            if(user.userExists(username)){
                dout.writeUTF("Please enter your password");
                dout.flush();

                String password = din.readUTF();
                if(user.checkPassword(username, password)){
                    dout.writeUTF("Welcome, " + username + ". You are now connected.");
                    dout.flush();
                    System.out.println(username + " is connected to the server.");
                }else{
                    System.out.println("Wrong passowrd input.");
                    dout.writeUTF("403");
                    dout.flush();
                    Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
                    //Iterate over set to find yours
                    for(Thread thread : setOfThread){
                        if(thread.getId()== threadId){
                            thread.interrupt();
                        }
                    }
                }            
            }else{
                System.out.println("User does not exist.");
                dout.writeUTF("403");
                dout.flush();
                Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
                //Iterate over set to find yours
                for(Thread thread : setOfThread){
                    if(thread.getId()== threadId){
                        thread.interrupt();
                    }
                }
            }
        } catch(IOException ex){
             Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Wait for commands from the Client
        while (true) {
            try {
                if (din.available() > 0) {
                    received = din.readUTF();
                    rec = received.split(" ");
                    receiver = rec[0];

                } else {
                    receiver = "default";
                }

                switch (receiver) {
                    // When Client Exits Server
                    case "Exit":
                        Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
                        //Iterate over set to find yours
                        for(Thread thread : setOfThread){
                            if(thread.getId()== threadId){
                                thread.interrupt();
                                System.out.println(username + " has left the server.");
                                dout.writeUTF("Goodbye " + username + "!");
                                dout.flush();
                            }
                        }
                        break;
                    // When Client uploads a file
                    case "Upload":
                        RandomAccessFile rw = null;
                        long current_file_pointer = 0;
                        boolean loop_break = false;
                        System.out.println(username + " is uploading file to Server.");

                        while (!loop_break) {
                            byte[] initilize = new byte[1];
                            try {
                                din.read(initilize, 0, initilize.length);
                                if (initilize[0] == 2) {
                                    byte[] cmd_buff = new byte[3];
                                    din.read(cmd_buff, 0, cmd_buff.length);
                                    byte[] recv_data = ReadStream();

                                    // Upload file to Server in the Server Folder
                                    switch (Integer.parseInt(new String(cmd_buff))) {
                                        case 124:
                                            rw = new RandomAccessFile("../Server/Files/" + new String(recv_data), "rw"); // Change
                                                                                                                   // folder
                                                                                                                   // name
                                                                                                                   // if
                                                                                                                   // required
                                                                                                                   // and
                                                                                                                   // recompile
                                                                                                                   // code
                                            dout.write(CreateDataPacket("125".getBytes("UTF8"),
                                                    String.valueOf(current_file_pointer).getBytes("UTF8"))); // Write
                                                                                                             // data
                                            dout.flush();
                                            break;
                                        case 126:
                                            rw.seek(current_file_pointer);
                                            rw.write(recv_data);
                                            current_file_pointer = rw.getFilePointer();
                                            dout.write(CreateDataPacket("125".getBytes("UTF8"),
                                                    String.valueOf(current_file_pointer).getBytes("UTF8")));
                                            dout.flush();
                                            break;
                                        case 127:
                                            if ("Close".equals(new String(recv_data))) {
                                                loop_break = true;
                                                System.out.println(
                                                        username + " has successfully uploaded file to server"); // Trace
                                                                                                                 // on
                                                                                                                 // terminal
                                                                                                                 // to
                                                                                                                 // show
                                                                                                                 // file
                                                                                                                 // uploaded
                                            }
                                            break;
                                    }
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        break;
                    case "SecureUpload":
                        dout.writeUTF("Please enter the password");
                        dout.flush();

                        //Read client password and check if password match, 403 Error if not
                        String pass = din.readUTF();
                        if (!pass.equals(password)) {
                            dout.writeUTF("403");
                            dout.flush();
                            break;
                        }

                        dout.writeUTF("Choose file");
                        dout.flush();
                        RandomAccessFile rw_secure = null;
                        long current_file_pointer_secure = 0;
                        boolean loop_break_secure = false;
                        System.out.println(username + " is uploading file to Server.");

                        while (!loop_break_secure) {
                            byte[] initilize = new byte[1];
                            try {
                                din.read(initilize, 0, initilize.length);
                                if (initilize[0] == 2) {
                                    byte[] cmd_buff = new byte[3];
                                    din.read(cmd_buff, 0, cmd_buff.length);
                                    byte[] recv_data = ReadStream();

                                    // Upload file to Server in the Secured Server Folder
                                    switch (Integer.parseInt(new String(cmd_buff))) {
                                        case 124:
                                            rw_secure = new RandomAccessFile(
                                                    "../Server/Secured/" + new String(recv_data), "rw"); // Change
                                                                                                         // folder name
                                                                                                         // if required
                                                                                                         // and
                                                                                                         // recompile
                                                                                                         // code
                                            dout.write(CreateDataPacket("125".getBytes("UTF8"),
                                                    String.valueOf(current_file_pointer_secure).getBytes("UTF8"))); // Write
                                                                                                                    // data
                                            dout.flush();
                                            break;
                                        case 126:
                                            rw_secure.seek(current_file_pointer_secure);
                                            rw_secure.write(recv_data);
                                            current_file_pointer_secure = rw_secure.getFilePointer();
                                            dout.write(CreateDataPacket("125".getBytes("UTF8"),
                                                    String.valueOf(current_file_pointer_secure).getBytes("UTF8")));
                                            dout.flush();
                                            break;
                                        case 127:
                                            if ("Close".equals(new String(recv_data))) {
                                                loop_break_secure = true;
                                                System.out.println(
                                                        username + " has successfully uploaded file to server"); // Trace
                                                                                                                 // on
                                                                                                                 // terminal
                                                                                                                 // to
                                                                                                                 // show
                                                                                                                 // file
                                                                                                                 // uploaded
                                            }
                                            break;
                                    }
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        break;

                    // Get all files from the Server folder an return them as a String and send to
                    // Client
                    case "Files":
                        System.out.println("Sending list of files to " + username);
                        List<String> results = new ArrayList<String>();

                        File[] files = new File("../Server/Files").listFiles();
                        // If this pathname does not denote a directory, then listFiles() returns null.

                        for (File file : files) {
                            if (file.isFile()) {
                                results.add(" -> " + file.getName());
                            }
                        }

                        String joined = String.join("\n", results);
                        dout.writeUTF("/Server/Files/: \n" + joined); // Send File list to Client
                        break;
                    case "SecuredFiles":

                        dout.writeUTF("Please enter the password");
                        dout.flush();

                        //Read client password and check if password match, 403 Error if not
                        if (!din.readUTF().equals(password)) {
                            dout.writeUTF("403");
                            dout.flush();
                            break;
                        }
                        System.out.println("Sending list of Secured files to " + username);
                        List<String> secured_results = new ArrayList<String>();

                        File[] secured_files = new File("../Server/Secured").listFiles();
                        // If pathname does not denote  directory,  listFiles() returns null.

                        for (File file : secured_files) {
                            if (file.isFile()) {
                                secured_results.add(" -> " + file.getName());
                            }
                        }

                        String joined_secured = String.join("\n", secured_results);

                        dout.writeUTF("/Server/Secured/: \n" + joined_secured); // Send Secured File list to Client
                        dout.flush();
                        break;
                    // When client wants to download a file from the server
                    case "Download":
                        try {
                            File target_file = new File("../Server/Files/" + rec[1]);
                            if (!target_file.exists()) {
                                dout.writeUTF("404"); //File not found error handling
                                dout.flush();
                                break;
                            }

                            dout.writeUTF("Downloading file from Server. Please wait...");
                            dout.flush();
                            String chk_status = din.readUTF();

                            if(chk_status.equals("499")){
                                System.out.println("Client cancelled donwnload");
                                break;
                            }else if(chk_status.equals("200")){
                                System.out.println("Transferring " + rec[1] + " to " + username + ". Please wait...");
                            }

                            // Create packet of data for file and write it to DataOutputStream
                            dout.write(
                                    CreateDataPacket("124".getBytes("UTF8"), target_file.getName().getBytes("UTF8")));
                            dout.flush();

                            
                            rw = new RandomAccessFile(target_file, "r");
                            current_file_pointer = 0;
                            loop_break = false;
                            // Send file to Client
                            while (true) {
                                if (din.read() == 2) {
                                    byte[] cmd_buff = new byte[3];
                                    din.read(cmd_buff, 0, cmd_buff.length);
                                    byte[] recv_buff = ReadStream();
                                    switch (Integer.parseInt(new String(cmd_buff))) {
                                        case 125:
                                            current_file_pointer = Long.valueOf(new String(recv_buff));
                                            int buff_len = (int) (rw.length() - current_file_pointer < 20000
                                                    ? rw.length() - current_file_pointer
                                                    : 20000);
                                            byte[] temp_buff = new byte[buff_len];
                                            if (current_file_pointer != rw.length()) {
                                                rw.seek(current_file_pointer);
                                                rw.read(temp_buff, 0, temp_buff.length);
                                                dout.write(CreateDataPacket("126".getBytes("UTF8"), temp_buff));
                                                dout.flush();
                                            } else {
                                                loop_break = true;
                                                System.out
                                                        .println(rec[1] + " successfully transferred to " + username + "."); // Upon
                                                                                                                        // successful
                                                                                                                        // file
                                                                                                                        // transfer
                                                                                                                        // to
                                                                                                                        // client
                                            }
                                            break;
                                    }
                                }
                                if (loop_break == true) {
                                    dout.write(CreateDataPacket("127".getBytes("UTF8"), "Close".getBytes("UTF8")));
                                    dout.flush();
                                    break;
                                }
                            }
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            System.out.println("Array out of bounds error!");
                        }
                        break;
                    case "SecureDownload":
                        dout.writeUTF("Please enter the password");
                        dout.flush();

                        //Read client password and check if password match, 403 Error if not
                        if (!din.readUTF().equals(password)) {
                            dout.writeUTF("403");
                            dout.flush();
                            break;
                        }
                        try {
                            File target_file = new File("../Server/Secured/" + rec[1]);
                            if (!target_file.exists()) {
                                dout.writeUTF("404"); //404 Error: File not found
                                dout.flush();
                                break;
                            }

                            dout.writeUTF("Downloading file from Server. Please wait..."); 
                            dout.flush();
                            
                            String chk_status = din.readUTF();

                            if(chk_status.equals("499")){
                                System.out.println("Client cancelled donwnload");
                                break;
                            }else if(chk_status.equals("200")){
                                System.out.println("Transferring " + rec[1] + " to " + username + ". Please wait...");
                            }

                            // Create packet of data for file and write it to DataOutputStream
                            dout.write(
                                    CreateDataPacket("124".getBytes("UTF8"), target_file.getName().getBytes("UTF8")));
                            dout.flush();
                            System.out.println("Transferring " + rec[1] + " to " + username + ". Please wait...");

                            rw = new RandomAccessFile(target_file, "r");
                            current_file_pointer = 0;
                            loop_break = false;
                            // Send file to Client
                            while (true) {
                                if (din.read() == 2) {
                                    byte[] cmd_buff = new byte[3];
                                    din.read(cmd_buff, 0, cmd_buff.length);
                                    byte[] recv_buff = ReadStream();
                                    switch (Integer.parseInt(new String(cmd_buff))) {
                                        case 125:
                                            current_file_pointer = Long.valueOf(new String(recv_buff));
                                            int buff_len = (int) (rw.length() - current_file_pointer < 20000
                                                    ? rw.length() - current_file_pointer
                                                    : 20000);
                                            byte[] temp_buff = new byte[buff_len];
                                            if (current_file_pointer != rw.length()) {
                                                rw.seek(current_file_pointer);
                                                rw.read(temp_buff, 0, temp_buff.length);
                                                dout.write(CreateDataPacket("126".getBytes("UTF8"), temp_buff));
                                                dout.flush();
                                            } else {
                                                loop_break = true;
                                                System.out
                                                        .println(rec[1] + " successfully transferred to " + username + "."); // Upon
                                                                                                                        // successful
                                                                                                                        // file
                                                                                                                        // transfer
                                                                                                                        // to
                                                                                                                        // client
                                            }
                                            break;
                                    }
                                }
                                if (loop_break == true) {
                                    dout.write(CreateDataPacket("127".getBytes("UTF8"), "Close".getBytes("UTF8")));
                                    dout.flush();
                                    break;
                                }
                            }
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            System.out.println("Array out of bounds error!");
                        }
                        break;

                }

            } catch (IOException ex) {
                Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // Reading the File Data that Client selected and convert it to bytes that will
    // be used
    // for transfer
    private byte[] ReadStream() {
        byte[] data_buff = null;
        try {
            int b = 0;
            String buff_length = "";
            while ((b = din.read()) != 4) {
                buff_length += (char) b;
            }
            int data_length = Integer.parseInt(buff_length);
            data_buff = new byte[Integer.parseInt(buff_length)];
            int byte_read = 0;
            int byte_offset = 0;
            while (byte_offset < data_length) {
                byte_read = din.read(data_buff, byte_offset, data_length - byte_offset);
                byte_offset += byte_read;
            }
        } catch (IOException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data_buff;
    }

    // Create Data Packet and return the packet
    private byte[] CreateDataPacket(byte[] cmd, byte[] data) {
        byte[] packet = null;
        try {
            byte[] initialize = new byte[1];
            initialize[0] = 2;
            byte[] separator = new byte[1];
            separator[0] = 4;
            byte[] data_length = String.valueOf(data.length).getBytes("UTF8");
            packet = new byte[initialize.length + cmd.length + separator.length + data_length.length + data.length];

            // Here we are simulating a data packet with the necessary identifying
            // information
            System.arraycopy(initialize, 0, packet, 0, initialize.length);
            System.arraycopy(cmd, 0, packet, initialize.length, cmd.length);
            System.arraycopy(data_length, 0, packet, initialize.length + cmd.length, data_length.length);
            System.arraycopy(separator, 0, packet, initialize.length + cmd.length + data_length.length,
                    separator.length);
            System.arraycopy(data, 0, packet, initialize.length + cmd.length + data_length.length + separator.length,
                    data.length);

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
        return packet;
    }
}

//Reference: https://www.youtube.com/watch?v=hCMVx9ywBqA