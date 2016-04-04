package com.apex.androi.wifitransfer;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import com.lis.pascal.wifitransfer.R;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.HashSet;


public class ConnectionAcceptor implements Runnable {



    android.os.Handler  handler=new Handler();
    private MainActivity mainActivity;
    boolean stop = false;
    int port = 8888;
    public static String ipstr=null;
    ServerSocket ssock;

    HashSet<SingleConnection> hsT; //connection list
    HashSet<InetAddress> hs; //unique ip list
    HashSet<InetAddress> hsP; //authorized users list


    ConnectionAcceptor(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        handler.postDelayed(runnable,500);

    }

    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, 500);


            try {



                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();



                        if (!inetAddress.isLoopbackAddress()) {


                            ipstr = inetAddress.getHostAddress().toString() + ":" + ssock.getLocalPort() + "";


                        }



                    }
                }

                if(ipstr==null){
                    ipstr="Enable Your Network";
                }


            } catch (Exception e) {
                e.printStackTrace();
            }



        }
    };

    /**
     * Authorizes a connection and adds it to the list
     *
     * @param conn     Connection attempting to authorize
     * @param password Password attempted by connection
     * @return True if correct password
     */
    synchronized boolean authUser(SingleConnection conn, String password) {
//        if(password == mainActivity.)
//        System.out.println("pass: " + password + " attempted");
//        System.out.println("pass=" + mainActivity.getPassword());
        if (password.equals(mainActivity.getPassword())) {
            if (!hsP.contains(conn.sock.getInetAddress()))
                hsP.add(conn.sock.getInetAddress());
            return true;
        } else
            return false;
    }

    synchronized boolean getAuth(InetAddress addr) {
        return hsP.contains(addr);
    }

    synchronized void removeAllAuth() {
        hsP.clear();
    }

    synchronized void removeConn(SingleConnection conn) {
        if (hsT.contains(conn))
            hsT.remove(conn);
    }

    synchronized void addConn(SingleConnection conn) {
        hsT.add(conn);
    }


    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        try {
            ssock = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        WifiManager wm = (WifiManager) mainActivity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wi = wm.getConnectionInfo();

        int ip = 0;

        if (wm.isWifiEnabled()) {
            ip = wi.getIpAddress();


        }




//        System.out.println(ip);

        String ipAddress = Formatter.formatIpAddress(
                wm.getConnectionInfo().getIpAddress());

        //   String is="";





        // Log.d("MYTAG",is);
        Log.d("MYTAG", ipAddress);
        Log.d("MYTAG", ip + "");
        if (ip != 0x00000000)
            ipstr = (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." + ((ip >> 16) & 0xff)
                    + "." + ((ip >> 24) & 0xff) + ":" + ssock.getLocalPort();

        //ipstr = ipAddress+":"+ssock.getLocalPort()+"";

        Log.d("MYTAG", ipstr + "");




        // changes the ip address shown in the app
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                TextView tv = (TextView) mainActivity.findViewById(R.id.ip);
                tv.setText(ipstr);


            }
        });





//        System.out.println("Set ip: " + ipstr);


        hs = new HashSet<>();

        hsT = new HashSet<>();
        hsP = new HashSet<>();

        while (!stop) {
            try {
                Socket s = ssock.accept();
//                System.out.println("sendbuffersize:" + s.getSendBufferSize());
//                System.out.println("recvbuffersize:" + s.getReceiveBufferSize());
                SingleConnection serv = new SingleConnection(mainActivity, this, s, getAuth(s.getInetAddress()));
                addConn(serv);
                new Thread(serv).start();

                if (!hs.contains(s.getInetAddress())) {
                    hs.add(s.getInetAddress());
                    mainActivity.makeToast("New connection: " + s.getInetAddress(), false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            ssock.close();
            for (SingleConnection i : hsT) {
                i.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
//            System.out.println("could not close server socket used in acceptor");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deauth() {
        for (SingleConnection i : hsT)
            i.logout();

        removeAllAuth();
    }



}
