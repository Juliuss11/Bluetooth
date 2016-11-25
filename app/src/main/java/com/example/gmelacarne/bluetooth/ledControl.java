package com.example.gmelacarne.bluetooth;

/**
 * Created by gmelacarne on 16/11/2016.
 */

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.io.IOException;
import java.util.UUID;


public class ledControl extends AppCompatActivity {
    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected TextView state, messageText, scrollPosition;
    Button btnLed;
    boolean ledStatus = false;
    SeekBar seekBar;
    int brightness = 0;
    String stateString, messageString, buttonString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get the parameter from the activity that start this one
        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the chat
        setContentView(R.layout.chat_layout);

        // initialize the interface and the botton
        state = (TextView) findViewById(R.id.state);
        messageText = (TextView) findViewById(R.id.messageText);
        btnLed = (Button) findViewById(R.id.buttonLed);
        btnLed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ledStatus) {
                    turnOffLed();
                } else {
                    turnOnLed();
                }
                ledStatus = !ledStatus;
            }
        });
        messageString = "Connecting...";
        stateString = "Led Off";
        buttonString = "Turn led On";

        //update the view
        updateLayout();

        // initialize the SeekBar
        scrollPosition = (TextView) findViewById(R.id.scrollPosition);
        seekBar = (SeekBar) findViewById(R.id.setBrightiness);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                scrollPosition.setText(String.valueOf(i));
                brightness = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // start the bluetooth connection, done in an other thread
        final ledControl activity = this;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection

                    messageString = "Connected";
                    // maybe here we have to start the thread for the reading from the BT
                    new Thread(new ReadingBT()).start();
                } catch (IOException e) {
                    messageString = "Fail to connect";
                }

                // update the view
                updateLayout();

            }
        }).start(); // .start() to start the thread

    }

    private void turnOnLed() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write("a".toString().getBytes());
                stateString = "Led on";
            } catch (IOException e) {
                stateString = "Error on turn led on";
            }
        }
        updateLayout();
    }

    private void turnOffLed() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write("s".toString().getBytes());
                stateString = "Led off";
            } catch (IOException e) {
                stateString = "Error on turn led off";
            }
        }
        updateLayout();
    }

    private void changeBrightness() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(brightness);
                state.setText("Led on");
            } catch (IOException e) {
                state.setText("Error on turn led on");
            }
        }

    }


    private void updateLayout() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnLed.setText(buttonString);
                messageText.setText(messageString);
                state.setText(stateString);
                //scrollPosition.setText(String.valueOf(brightness) + "%");
            }
        });
    }

    private class ReadingBT implements Runnable {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        StringBuilder readMessage = new StringBuilder();

        @Override
        public void run() {
            while (true) {
                try {

                    bytes = btSocket.getInputStream().read(buffer);
                    final String read = new String(buffer, 0, bytes);
                    readMessage.append(read);

                    if (read.contains("\n")) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                messageText.setText(readMessage);
                            }
                        });

                        //clear the message before read the new one
                        readMessage.delete(0, readMessage.length());
                    }

                } catch (IOException e) {

//                    connectionLost();
                    break;
                }
            }

        }
    }

}
