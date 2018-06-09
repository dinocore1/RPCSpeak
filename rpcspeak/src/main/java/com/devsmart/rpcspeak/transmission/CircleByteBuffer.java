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

    public int free() {
        return mCapacity - mAvailable;
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

    public int put(byte[] buff, int offset, int maxLen) {
        maxLen = Math.min(free(), maxLen);
        int limit = mIdxGet <= mIdxPut ? mCapacity : mIdxGet;
        int count = Math.min(limit - mIdxPut, maxLen);

        System.arraycopy(buff, offset, mBuffer, mIdxPut, count);
        mIdxPut = (mIdxPut + count) % mCapacity;
        mAvailable += count;
        return count;
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

    public int peek(byte[] dest, int offset, int maxLen, int skip) {
        int idxGet = (mIdxGet + skip) % mCapacity;
        maxLen = Math.min(mAvailable, maxLen);
        int limit = idxGet < mIdxPut ? mIdxPut : mCapacity;
        int count = Math.min(limit - idxGet, maxLen);

        System.arraycopy(mBuffer, idxGet, dest, offset, count);
        return count;
    }

    public void skip(int size) {
        mIdxGet = (mIdxGet + size) % mCapacity;
        mAvailable -= size;
    }
}
