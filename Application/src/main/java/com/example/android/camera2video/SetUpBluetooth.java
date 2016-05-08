package com.example.rocky.motioncapture;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class SetUpBluetooth extends AppCompatActivity {

    private BluetoothAdapter bluetooth;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> arrayAdapter;

    public static String EXTRA_DEVICE_ADDRESS;

    ListView pairedListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up_bluetooth);

        bluetooth = BluetoothAdapter.getDefaultAdapter();


        if(bluetooth == null) {
            //Show a mensage that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            //finish apk
            finish();
        }
        else{
            if(!bluetooth.isEnabled()){ //Request bluetooth to be enabled if not already
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon,1);
            }

            // Query paired devices
            pairedDevices = bluetooth.getBondedDevices();
            arrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    arrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            pairedListView = (ListView) findViewById(R.id.paired_devices);
            pairedListView.setOnItemClickListener(mDeviceClickListener);
            pairedListView.setAdapter(arrayAdapter);

            // Now time to connect paired devices. This device will act as the server.
            //Thread serverConnect = new Thread(new AcceptThread());
            //serverConnect.start();


        }
    }
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            Intent i = new Intent(SetUpBluetooth.this, VideoCapture.class);
            i.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(i);


        }
    };

    // Run as server
    private class AcceptThread implements Runnable {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = bluetooth.listenUsingRfcommWithServiceRecord("Motion capture", UUID.fromString("888fbf7e-370a-43b1-8fc4-63220e3f9fb3"));
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();

                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    mmServerSocket.close();
                    break;
                }
                } catch (IOException e) {
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        private void manageConnectedSocket(BluetoothSocket socket){
            Thread mConnect = new Thread(new ConnectedThread(socket));
            mConnect.start();
        }

    }

    // Keep the connection going
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    String readMessage = new String(buffer, 0, bytes);

                    // Make any received message stop recoding

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
