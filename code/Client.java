import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import java.util.*;
import java.util.ArrayList;
import java.awt.FileDialog;
import java.util.List;
import java.awt.Frame;
import java.awt.EventQueue;


/**
 * Author: Sabir Buxsoo 
 * Assignment: 1
 * Client Class
 * Implements multithreading for multiple client requests
 */
public class Client {

    // Main Method, taking args from terminal

    public static void main(String[] args) {
        try {

            // Create a new TCPDataClient Object
            Client obj = new Client();

            // Socket connecting to port 1337
            Socket obj_client = new Socket(InetAddress.getByName("127.0.0.1"), 1999);

            // Data Input and Output Streams
            DataInputStream din = new DataInputStream(obj_client.getInputStream());
            DataOutputStream dout = new DataOutputStream(obj_client.getOutputStream());

            // Take Client Input
            Scanner scn = new Scanner(System.in);

            JFileChooser jfc = null;

            // Get Welcome Message
            System.out.println(din.readUTF());

            //User Login
            String username = scn.nextLine();
            dout.writeUTF(username);
            dout.flush();

            String auth_response = din.readUTF();

            if(auth_response.equals("403")){
                System.out.println("User does not exist. Access Denied.\nGoodbye!");
                System.exit(0);
            }else{
                System.out.println(auth_response);
                String password = scn.nextLine();
                dout.writeUTF(password);
                dout.flush();

                String auth_response_pass = din.readUTF();
                if(auth_response_pass.equals("403")){
                    System.out.println("Wrong password. Access Denied.\nGoodbye!");
                    System.exit(0);
                }
            }

            while (true) {
                // Get Client Command
                System.out.println("\nEnter command:");
                String snd = scn.nextLine();
                String[] tosend = snd.split(" ");

                if (tosend[0].equals("Download") && tosend.length == 1) {
                    System.out.println("Wrong command. Download <file_name.file_type>");
                    continue;
                }

                if (tosend[0].equals("SecureDownload") && tosend.length == 1) {
                    System.out.println("Wrong command. SecureDownload <file_name.file_type>");
                    continue;
                }

                if (tosend[0].equals("Upload")) {
                    if (jfc == null) {
                        jfc = new JFileChooser();
                    }

                    int dialog_value = jfc.showOpenDialog(null);

                    // Using Java Swing to open File Chooser for client to select file to upload
                    if (dialog_value == JFileChooser.APPROVE_OPTION) {
                        File target_file = jfc.getSelectedFile();

                        //Working on robustness
                        Long file_size = target_file.length();
                        
                        //Do not allow file size greater than 100mb
                        if(file_size > 104857600){
                            System.out.println("File too large. Must be less than 100 mb");
                            continue;
                        }

                        dout.writeUTF(String.join(" ", tosend));
                        dout.flush();

                        // Create packet of data for file and write it to DataOutputStream
                        dout.write(
                                obj.CreateDataPacket("124".getBytes("UTF8"), target_file.getName().getBytes("UTF8")));
                        dout.flush();
                        System.out.println("Transferring file to server. Please wait...");

                        RandomAccessFile rw = new RandomAccessFile(target_file, "r");
                        long current_file_pointer = 0;
                        boolean loop_break = false;

                        // Upload File to Server
                        while (true) {
                            if (din.read() == 2) {
                                byte[] cmd_buff = new byte[3];
                                din.read(cmd_buff, 0, cmd_buff.length);
                                byte[] recv_buff = obj.ReadStream(din);
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
                                            dout.write(obj.CreateDataPacket("126".getBytes("UTF8"), temp_buff));
                                            dout.flush();
                                        } else {
                                            loop_break = true;
                                            System.out.println("File successfully uploaded to server.");
                                        }
                                        break;
                                }
                            }
                            if (loop_break == true) {
                                dout.write(obj.CreateDataPacket("127".getBytes("UTF8"), "Close".getBytes("UTF8")));
                                dout.flush();
                                break;
                            }
                        }
                    }
                } else if (tosend[0].equals("SecureUpload")) {
                    dout.writeUTF(String.join(" ", tosend));
                    dout.flush();

                    System.out.println(din.readUTF());

                    //Send password to server
                    dout.writeUTF(scn.nextLine());
                    dout.flush();

                    //If password wrong, 403 Error: Access Forbidden
                    String files_s_out_s = din.readUTF();
                    if (files_s_out_s.equals("403")) {
                        System.out.println(files_s_out_s);
                        continue;
                    }

                    System.out.println(files_s_out_s);
                    if (jfc == null) {
                        jfc = new JFileChooser();
                    }

                    int dialog_value = jfc.showOpenDialog(null);

                    // Using Java Swing to open File Chooser for client to select file to upload
                    if (dialog_value == JFileChooser.APPROVE_OPTION) {
                        File target_file = jfc.getSelectedFile();
                        
                        // Create packet of data for file and write it to DataOutputStream
                        dout.write(
                                obj.CreateDataPacket("124".getBytes("UTF8"), target_file.getName().getBytes("UTF8")));
                        dout.flush();
                        System.out.println("Transferring file to server. Please wait...");

                        RandomAccessFile rw = new RandomAccessFile(target_file, "r");
                        long current_file_pointer = 0;
                        boolean loop_break = false;

                        // Upload File to Server
                        while (true) {
                            if (din.read() == 2) {
                                byte[] cmd_buff = new byte[3];
                                din.read(cmd_buff, 0, cmd_buff.length);
                                byte[] recv_buff = obj.ReadStream(din);
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
                                            dout.write(obj.CreateDataPacket("126".getBytes("UTF8"), temp_buff));
                                            dout.flush();
                                        } else {
                                            loop_break = true;
                                            System.out.println("File successfully uploaded to server.");
                                        }
                                        break;
                                }
                            }
                            if (loop_break == true) {
                                dout.write(obj.CreateDataPacket("127".getBytes("UTF8"), "Close".getBytes("UTF8")));
                                dout.flush();
                                break;
                            }
                        }
                    }

                } else if (tosend[0].equals("Files")) { // Request list of files from the Files Folder on Server
                    dout.writeUTF(String.join(" ", tosend));
                    dout.flush();
                    System.out.println(din.readUTF());
                } else if (tosend[0].equals("SecuredFiles")) { // Request list of secured files from the Secured Folder on Server
                    dout.writeUTF(String.join(" ", tosend));
                    dout.flush();
                    System.out.println(din.readUTF());

                    //Enter Password
                    String password = scn.nextLine();
                    dout.writeUTF(password);
                    dout.flush();

                    //If password wrong, 403 Error: Access Forbidden
                    String files_s_out = din.readUTF();
                    if (files_s_out.equals("403")) {
                        System.out.println("Access forbidden. Wrong password!");
                        continue;
                    } else {
                        System.out.println(files_s_out);
                    }
                } else if (tosend[0].equals("Download")) { // Download a file from the server
                    dout.writeUTF(String.join(" ", tosend));
                    dout.flush();

                    if (din.readUTF().equals("404")) {
                        System.out.println("File not found on Server");
                        continue;
                    }
                   
                    //Open Frame to Select Save Directory
                    String osName = System.getProperty("os.name");
                    String homeDir = System.getProperty("user.home");
                    String save_location = "";
                    File selectedPath = null;
                    if (osName.equals("Mac OS X")) {
                        //Only works for Mac OS
                        System.setProperty("apple.awt.fileDialogForDirectories", "true");
                        FileDialog fd = new FileDialog(new Frame(), "Choose a file", FileDialog.LOAD);
                        fd.setDirectory(homeDir);
                        fd.setVisible(true);
                        String fileName = fd.getFile();
                        //System.out.println(fileName);
                        File file;
                        if (fileName != null) {
                            file = new File(fd.getDirectory() + fileName);
                            save_location = file.getAbsolutePath();
                            //System.out.println("You selected "+save_location);
                            dout.writeUTF("200"); //Status OK
                            dout.flush();
                        } else {
                            System.out.println("You haven't selected anything");
                            dout.writeUTF("499"); //Client closed request
                            dout.flush();
                            continue;
                        }
                    }else {
                        //For Windows OS
                        JFileChooser chooser = new JFileChooser();
                        
                        chooser.setCurrentDirectory(new java.io.File("."));
                        chooser.setDialogTitle("choosertitle");
                        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        chooser.setAcceptAllFileFilterUsed(false);

                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            dout.writeUTF("200"); //Status OK
                            dout.flush();
                            save_location = chooser.getSelectedFile().getAbsolutePath();
                        } else {
                            System.out.println("You must select a destination folder. Please try again");
                            dout.writeUTF("499"); //Client closed request
                            dout.flush();
                            continue;
                        }
                    }
                    
                    System.out.println("Downloading " + tosend[1] + " from Server. Please wait...");
                    RandomAccessFile rw = null;
                    long current_file_pointer = 0;
                    boolean loop_break = false;
                    while (!loop_break) {
                        byte[] initilize = new byte[1];
                        try {
                            din.read(initilize, 0, initilize.length);
                            if (initilize[0] == 2) {
                                byte[] cmd_buff = new byte[3];
                                din.read(cmd_buff, 0, cmd_buff.length);
                                byte[] recv_data = obj.ReadStream(din); // Read File using the ReadStream Method

                                switch (Integer.parseInt(new String(cmd_buff))) {
                                    case 124:
                                        rw = new RandomAccessFile(save_location + "/" + new String(recv_data), "rw");
                                        dout.write(obj.CreateDataPacket("125".getBytes("UTF8"),
                                                String.valueOf(current_file_pointer).getBytes("UTF8")));
                                        dout.flush();
                                        break;
                                    case 126:
                                        rw.seek(current_file_pointer);
                                        rw.write(recv_data);
                                        current_file_pointer = rw.getFilePointer();
                                        dout.write(obj.CreateDataPacket("125".getBytes("UTF8"),
                                                String.valueOf(current_file_pointer).getBytes("UTF8")));
                                        dout.flush();
                                        break;
                                    case 127:
                                        if ("Close".equals(new String(recv_data))) {
                                            loop_break = true;
                                            System.out.println(tosend[1] + " successfully downloaded to" + save_location);
                                            rw.close();
                                        }
                                        break;
                                }
                            }

                        } catch (IOException ex) {
                            System.out.println("Error again");
                            continue;
                        } 
                    }

                    // break;
                } else if (tosend[0].equals("SecureDownload")) { // Download Secured file from the server
                    dout.writeUTF(String.join(" ", tosend));
                    dout.flush();

                    System.out.println(din.readUTF());

                    //Enter Password
                    String password = scn.nextLine();
                    dout.writeUTF(password);
                    dout.flush();

                    //If password wrong, 403 Error: Access Denied
                    String files_s_out = din.readUTF();
                    if (files_s_out.equals("403")) {
                        System.out.println("Access forbidden. Wrong password!");
                        continue;
                    } else if (files_s_out.equals("404")) { //File not Found error, 404 Error
                        System.out.println("File not found on Server");
                        continue;
                    }

                    String osName = System.getProperty("os.name");
                    String homeDir = System.getProperty("user.home");
                    String save_location = "";
                    File selectedPath = null;

                    //Open Frame to select Save directory
                    if (osName.equals("Mac OS X")) {
                        //Mac OS
                        System.setProperty("apple.awt.fileDialogForDirectories", "true");
                        FileDialog fd = new FileDialog(new Frame(), "Choose a file", FileDialog.LOAD);
                        fd.setDirectory(homeDir);
                        fd.setVisible(true);
                        String fileName = fd.getFile();
                        //System.out.println(fileName);
                        File file;
                        if (fileName != null) {
                            file = new File(fd.getDirectory() + fileName);
                            save_location = file.getAbsolutePath();
                            //System.out.println("You selected "+save_location);
                            dout.writeUTF("200"); //Status OK
                            dout.flush();
                        } else {
                            System.out.println("You haven't selected anything");
                            dout.writeUTF("499"); //Client closed request
                            dout.flush();
                            continue;
                        }
                    }else {
                        //Windows
                        JFileChooser chooser = new JFileChooser();
                        
                        chooser.setCurrentDirectory(new java.io.File("."));
                        chooser.setDialogTitle("choosertitle");
                        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        chooser.setAcceptAllFileFilterUsed(false);

                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            dout.writeUTF("200"); //Status OK
                            dout.flush();
                            save_location = chooser.getSelectedFile().getAbsolutePath();
                        } else {
                            System.out.println("You must select a destination folder. Please try again");
                            dout.writeUTF("499"); //Client closed request
                            dout.flush();
                            continue;
                        }
                    }

                    System.out.println("Downloading " + tosend[1] + " from Server. Please wait...");
                    RandomAccessFile rw = null;
                    long current_file_pointer = 0;
                    boolean loop_break = false;
                    while (!loop_break) {
                        byte[] initilize = new byte[1];
                        try {
                            din.read(initilize, 0, initilize.length);
                            if (initilize[0] == 2) {
                                byte[] cmd_buff = new byte[3];
                                din.read(cmd_buff, 0, cmd_buff.length);
                                byte[] recv_data = obj.ReadStream(din); // Read File using the ReadStream Method

                                switch (Integer.parseInt(new String(cmd_buff))) {
                                    case 124:
                                        rw = new RandomAccessFile(save_location + "/" + new String(recv_data), "rw");
                                        dout.write(obj.CreateDataPacket("125".getBytes("UTF8"),
                                                String.valueOf(current_file_pointer).getBytes("UTF8")));
                                        dout.flush();
                                        break;
                                    case 126:
                                        rw.seek(current_file_pointer);
                                        rw.write(recv_data);
                                        current_file_pointer = rw.getFilePointer();
                                        dout.write(obj.CreateDataPacket("125".getBytes("UTF8"),
                                                String.valueOf(current_file_pointer).getBytes("UTF8")));
                                        dout.flush();
                                        break;
                                    case 127:
                                        if ("Close".equals(new String(recv_data))) {
                                            loop_break = true;
                                            System.out.println(tosend[1] + " successfully downloaded to " + save_location);
                                            rw.close();
                                        }
                                        break;
                                }
                            }

                        } catch (IOException ex) {
                            System.out.println("Error again");
                        }
                    }

                    // break;
                } else if (tosend[0].equals("Exit")) {
                    dout.writeUTF(String.join(" ", tosend));
                    dout.flush();

                    System.out.println(din.readUTF());
                    System.exit(0);
                }
            }

        } catch (UnknownHostException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
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
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return packet;
    }

    // Reading the File Data that Client selected and convert it to bytes that will
    // be used
    // for transfer
    private byte[] ReadStream(DataInputStream din) {
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
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data_buff;
    }

}

//Reference: https://www.youtube.com/watch?v=hCMVx9ywBqA