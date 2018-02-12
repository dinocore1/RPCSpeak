package com.devsmart.rpcspeak;


import com.devsmart.ubjson.*;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.*;
import static com.google.common.base.Preconditions.*;

public class RPCEndpoint implements RejectedExecutionHandler{

    private static final Logger LOGGER = LoggerFactory.getLogger(RPCEndpoint.class);

    public static final String KEY_TYPE = "type";
    public static final String KEY_ID = "id";
    public static final String KEY_METHOD = "method";
    public static final String KEY_ARGS = "args";
    public static final String KEY_RESULT = "result";
    private static final String KEY_EXCEPTION = "except";
    public static final int TYPE_REQUEST = 0;
    public static final int TYPE_RESPONSE = 1;
    public static final int TYPE_SHUTDOWN = 2;
    private static final int MAX_REQUEST_ID = 0x10000;

    public static final UBValue EXCEPTION_UNKNOWNMETHOD = UBValueFactory.createString("Unknown Method");
    public static final UBValue EXCEPTION_BUSY = UBValueFactory.createString("Too Busy");


    public interface Listener {
        void onRemoteShutdown();
    }

    private final UBReader mInputReader;
    private final UBWriter mOutputWriter;
    private final BlockingQueue<Runnable> mServiceQueue;
    private final ExecutorService mServiceExecutor;
    private int mRequestId;
    private Listener mListener;

    private boolean mRunning;
    private ReaderThread mReaderThread;

    private final HashMap<String, RPC> mMethods = new HashMap<String, RPC>();
    private final HashMap<Integer, HandleRequestTask> mRequests = new HashMap<Integer, HandleRequestTask>();

    public RPCEndpoint(int maxNumThreads, int maxQueuedRequests, InputStream in, OutputStream out) {
        mServiceQueue = new ArrayBlockingQueue<Runnable>(maxQueuedRequests);
        mServiceExecutor = new ThreadPoolExecutor(1, maxNumThreads, 30, TimeUnit.SECONDS, mServiceQueue, this);
        mInputReader = new UBReader(in);
        mOutputWriter = new UBWriter(out);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private class ReaderThread extends Thread {
        public ReaderThread() {
            super("RPCEndpoint Reader");
        }

        @Override
        public void run() {
            try {
                while (mRunning) {
                    try {
                        UBValue msg = mInputReader.read();

                        if (msg.isObject()) {
                            final UBObject msgObj = msg.asObject();

                            UBValue msgType = msgObj.get("type");
                            if (msgType != null && msgType.isInteger()) {
                                final int type = msgType.asInt();
                                switch (type) {
                                    case TYPE_REQUEST:
                                        mServiceExecutor.execute(new HandleServiceTask(msgObj));
                                        break;

                                    case TYPE_RESPONSE:
                                        handleResponse(msgObj);
                                        break;

                                    case TYPE_SHUTDOWN:
                                        mServiceExecutor.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                handleRemoteShutdown(msgObj);
                                            }
                                        });

                                        break;

                                    default:
                                        throw new IOException("unknown message type:" + type);
                                }
                            } else {
                                LOGGER.warn("received unknown object: {}", msgObj);
                            }
                        }

                    } catch (Exception e) {
                        LOGGER.error("error reading message: {}", e);
                    }

                }
            } finally {
                mServiceExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if(mListener != null) {
                            mListener.onRemoteShutdown();
                        }
                    }
                });
                mReaderThread = null;
            }
        }
    }

    private class HandleServiceTask implements Runnable {

        final Request mRequest;

        public HandleServiceTask(UBObject msgObj) {
           mRequest = new Request(msgObj);
        }

        @Override
        public void run() {
            UBObject respMsg;

            RPC method = getMethod(mRequest.mMethod);
            if(method != null) {
                respMsg = createResponse(mRequest.mId, method.invoke(mRequest.mArgs));
            } else {
                respMsg = createUnknownMethodResponse(mRequest.mId);
            }

            sendMessage(respMsg);
        }
    }

    private class HandleRequestTask {

        final Request mRequest;
        public UBValue mResult;

        void sendRequest() {
            sendMessage(mRequest.toMsg());
        }

        public HandleRequestTask(Request r) {
            mRequest = r;
        }
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if(executor == mServiceExecutor) {
            HandleServiceTask task = (HandleServiceTask) r;
            UBObject respMsg = createBusyResponse(task.mRequest.mId);
            sendMessage(respMsg);
        }

    }


    private static UBObject createResponse(int id, UBValue result) {
        UBObject msgObj = UBValueFactory.createObject();
        msgObj.put(KEY_TYPE, UBValueFactory.createInt(TYPE_RESPONSE));
        msgObj.put(KEY_ID, UBValueFactory.createInt(id));
        msgObj.put(KEY_RESULT, result);

        return msgObj;
    }

    private static UBObject createUnknownMethodResponse(int id) {
        UBObject msgObj = UBValueFactory.createObject();
        msgObj.put(KEY_TYPE, UBValueFactory.createInt(TYPE_RESPONSE));
        msgObj.put(KEY_ID, UBValueFactory.createInt(id));
        msgObj.put(KEY_EXCEPTION, EXCEPTION_UNKNOWNMETHOD);

        return msgObj;
    }

    private static UBObject createBusyResponse(int id) {
        UBObject msgObj = UBValueFactory.createObject();
        msgObj.put(KEY_TYPE, UBValueFactory.createInt(TYPE_RESPONSE));
        msgObj.put(KEY_ID, UBValueFactory.createInt(id));
        msgObj.put(KEY_EXCEPTION, EXCEPTION_BUSY);

        return msgObj;
    }

    private static UBObject createShutdownMessage() {
        UBObject msgObj = UBValueFactory.createObject();
        msgObj.put(KEY_TYPE, UBValueFactory.createInt(TYPE_SHUTDOWN));

        return  msgObj;
    }

    private void handleResponse(UBObject msgObj) {
        final int id = msgObj.get(KEY_ID).asInt();
        UBValue result = msgObj.get(KEY_RESULT);
        if(result != null) {
            HandleRequestTask task;
            synchronized (this) {
                task = mRequests.remove(id);
            }

            synchronized (task) {
                task.mResult = result;
                task.notify();
            }

        }
    }

    private void handleRemoteShutdown(UBObject msgObj) {
        try {
            if (mListener != null) {
                mListener.onRemoteShutdown();
            }
            mServiceExecutor.shutdown();
            mRunning = false;
            mReaderThread.join();
            mInputReader.close();
            mOutputWriter.close();
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private void sendMessage(UBValue msg) {
        synchronized (mOutputWriter) {
            try {
                mOutputWriter.write(msg);
            } catch (IOException e) {
                LOGGER.error("error sending message: {}", msg);
            }
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
            sendMessage(createShutdownMessage());
            mServiceExecutor.shutdown();
            mRunning = false;
            mReaderThread.join();
            mInputReader.close();
            mOutputWriter.close();
        } catch (Exception e) {
            LOGGER.error("error shutting down: {}", e);
        }
    }

    public synchronized void registerMethod(String method, RPC obj) {
        checkArgument(!Strings.isNullOrEmpty(method) && obj != null);
        mMethods.put(method, obj);
    }

    public synchronized void unregisterMethod(String method) {
        mMethods.remove(method);
    }

    public UBValue RPC(String method, UBArray args) {
        HandleRequestTask requestTask;
        synchronized (this) {
            mRequestId = (mRequestId + 1) % MAX_REQUEST_ID;
            requestTask = new HandleRequestTask(new Request(mRequestId, method, args));
            mRequests.put(requestTask.mRequest.mId, requestTask);
        }

        try {
            synchronized (requestTask) {
                requestTask.sendRequest();
                requestTask.wait();
                return requestTask.mResult;
            }
        } catch(InterruptedException e){
            LOGGER.error("unepected interrupt: {}", e);
            Throwables.propagate(e);
            return null;
        }
    }

    private synchronized RPC getMethod(String method) {
        return mMethods.get(method);
    }


}
