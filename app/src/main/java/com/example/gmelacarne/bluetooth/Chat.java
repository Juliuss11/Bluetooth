package com.example.gmelacarne.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.UUID;

/**
 * Created by gmelacarne on 18/11/2016.
 */
public class Chat extends AppCompatActivity {
    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected  TextView state, messageText, scrollPosition;
    Button buttonLed;
    boolean ledStatus = false;
    SeekBar seekBar;
    String stateString, messageString, buttonString;
    int brightness = 0;



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
        messageText= (TextView) findViewById(R.id.messageText);
        scrollPosition = (TextView) findViewById(R.id.scrollPosition);
        buttonLed = (Button) findViewById(R.id.buttonLed);
        buttonLed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                if(ledStatus){
                    turnOffLed();
                }
                else{
                    turnOnLed();
                }
                */
                ledStatus=!ledStatus;
                sendBTMessage();
            }
        });

        messageString = "Connecting...";
        stateString = "Led Off";
        buttonString = "Turn led On";

        //update the view
        updateLayout();

        buttonLed.setText("Turn led On");
        messageText.setText("Connecting...");
        state.setText("Led Off");

        // initialize the SeekBar
        seekBar = (SeekBar) findViewById(R.id.setBrightiness);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                //scrollPosition.setText(String.valueOf(i));
                brightness = i;
                updateLayout();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // start the bluetooth connection, done in an other thread
        final Chat activity = this;

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
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("a".toString().getBytes());
                stateString = "Led on";
                buttonString = "Turn led On";
            }
            catch (IOException e)
            {
                stateString = "Error on turn led on";
            }
        }
        // update the view
        updateLayout();

    }

    private void turnOffLed() {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("s".toString().getBytes());
                stateString = "Led off";
                buttonString = "Turn led Off";
            }
            catch (IOException e)
            {
                stateString = "Error on turn led off";
            }
        }
        // update the view
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
                buttonLed.setText(buttonString);
                messageText.setText(messageString);
                state.setText(stateString);
                scrollPosition.setText(String.valueOf(brightness));
            }
        });
    }

    private void sendBTMessage(){
        if (btSocket != null) {
            try {
                if(ledStatus) {
                    btSocket.getOutputStream().write("a".toString().getBytes());
                }
                else {
                    btSocket.getOutputStream().write("s".toString().getBytes());
                }
                btSocket.getOutputStream().write((byte)brightness);
            } catch (IOException e) {
                stateString = "Error on sending message";
            }
        }
    }

    private class ReadingBT implements Runnable
    {
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
                        //delete the message after write it
                        readMessage.delete(0,readMessage.length());
                    }

                } catch (IOException e) {

//                    connectionLost();
                    break;
                }
            }

        }

    }

}
