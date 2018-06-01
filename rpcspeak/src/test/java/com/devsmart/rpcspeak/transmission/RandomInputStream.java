package com.devsmart.rpcspeak.transmission;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class RandomInputStream extends InputStream {


    private Random mRandom;

    public RandomInputStream(Random random) {
        mRandom = random;
    }

    @Override
    public int read() throws IOException {
        return 0;
    }
}
