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
import java.util.Iterator;
import java.util.Map;
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
    public static final UBValue EXCEPTION_THROWN = UBValueFactory.createString("Exception Thrown");
    public static final UBValue EXCEPTION_SHUTDOWN = UBValueFactory.createString("Shutdown");


    public interface Listener {
        void onShutdown();
    }

    private final UBReader mInputReader;
    private final UBWriter mOutputWriter;
    private final BlockingQueue<Runnable> mServiceQueue;
    private final ExecutorService mServiceExecutor;
    private int mRequestId;
    private Listener mListener;

    private ReaderThread mReaderThread;

    private final HashMap<String, RPC> mMethods = new HashMap<String, RPC>();
    private final HashMap<Integer, HandleRequestTask> mRequests = new HashMap<Integer, HandleRequestTask>();

    public RPCEndpoint(int maxNumThreads, int maxQueuedRequests, InputStream in, OutputStream out) {
        mServiceQueue = new ArrayBlockingQueue<Runnable>(maxQueuedRequests);
        mServiceExecutor = new ThreadPoolExecutor(1, maxNumThreads, 30, TimeUnit.SECONDS, mServiceQueue, this);
        mInputReader = new UBReader(in);
        mOutputWriter = new UBWriter(out);
    }

    public RPCEndpoint(InputStream in, OutputStream out) {
        this(5, 10, in, out);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private class ReaderThread extends Thread {

        boolean mRunning;

        public ReaderThread() {
            super("RPCEndpoint Reader");
        }

        @Override
        public void run() {
            mRunning = true;
            try {
                while (mRunning) {

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
                                    LOGGER.debug("got shutdown request");
                                    mRunning = false;
                                    break;

                                default:
                                    throw new IOException("unknown message type:" + type);
                            }
                        } else {
                            LOGGER.warn("received unknown object: {}", msgObj);
                        }
                    }

                }
            } catch (Exception e) {
                LOGGER.error("", e);
            } finally {
                mRunning = false;
                shutdown();
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
                try {
                    UBValue responseObj = method.invoke(mRequest.mArgs);
                    respMsg = createResponse(mRequest.mId, responseObj);
                } catch(Throwable e) {
                    respMsg = createExceptionResponse(mRequest.mId, e);
                }
            } else {
                respMsg = createUnknownMethodResponse(mRequest.mId);
            }

            sendMessage(respMsg);
        }
    }

    private class HandleRequestTask {

        final Request mRequest;
        public UBValue mResult;
        public UBValue mExcept;

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

    private static UBObject createExceptionResponse(int id, Throwable throwable) {
        UBObject msgObj = UBValueFactory.createObject();
        msgObj.put(KEY_TYPE, UBValueFactory.createInt(TYPE_RESPONSE));
        msgObj.put(KEY_ID, UBValueFactory.createInt(id));
        msgObj.put(KEY_EXCEPTION, EXCEPTION_THROWN);

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
        UBValue exceptObj = msgObj.get(KEY_EXCEPTION);

        HandleRequestTask task;
        synchronized (this) {
            task = mRequests.remove(id);
        }

        synchronized (task) {
            task.mResult = result;
            task.mExcept = exceptObj;
            task.notify();
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

    public synchronized void start() {
        if(mReaderThread != null) {
            LOGGER.warn("already running");
            return;
        }

        mReaderThread = new ReaderThread();
        mReaderThread.start();
    }

    public synchronized void shutdown() {
        internalShutdown();
    }

    private synchronized void internalShutdown() {
        try {

            sendMessage(createShutdownMessage());

            if(mReaderThread != null) {
                mReaderThread.mRunning = false;
            }
            mReaderThread = null;


            Iterator<Map.Entry<Integer, HandleRequestTask>> it = mRequests.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, HandleRequestTask> entry = it.next();
                HandleRequestTask task = entry.getValue();
                task.mExcept = EXCEPTION_SHUTDOWN;

                it.remove();

                synchronized (task) {
                    task.notifyAll();
                }
            }



            if(mListener != null) {
                mListener.onShutdown();
            }
            mListener = null;

            mServiceExecutor.shutdown();

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

    public UBValue RPC(String method, UBArray args) throws RPCException {
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
                if(requestTask.mExcept != null) {
                    throw new RPCException(requestTask.mExcept);
                } else {
                    return requestTask.mResult;
                }
            }
        } catch(InterruptedException e){
            LOGGER.error("unexpected interrupt: {}", e);
            Throwables.propagate(e);
            return null;
        }
    }

    private synchronized RPC getMethod(String method) {
        return mMethods.get(method);
    }


}
