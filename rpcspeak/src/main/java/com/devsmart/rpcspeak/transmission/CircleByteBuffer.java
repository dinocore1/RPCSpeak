package com.devsmart.rpcspeak.transmission;

public class CircleByteBuffer {

    private final byte[] mBuffer;
    private final int mCapacity;
    private int mIdxPut;
    private int mIdxGet;
    private int mAvailable;

    public CircleByteBuffer(int size) {
        mBuffer = new byte[size];
        mCapacity = size;
    }

    public int getCapacity() {
        return mCapacity;
    }

    public int getAvailable() {
        return mAvailable;
    }

    public boolean put(byte b) {
        if(mAvailable == mCapacity) {
            return false;
        }

        mBuffer[mIdxPut] = b;
        mIdxPut = (mIdxPut + 1) % mCapacity;
        mAvailable++;
        return true;
    }

    public byte get() {
        byte retval = mBuffer[mIdxGet];
        mIdxGet = (mIdxGet + 1) % mCapacity;
        mAvailable--;
        return retval;
    }

    public int get(byte[] dest, int offset, int maxLen) {

        maxLen = Math.min(mAvailable, maxLen);
        int limit = mIdxGet < mIdxPut ? mIdxPut : mCapacity;
        int count = Math.min(limit - mIdxGet, maxLen);

        System.arraycopy(mBuffer, mIdxGet, dest, offset, count);
        mIdxGet = (mIdxGet + count) % mCapacity;
        mAvailable -= count;
        return count;

    }

    public void skip(int size) {
        mIdxGet = (mIdxGet + size) % mCapacity;
        mAvailable -= size;
    }
}
