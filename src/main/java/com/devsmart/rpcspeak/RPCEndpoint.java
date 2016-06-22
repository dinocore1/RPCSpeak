package com.devsmart.rpcspeak;


import com.devsmart.ubjson.UBObject;
import com.devsmart.ubjson.UBReader;
import com.devsmart.ubjson.UBValue;
import com.devsmart.ubjson.UBWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.*;

public class RPCEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(RPCEndpoint.class);



    private final UBReader mInputReader;
    private final UBWriter mOutputWriter;
    private final BlockingQueue<Runnable> mRequestQueue;
    private ExecutorService mServiceThreads;

    private boolean mRunning;
    private ReaderThread mReaderThread;

    RPCEndpoint(int maxNumThreads, int maxQueuedRequests, InputStream in, OutputStream out) {
        mRequestQueue = new ArrayBlockingQueue<Runnable>(maxQueuedRequests);
        mServiceThreads = new ThreadPoolExecutor(1, maxNumThreads, 30, TimeUnit.SECONDS, mRequestQueue);
        mInputReader = new UBReader(in);
        mOutputWriter = new UBWriter(out);
    }

    private class ReaderThread extends Thread {
        public ReaderThread() {
            super("RPCEndpoint Reader");
        }

        @Override
        public void run() {
            while(mRunning) {
                try {
                    UBValue msg = mInputReader.read();

                    if(msg.isObject()) {
                        UBObject msgObj = msg.asObject();
                        msgObj.get("type");
                    }

                } catch (IOException e) {
                    LOGGER.error("error reading message: {}", e);
                }

            }
            mReaderThread = null;
        }
    }

    private synchronized void sendMessage(UBValue msg) {
        try {
            mOutputWriter.write(msg);
        } catch (IOException e) {
            LOGGER.error("error sending message: {}", msg);
        }
    }

    public void start() {
        if(mRunning) {
            LOGGER.warn("already running");
            return;
        }

        mRunning = true;
        mReaderThread = new ReaderThread();
        mReaderThread.start();

    }

    public void shutdown() {
        if(!mRunning) {
            LOGGER.warn("not running");
            return;
        }

        try {
            mRunning = false;
            mReaderThread.join();
        } catch (InterruptedException e) {
            LOGGER.error("error shutting down: {}", e);
        }
    }


}
