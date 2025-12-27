/*
 * Copyright 2001 Phil Burk, Mobileer Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jsyn.util.soundfile;

import com.jsyn.data.FloatSample;
import com.jsyn.data.SampleMarker;

import java.io.IOException;
import java.util.HashMap;

/**
 * Base class for various types of audio specific file parsers.
 * 
 * @author (C) 2001 Phil Burk, SoftSynth.com
 */

abstract class AudioFileParser implements ChunkHandler {
    public IFFParser parser;
    public byte[] byteData;
    boolean ifLoadData = true; /* If true, load sound data into memory. */
    public long dataPosition; /*
                        * Number of bytes from beginning of file where sound data resides.
                        */
    public int bitsPerSample;
    public int bytesPerFrame; // in the file
    public int bytesPerSample; // in the file
    public HashMap<Integer, SampleMarker> cueMap = new HashMap<Integer, SampleMarker>();
    public short samplesPerFrame;
    public double frameRate;
    public int numFrames;
    public double originalPitch = 60.0;
    public int sustainBegin = -1;
    public int sustainEnd = -1;

    public AudioFileParser() {
    }

    /**
     * @return Number of bytes from beginning of stream where sound data resides.
     */
    public long getDataPosition() {
        return dataPosition;
    }

    /**
     * This can be read by another thread when load()ing a sample to determine how many bytes have
     * been read so far.
     */
    public synchronized long getNumBytesRead() {
        IFFParser p = parser; // prevent race
        if (p != null)
            return p.getOffset();
        else
            return 0;
    }

    /**
     * This can be read by another thread when load()ing a sample to determine how many bytes need
     * to be read.
     */
    public synchronized long getFileSize() {
        IFFParser p = parser; // prevent race
        if (p != null)
            return p.getFileSize();
        else
            return 0;
    }

    public SampleMarker findOrCreateCuePoint(int uniqueID) {
        SampleMarker cuePoint = cueMap.get(uniqueID);
        if (cuePoint == null) {
            cuePoint = new SampleMarker();
            cueMap.put(uniqueID, cuePoint);
        }
        return cuePoint;
    }

    public FloatSample load(IFFParser parser) throws IOException {
        this.parser = parser;
        parser.parseAfterHead(this);
        return finish();
    }

    abstract FloatSample finish() throws IOException;

    FloatSample makeSample(float[] floatData) throws IOException {
        FloatSample floatSample = new FloatSample(floatData, samplesPerFrame);

        floatSample.setChannelsPerFrame(samplesPerFrame);
        floatSample.setFrameRate(frameRate);
        floatSample.setPitch(originalPitch);

        if (sustainBegin >= 0) {
            floatSample.setSustainBegin(sustainBegin);
            floatSample.setSustainEnd(sustainEnd);
        }

        for (SampleMarker marker : cueMap.values()) {
            floatSample.addMarker(marker);
        }

        /* Set Sustain Loop by assuming first two markers are loop points. */
        if (floatSample.getMarkerCount() >= 2) {
            floatSample.setSustainBegin(floatSample.getMarker(0).position);
            floatSample.setSustainEnd(floatSample.getMarker(1).position);
        }
        return floatSample;
    }

    public String parseString(IFFParser parser, int textLength) throws IOException {
        byte[] bar = new byte[textLength];
        parser.read(bar);
        return new String(bar);
    }
}
