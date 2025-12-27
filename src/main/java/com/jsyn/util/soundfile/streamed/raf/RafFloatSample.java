package com.jsyn.util.soundfile.streamed.raf;

import com.jsyn.data.FloatSample;
import lombok.SneakyThrows;

/**
 * @author IzumiiKonata
 * Date: 2025/5/5 12:48
 */
public class RafFloatSample extends FloatSample {

    public final RafWAVEFileParser.StreamedByteDataReader reader;

    /** Constructor for multi-channel samples with data. */
    public RafFloatSample(RafWAVEFileParser.StreamedByteDataReader reader, int numFrames, int channelsPerFrame) {
        this.reader = reader;
        this.numFrames = numFrames;
        this.channelsPerFrame = channelsPerFrame;
    }

    @Override
    public void allocate(int numFrames, int channelsPerFrame) {
//        buffer = new float[numFrames * channelsPerFrame];

    }

    @Override
    public void read(int startFrame, float[] data, int startIndex, int numFrames) {
        int numSamplesToRead = numFrames * channelsPerFrame;
        int firstSampleIndexToRead = startFrame * channelsPerFrame;
//        System.arraycopy(buffer, firstSampleIndexToRead, data, startIndex, numSamplesToRead);
    }

    public int lastReadIndexOffset = 0;

    float[] lastReadData;

    @Override
    @SneakyThrows
    public double readDouble(int index) {

        if (closed)
            return 0;

        if (index - lastReadIndexOffset >= reader.floatArrLen || index - lastReadIndexOffset < 0 || lastReadData == null) {
            lastReadIndexOffset = index;

            lastReadData = reader.read((long) index * reader.getSampleSizeInBytes());
        }

        if (lastReadData == null || index - lastReadIndexOffset > lastReadData.length - 1)
            return 0;

        return lastReadData[index - lastReadIndexOffset];
    }

    boolean closed = false;

    @Override
    @SneakyThrows
    public void cleanUp() {

        closed = true;

        if (this.reader != null)
            this.reader.parser.raf.close();
    }
}
